package com.vaiinilla.app.core.auth

import com.vaiinilla.app.core.notifications.DependentNamesStore
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.cache.RemoteResponseCache
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Base64
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StudentSessionCleanupTest {
    private val context = RuntimeEnvironment.getApplication()
    private val sessionStore = FakeSessionStore()
    private val cache = RemoteResponseCache(context, sessionStore)
    private val coordinator = VaiinillaJwtRefreshCoordinator(Provider { throw UnsupportedOperationException() })
    private val auth = RecordingAuth()
    private val pickups = RecordingPickups()

    private val cleanup =
        StudentSessionCleanup(
            authRepository = auth,
            guestSessionStore = GuestSessionStore(context),
            pickupTokenStore = pickups,
            refreshCoordinator = coordinator,
            responseCache = cache,
            context = context,
        ).also { it.cleanupDispatcher = Dispatchers.Unconfined }

    @Test
    fun `signing out leaves nothing of the previous user behind`() =
        runBlocking {
            sessionStore.token = fakeJwt()
            cache.write("wallets/me", emptyMap(), """{"saldo":"10.00"}""")
            DependentNamesStore.replace(context, mapOf("u-ana" to "Ana"))
            coordinator.startSession(OperationalRole.CLIENT, expiresInSeconds = 900) { Result.success(Unit) }
            assertEquals("""{"saldo":"10.00"}""", cache.read("wallets/me"))

            cleanup.clear()

            assertNull(cache.read("wallets/me"))
            assertNull(DependentNamesStore.firstName(context, "u-ana"))
            assertTrue(auth.signedOut)
            assertTrue(pickups.cleared)
            assertTrue(coordinator.refreshActiveSession().isFailure)
        }

    private fun fakeJwt(): String {
        val payload = """{"uid":"u1","rol":"cliente","establecimiento_id":"e1","membresia_id":"m1"}"""
        return "h.${Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())}.s"
    }

    private class FakeSessionStore : SecureSessionStore {
        var token: String? = null

        override fun saveAccessToken(token: String) {
            this.token = token
        }

        override fun readAccessToken(): String? = token

        override fun clear() {
            token = null
        }
    }

    private class RecordingPickups : PickupTokenStore {
        var cleared = false

        override fun save(
            orderId: String,
            pickupToken: String,
        ) = Unit

        override fun read(orderId: String): String? = null

        override fun clear() {
            cleared = true
        }
    }

    private class RecordingAuth : StudentAuthRepository {
        var signedOut = false

        override fun peekSession(): StudentAuthSession? = null

        override fun isReadyForCheckout(establishmentId: String?) = false

        override suspend fun signUp(
            email: String,
            password: String,
            displayName: String,
        ) = Result.failure<StudentAuthSession>(UnsupportedOperationException())

        override suspend fun signIn(
            email: String,
            password: String,
        ) = Result.failure<StudentAuthSession>(UnsupportedOperationException())

        override suspend fun reloadSession() = Result.success<StudentAuthSession?>(null)

        override suspend fun getIdToken(forceRefresh: Boolean) = Result.failure<String>(UnsupportedOperationException())

        override suspend fun signOut() {
            signedOut = true
        }
    }
}
