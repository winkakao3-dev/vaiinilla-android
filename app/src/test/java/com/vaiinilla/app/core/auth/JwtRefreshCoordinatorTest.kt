package com.vaiinilla.app.core.auth

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Provider
import kotlin.concurrent.thread

class JwtRefreshCoordinatorTest {
    private fun coordinator(): VaiinillaJwtRefreshCoordinator =
        VaiinillaJwtRefreshCoordinator(
            authRepositoryProvider = Provider { throw UnsupportedOperationException() },
        )

    @Test
    fun `clearSession waits for an in-flight refresh before clearing state`() {
        val refreshStarted = CountDownLatch(1)
        val allowRefresh = CountDownLatch(1)
        val savedTokens = Collections.synchronizedList(mutableListOf<String>())
        val coordinator = coordinator()

        coordinator.startSession(OperationalRole.CLIENT, expiresInSeconds = 900) {
            refreshStarted.countDown()
            allowRefresh.await(5, TimeUnit.SECONDS)
            savedTokens += "fresh-jwt"
            Result.success(Unit)
        }

        val refreshThread = thread { coordinator.refreshActiveSession() }
        assertTrue(refreshStarted.await(5, TimeUnit.SECONDS))

        val clearReturned = AtomicBoolean(false)
        val clearThread =
            thread {
                coordinator.clearSession()
                clearReturned.set(true)
            }

        Thread.sleep(200)
        assertFalse(
            "clearSession returned while a refresh was still writing the token",
            clearReturned.get(),
        )

        allowRefresh.countDown()
        refreshThread.join(5_000)
        clearThread.join(5_000)

        assertTrue(clearReturned.get())
        assertEquals(listOf("fresh-jwt"), savedTokens)
    }

    @Test
    fun `el refresh en vuelo del modo anterior no pisa el token del modo nuevo`() {
        val refreshStarted = CountDownLatch(1)
        val allowRefresh = CountDownLatch(1)
        val token = AtomicReference("cajero-jwt")
        val coordinator = coordinator()

        coordinator.startSession(OperationalRole.CASHIER, expiresInSeconds = 900) {
            refreshStarted.countDown()
            allowRefresh.await(5, TimeUnit.SECONDS)
            token.set("cajero-jwt-refrescado")
            Result.success(Unit)
        }
        val refreshThread = thread { coordinator.refreshActiveSession() }
        assertTrue(refreshStarted.await(5, TimeUnit.SECONDS))

        val adminThread =
            thread {
                coordinator.startSession(
                    OperationalRole.ADMIN,
                    expiresInSeconds = 900,
                    saveToken = { token.set("admin-jwt") },
                ) { Result.success(Unit) }
            }
        Thread.sleep(200)
        allowRefresh.countDown()
        refreshThread.join(5_000)
        adminThread.join(5_000)

        assertEquals("admin-jwt", token.get())
    }

    @Test
    fun `refresh fails cleanly after clearSession`() {
        val coordinator = coordinator()
        coordinator.startSession(OperationalRole.CLIENT, expiresInSeconds = 900) {
            Result.success(Unit)
        }
        coordinator.clearSession()

        assertTrue(coordinator.refreshActiveSession().isFailure)
    }

    private fun expiryEvents(coordinator: VaiinillaJwtRefreshCoordinator): AtomicInteger {
        val count = AtomicInteger(0)
        val ready = CountDownLatch(1)
        thread(isDaemon = true) {
            runBlocking {
                coroutineScope {
                    val job =
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            coordinator.sessionExpired.collect { count.incrementAndGet() }
                        }
                    ready.countDown()
                    delay(3_000)
                    job.cancel()
                }
            }
        }
        ready.await(2, TimeUnit.SECONDS)
        return count
    }

    private fun rejecting(coordinator: VaiinillaJwtRefreshCoordinator) {
        coordinator.startSession(OperationalRole.CLIENT, expiresInSeconds = 900) {
            Result.failure(ApiClientException("UNAUTHORIZED", "Sesión inválida", 401))
        }
    }

    @Test
    fun `three rejected refreshes in a row announce that the session expired`() {
        val coordinator = coordinator()
        val events = expiryEvents(coordinator)
        rejecting(coordinator)

        repeat(2) { coordinator.refreshActiveSession() }
        Thread.sleep(100)
        assertEquals(0, events.get())

        coordinator.refreshActiveSession()
        Thread.sleep(200)
        assertEquals(1, events.get())
    }

    @Test
    fun `network failures never announce an expired session`() {
        val coordinator = coordinator()
        val events = expiryEvents(coordinator)
        coordinator.startSession(OperationalRole.CLIENT, expiresInSeconds = 900) {
            Result.failure(IOException("sin red"))
        }

        repeat(5) { coordinator.refreshActiveSession() }
        Thread.sleep(200)

        assertEquals(0, events.get())
    }

    @Test
    fun `a non-rejection failure between rejections resets the count`() {
        val coordinator = coordinator()
        val events = expiryEvents(coordinator)
        val outcome = AtomicReference<Result<Unit>>(Result.failure(ApiClientException("UNAUTHORIZED", "x", 401)))
        coordinator.startSession(OperationalRole.CLIENT, expiresInSeconds = 900) { outcome.get() }

        repeat(2) { coordinator.refreshActiveSession() }
        outcome.set(Result.failure(IOException("sin red")))
        coordinator.refreshActiveSession()
        outcome.set(Result.failure(ApiClientException("UNAUTHORIZED", "x", 401)))
        repeat(2) { coordinator.refreshActiveSession() }
        Thread.sleep(200)

        assertEquals(0, events.get())
    }
}
