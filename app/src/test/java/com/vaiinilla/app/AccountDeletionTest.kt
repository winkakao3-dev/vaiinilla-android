package com.vaiinilla.app

import com.vaiinilla.app.core.auth.StudentSessionCleanup
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.account.RemoteAccountDeletionRepository
import com.vaiinilla.app.data.auth.student.StudentAuthUserNotFoundException
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.account.AccountDeletionRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.ui.account.AccountDeletionStatus
import com.vaiinilla.app.ui.account.AccountDeletionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException
import java.util.UUID
import javax.inject.Provider

class RemoteAccountDeletionRepositoryTest {
    @Test
    fun `uses Firebase bearer, exact body, endpoint and idempotency key`() =
        kotlinx.coroutines.test.runTest {
            val client = RecordingApiClient()
            val repository = RemoteAccountDeletionRepository(client)

            repository.deleteAccount("firebase-id-token", "550e8400-e29b-41d4-a716-446655440000").getOrThrow()

            assertEquals("identidad/cuenta", client.path)
            assertEquals("firebase-id-token", client.bearer)
            assertEquals("{\"confirmacion\":\"ELIMINAR\"}", client.body)
            assertEquals(
                mapOf("Idempotency-Key" to "550e8400-e29b-41d4-a716-446655440000"),
                client.headers,
            )
            assertFalse(client.body.orEmpty().contains("password", ignoreCase = true))
        }

    private class RecordingApiClient : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        var path: String? = null
        var bearer: String? = null
        var body: String? = null
        var headers: Map<String, String> = emptyMap()

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> = Result.failure(AssertionError("GET no esperado"))

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = Result.failure(AssertionError("POST no esperado"))

