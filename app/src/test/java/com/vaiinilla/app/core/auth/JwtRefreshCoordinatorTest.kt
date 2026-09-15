package com.vaiinilla.app.core.auth

import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
    fun `refresh fails cleanly after clearSession`() {
        val coordinator = coordinator()
        coordinator.startSession(OperationalRole.CLIENT, expiresInSeconds = 900) {
            Result.success(Unit)
        }
        coordinator.clearSession()

        assertTrue(coordinator.refreshActiveSession().isFailure)
    }
}