        override fun deleteWithBearer(
            bearer: String,
            path: String,
            body: String?,
            headers: Map<String, String>,
        ): Result<String> {
            this.bearer = bearer
            this.path = path
            this.body = body
            this.headers = headers
            return Result.success("")
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountDeletionViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var auth: FakeStudentAuthRepository
    private lateinit var deletion: FakeAccountDeletionRepository
    private lateinit var sessionStore: RecordingSessionStore
    private lateinit var viewModel: AccountDeletionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        deletion = FakeAccountDeletionRepository()
        sessionStore = RecordingSessionStore()
        auth = FakeStudentAuthRepository(sessionStore)
        val guestStore = GuestSessionStore(RuntimeEnvironment.getApplication())
        val refreshCoordinator =
            VaiinillaJwtRefreshCoordinator(
                authRepositoryProvider = Provider { throw UnsupportedOperationException() },
            )
        val cleanup =
            StudentSessionCleanup(
                authRepository = auth,
                guestSessionStore = guestStore,
                pickupTokenStore =
                    object : PickupTokenStore {
                        override fun save(
                            orderId: String,
                            pickupToken: String,
                        ) = Unit

                        override fun read(orderId: String): String? = null
                    },
                refreshCoordinator = refreshCoordinator,
            )
        viewModel = AccountDeletionViewModel(auth, deletion, cleanup)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cancel does not call backend`() {
        viewModel.requestConfirmation()
        viewModel.cancel()

        assertEquals(0, deletion.calls)
        assertEquals(AccountDeletionStatus.Idle, viewModel.state.value.status)
    }

    @Test
    fun `confirm starts reauthentication and not deletion`() {
        viewModel.requestConfirmation()
        viewModel.confirm()

        assertTrue(viewModel.state.value.status is AccountDeletionStatus.Reauthentication)
        assertEquals(0, auth.reauthenticationCalls)
        assertEquals(0, deletion.calls)
    }

    @Test
    fun `reauthentication failure does not call backend`() =
        runTest {
            auth.reauthenticationResult = Result.failure(IllegalStateException("Contraseña incorrecta."))
            viewModel.requestConfirmation()
            viewModel.confirm()
            viewModel.submitPassword("bad-password")
            advanceUntilIdle()

            assertEquals(1, auth.reauthenticationCalls)
            assertEquals(0, deletion.calls)
            assertTrue(viewModel.state.value.status is AccountDeletionStatus.Reauthentication)
        }

    @Test
    fun `sends Firebase ID token instead of Vaiinilla JWT`() =
        runTest {
            viewModel.beginAndSubmit()
            advanceUntilIdle()

            assertEquals("firebase-id-token", deletion.tokens.single())
            assertFalse(deletion.tokens.single().startsWith("vaiinilla-jwt"))
        }

    @Test
    fun `creates UUID v4 idempotency key`() =
        runTest {
            viewModel.beginAndSubmit()
            advanceUntilIdle()

            val key = deletion.keys.single()
            assertEquals(4, UUID.fromString(key).version())
            assertEquals(2, UUID.fromString(key).variant())
        }

    @Test
    fun `200 clears session and invokes navigation callback`() =
        runTest {
            var navigated = false
            viewModel.requestConfirmation()
            viewModel.confirm()
            viewModel.submitPassword("secret", onDeleted = { navigated = true })
            advanceUntilIdle()

            assertTrue(navigated)
            assertEquals(1, auth.signOutCalls)
            assertEquals(1, sessionStore.clearCalls)
            assertEquals(AccountDeletionStatus.Success, viewModel.state.value.status)
            assertEquals("Tu cuenta fue eliminada correctamente", viewModel.state.value.errorMessage)
        }

    @Test
    fun `reauthentication required asks again with same key`() =
        runTest {
            deletion.results += Result.failure(ApiClientException("REAUTHENTICATION_REQUIRED", "reauth", 401))
            deletion.results += Result.success(Unit)
            viewModel.beginAndSubmit()
            advanceUntilIdle()
            val firstKey = deletion.keys.single()
            assertTrue(viewModel.state.value.status is AccountDeletionStatus.Reauthentication)

            viewModel.submitPassword("secret")
            advanceUntilIdle()

            assertEquals(listOf(firstKey, firstKey), deletion.keys)
            assertEquals(AccountDeletionStatus.Success, viewModel.state.value.status)
        }

    @Test
    fun `502 keeps session and retry reuses same key`() =
        runTest {
            deletion.results += Result.failure(ApiClientException("ACCOUNT_DELETION_FAILED", "failed", 502))
            deletion.results += Result.success(Unit)
            viewModel.beginAndSubmit()
            advanceUntilIdle()
            val firstKey = deletion.keys.single()

            assertTrue(auth.hasSession)
            assertTrue(viewModel.state.value.status is AccountDeletionStatus.RecoverableError)
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(listOf(firstKey, firstKey), deletion.keys)
            assertEquals(AccountDeletionStatus.Success, viewModel.state.value.status)
        }

    @Test
    fun `timeout is recoverable and never reported as success`() =
        runTest {
            deletion.results += Result.failure(SocketTimeoutException("timeout"))
            viewModel.beginAndSubmit()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.status is AccountDeletionStatus.RecoverableError)
            assertFalse(viewModel.state.value.status is AccountDeletionStatus.Success)
            assertTrue(auth.hasSession)
        }

    @Test
    fun `double taps do not create parallel requests`() =
        runTest {
            viewModel.requestConfirmation()
            viewModel.confirm()
            viewModel.submitPassword("secret")
            viewModel.submitPassword("secret")
            advanceUntilIdle()

            assertEquals(1, deletion.calls)
        }

    @Test
    fun `Firebase user not found clears local session and returns to login`() =
        runTest {
            auth.tokenResult = Result.failure(StudentAuthUserNotFoundException())
            var returnedToLogin = false
            viewModel.requestConfirmation()
            viewModel.confirm()
            viewModel.submitPassword("secret", onSessionInvalidated = { returnedToLogin = true })
            advanceUntilIdle()

            assertEquals(0, deletion.calls)
            assertEquals(1, auth.signOutCalls)
            assertTrue(returnedToLogin)
            assertFalse(auth.hasSession)
        }

    private suspend fun AccountDeletionViewModel.beginAndSubmit() {
        requestConfirmation()
        confirm()
        submitPassword("secret")
    }

    private class FakeAccountDeletionRepository : AccountDeletionRepository {
        var calls = 0
        val tokens = mutableListOf<String>()
        val keys = mutableListOf<String>()
        val results = ArrayDeque<Result<Unit>>()

        override suspend fun deleteAccount(
            firebaseIdToken: String,
            idempotencyKey: String,
        ): Result<Unit> {
            calls += 1
            tokens += firebaseIdToken
            keys += idempotencyKey
            return if (results.isEmpty()) Result.success(Unit) else results.removeFirst()
        }
    }

    private class FakeStudentAuthRepository(
        private val sessionStore: SecureSessionStore,
    ) : StudentAuthRepository {
        var hasSession = true
        var signOutCalls = 0
        var reauthenticationCalls = 0
        var reauthenticationResult: Result<Unit> = Result.success(Unit)
        var tokenResult: Result<String> = Result.success("firebase-id-token")

        override fun peekSession(): StudentAuthSession? =
            if (hasSession) StudentAuthSession("uid", "ana@example.com", "Ana", true) else null

        override fun isReadyForCheckout(establishmentId: String?): Boolean = hasSession

        override suspend fun signUp(
            email: String,
            password: String,
            displayName: String,
        ): Result<StudentAuthSession> = Result.failure(UnsupportedOperationException())

        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<StudentAuthSession> = Result.failure(UnsupportedOperationException())

        override suspend fun reloadSession(): Result<StudentAuthSession?> = Result.success(peekSession())

        override suspend fun getIdToken(forceRefresh: Boolean): Result<String> = tokenResult

        override suspend fun reauthenticateWithPassword(password: String): Result<Unit> {
            reauthenticationCalls += 1
            return reauthenticationResult
        }

        override suspend fun signOut() {
            signOutCalls += 1
            hasSession = false
            sessionStore.clear()
        }
    }

    private class RecordingSessionStore : SecureSessionStore {
        var clearCalls = 0

        override fun saveAccessToken(token: String) = Unit

        override fun readAccessToken(): String? = "vaiinilla-jwt"

        override fun clear() {
            clearCalls += 1
        }
    }
}
