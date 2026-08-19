package com.vaiinilla.app

import com.vaiinilla.app.core.auth.StudentSessionCleanup
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.auth.ContextoExchanger
import com.vaiinilla.app.data.auth.SesionesContextoDataDto
import com.vaiinilla.app.data.auth.student.AccessEmailApi
import com.vaiinilla.app.data.auth.student.FixtureStudentAuthRepository
import com.vaiinilla.app.data.auth.student.StudentAuthPreferences
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.discovery.FixtureDiscoveryRepository
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRepository
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRequest
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentResult
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import com.vaiinilla.app.ui.auth.student.StudentAuthViewModel
import kotlinx.coroutines.CompletableDeferred
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
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudentAuthHandoffTest {
    private val centro =
        PublicEstablishment(
            id = "8246ff44-aad0-4e49-9268-b71c997893fe",
            name = "Cafetería Centro",
            slug = "cafeteria-centro",
            clientIdLabel = "Matrícula",
            clientIdRequired = true,
        )

    @Test
    fun `register path preserves venue and cart through auth handoff`() {
        val store = GuestSessionStore(RuntimeEnvironment.getApplication())
        val venue =
            GuestVenueContext(
                establishment = centro,
                space = PublicSpace(id = 12, name = "Mesa 4", type = "mesa"),
            )
        val catalog =
            FixtureDiscoveryRepository(TestFixtureSource(), ContractResponseParser())
                .getGuestCatalog("cafeteria-centro")
                .getOrThrow()
        val product = catalog.products.first()
        store.saveVenue(venue)
        val key = store.cartStorageKey(venue.establishment.id, venue.space?.id)
        store.saveCartSnapshot(
            key,
            listOf(CartLine(product = product, quantity = 2, selectedOptionIds = emptySet())),
        )

        assertEquals(venue, store.readVenue())
        assertEquals(1, store.readCartSnapshot(key).size)
        assertEquals(2, store.readCartSnapshot(key).single().quantity)

        val restored = store.restoreCartLines(store.readCartSnapshot(key), catalog.products)
        assertEquals(1, restored.size)
        assertEquals(product.id, restored.single().product.id)
        assertEquals(2, restored.single().quantity)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudentAuthViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var sessionStore: SecureSessionStore
    private lateinit var authRepository: FixtureStudentAuthRepository
    private lateinit var viewModel: StudentAuthViewModel
    private lateinit var contextExchangeStarted: CompletableDeferred<Unit>
    private lateinit var allowContextExchange: CompletableDeferred<Unit>
    private lateinit var emailVerificationStarted: CompletableDeferred<Unit>
    private lateinit var allowEmailVerification: CompletableDeferred<Unit>
    private lateinit var emailApi: CountingAccessEmailApi
    private var blockContextExchange = false
    private var blockEmailVerification = false

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        contextExchangeStarted = CompletableDeferred()
        allowContextExchange = CompletableDeferred()
        emailVerificationStarted = CompletableDeferred()
        allowEmailVerification = CompletableDeferred()
        val context = RuntimeEnvironment.getApplication()
        sessionStore =
            object : SecureSessionStore {
                private var token: String? = null

                override fun saveAccessToken(token: String) {
                    this.token = token
                }

                override fun readAccessToken(): String? = token

                override fun clear() {
                    token = null
                }
            }
        val preferences = StudentAuthPreferences(context)
        authRepository = FixtureStudentAuthRepository(sessionStore, preferences)
        val guestStore = GuestSessionStore(context)
        guestStore.saveVenue(
            GuestVenueContext(
                establishment =
                    PublicEstablishment(
                        id = "est-1",
                        name = "Demo",
                        slug = "demo",
                        clientIdLabel = "Identificador",
                        clientIdRequired = false,
                    ),
                space = null,
            ),
        )
        val enrollmentRepository =
            object : StudentEnrollmentRepository {
                override suspend fun enroll(
                    request: StudentEnrollmentRequest,
                    firebaseIdToken: String,
                ) = Result.success(StudentEnrollmentResult(membresiaId = "mock-membresia"))
            }
        val contextoExchange =
            ContextoExchanger { _, _, _, _ ->
                if (blockContextExchange) {
                    contextExchangeStarted.complete(Unit)
                    allowContextExchange.await()
                }
                SesionesContextoDataDto(
                    accessToken = "jwt-test",
                    tokenType = "Bearer",
                    expiresIn = 900,
                )
            }
        val refreshCoordinator =
            VaiinillaJwtRefreshCoordinator(
                authRepositoryProvider = Provider { throw UnsupportedOperationException() },
            )
        viewModel =
            StudentAuthViewModel(
                authRepository = authRepository,
                enrollmentRepository = enrollmentRepository,
                authorizedAccessRepository = fakeAuthorizedAccessRepository(),
                guestSessionStore = guestStore,
                sessionStore = sessionStore,
                contextoExchange = contextoExchange,
                refreshCoordinator = refreshCoordinator,
                preferences = preferences,
                remoteAccessEmailApi =
                    CountingAccessEmailApi(
                        shouldBlock = { blockEmailVerification },
                        started = emailVerificationStarted,
                        allow = allowEmailVerification,
                    ).also { emailApi = it },
                sessionCleanup =
                    StudentSessionCleanup(
                        authRepository,
                        guestStore,
                        object : PickupTokenStore {
                            override fun save(
                                orderId: String,
                                pickupToken: String,
                            ) = Unit

                            override fun read(orderId: String): String? = null
                        },
                        refreshCoordinator,
                    ),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `email exists suggests login`() =
        runTest {
            authRepository.signUp("taken@test.com", "secret1", "Ana")
            viewModel.updateName("Bea")
            viewModel.updateEmail("taken@test.com")
            viewModel.updatePassword("secret2")
            viewModel.updatePasswordConfirm("secret2")
            viewModel.updateTermsAccepted(true)
            viewModel.updatePrivacyAccepted(true)

            viewModel.register {}
            advanceUntilIdle()

            assertTrue(viewModel.state.value.emailExistsSuggestion)
            assertTrue(
                viewModel.state.value.errorMessage
                    ?.contains("Ya existe") == true,
            )
        }

    @Test
    fun `registration sends exactly one verification request when tapped twice`() =
        runTest {
            blockEmailVerification = true
            viewModel.updateName("Ana")
            viewModel.updateEmail("ana-once@test.com")
            viewModel.updatePassword("secret1")
            viewModel.updatePasswordConfirm("secret1")
            viewModel.updateTermsAccepted(true)
            viewModel.updatePrivacyAccepted(true)

            viewModel.register {}
            emailVerificationStarted.await()
            viewModel.register {}

            assertEquals(1, emailApi.verificationCalls)
            assertTrue(viewModel.state.value.loading)

            allowEmailVerification.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.loading)
        }

    @Test
    fun `verification failure keeps created session and does not recreate account`() =
        runTest {
            emailApi.verificationResult =
                Result.failure(ApiClientException("UPSTREAM_ERROR", "upstream", 502))
            viewModel.updateName("Ana")
            viewModel.updateEmail("ana-preserve@test.com")
            viewModel.updatePassword("secret1")
            viewModel.updatePasswordConfirm("secret1")
            viewModel.updateTermsAccepted(true)
            viewModel.updatePrivacyAccepted(true)

            viewModel.register {}
            advanceUntilIdle()

            assertEquals(1, emailApi.verificationCalls)
            assertTrue(viewModel.state.value.session != null)
            viewModel.register {}
            advanceUntilIdle()
            assertEquals(1, emailApi.verificationCalls)
            assertTrue(viewModel.state.value.emailExistsSuggestion)
        }

    @Test
    fun `unverified session blocks checkout`() =
        runTest {
            viewModel.updateName("Ana")
            viewModel.updateEmail("ana@test.com")
            viewModel.updatePassword("secret1")
            viewModel.updatePasswordConfirm("secret1")
            viewModel.updateTermsAccepted(true)
            viewModel.updatePrivacyAccepted(true)
            viewModel.register {}
            advanceUntilIdle()

            assertFalse(viewModel.isReadyForCheckout())
        }

    @Test
    fun `verified and enrolled session allows checkout`() =
        runTest {
            authRepository.signUp("ana@test.com", "secret1", "Ana")
            authRepository.markCurrentEmailVerified()
            authRepository.completeTestEnrollment("jwt-test", "est-1")

            assertTrue(viewModel.isReadyForCheckout())
        }

    @Test
    fun `enrollment for another establishment still requires auth`() =
        runTest {
            authRepository.signUp("ana@test.com", "secret1", "Ana")
            authRepository.markCurrentEmailVerified()
            authRepository.completeTestEnrollment("jwt-test", "other-est")

            assertFalse(viewModel.isReadyForCheckout())
        }

    @Test
    fun `process restart rehydrates required client context from authorized access`() =
        runTest {
            val context = RuntimeEnvironment.getApplication()
            val guestStore = GuestSessionStore(context)
            guestStore.saveVenue(
                GuestVenueContext(
                    establishment =
                        PublicEstablishment(
                            id = "est-required",
                            name = "Centro",
                            slug = "centro",
                            clientIdLabel = "Matrícula",
                            clientIdRequired = true,
                        ),
                    space = null,
                ),
            )
            val preferences = StudentAuthPreferences(context).also { it.clear() }
            val freshTokenSaved = CompletableDeferred<Unit>()
            val restartSessionStore =
                object : SecureSessionStore {
                    private var token: String? = null

                    override fun saveAccessToken(token: String) {
                        this.token = token
                        if (token == "fresh-jwt") freshTokenSaved.complete(Unit)
                    }

                    override fun readAccessToken(): String? = token

                    override fun clear() {
                        token = null
                    }
                }
            val auth = FixtureStudentAuthRepository(restartSessionStore, preferences)
            auth.signUp("restart@test.com", "secret1", "Ana")
            auth.markCurrentEmailVerified()
            auth.completeTestEnrollment("stale-jwt", "est-required")
            var exchangedIdentifier: String? = null
            val refreshCoordinator =
                VaiinillaJwtRefreshCoordinator(
                    authRepositoryProvider = Provider { throw UnsupportedOperationException() },
                )
            val vm =
                StudentAuthViewModel(
                    authRepository = auth,
                    enrollmentRepository =
                        object : StudentEnrollmentRepository {
                            override suspend fun enroll(
                                request: StudentEnrollmentRequest,
                                firebaseIdToken: String,
                            ) = Result.success(StudentEnrollmentResult(membresiaId = "membership-client"))
                        },
                    authorizedAccessRepository =
                        fakeAuthorizedAccessRepository(
                            listOf(
                                AuthorizedMode(
                                    role = OperationalRole.CLIENT,
                                    establishmentId = "est-required",
                                    establishmentName = "Centro",
                                    membershipId = "membership-client",
                                    clientIdentifier = "A012345",
                                ),
                            ),
                        ),
                    guestSessionStore = guestStore,
                    sessionStore = restartSessionStore,
                    contextoExchange =
                        ContextoExchanger { _, _, _, identifier ->
                            exchangedIdentifier = identifier
                            SesionesContextoDataDto(
                                accessToken = "fresh-jwt",
                                tokenType = "Bearer",
                                expiresIn = 900,
                            )
                        },
                    refreshCoordinator = refreshCoordinator,
                    preferences = preferences,
                    remoteAccessEmailApi = fakeAccessEmailApi(),
                    sessionCleanup =
                        StudentSessionCleanup(
                            auth,
                            guestStore,
                            object : PickupTokenStore {
                                override fun save(
                                    orderId: String,
                                    pickupToken: String,
                                ) = Unit

                                override fun read(orderId: String): String? = null
                            },
                            refreshCoordinator,
                        ),
                )

            freshTokenSaved.await()
            advanceUntilIdle()

            assertEquals("A012345", exchangedIdentifier)
            assertEquals("fresh-jwt", restartSessionStore.readAccessToken())
            assertTrue(vm.isReadyForCheckout())
        }

    @Test
    fun `session termination cancels enrollment that is still in flight`() =
        runTest {
            blockContextExchange = true
            authRepository.signUp("ana@test.com", "secret1", "Ana")
            authRepository.markCurrentEmailVerified()
            viewModel.refreshGuestVenue()
            var enrolled = false

            viewModel.completeEnrollment(onSuccess = { enrolled = true })
            contextExchangeStarted.await()
            viewModel.markSessionCleared()
            allowContextExchange.complete(Unit)
            advanceUntilIdle()

            assertFalse(enrolled)
            assertFalse(viewModel.state.value.enrollmentComplete)
            assertTrue(sessionStore.readAccessToken().isNullOrBlank())
        }

    @Test
    fun `login requires contextual id when establishment demands it`() =
        runTest {
            val context = RuntimeEnvironment.getApplication()
            val guestStore = GuestSessionStore(context)
            guestStore.saveVenue(
                GuestVenueContext(
                    establishment =
                        PublicEstablishment(
                            id = "est-required",
                            name = "Centro",
                            slug = "centro",
                            clientIdLabel = "Matrícula",
                            clientIdRequired = true,
                        ),
                    space = null,
                ),
            )
            val preferences = StudentAuthPreferences(context)
            val auth = FixtureStudentAuthRepository(sessionStore, preferences)
            auth.signUp("ana@test.com", "secret1", "Ana")
            auth.markCurrentEmailVerified()
            val enrollmentRepository =
                object : StudentEnrollmentRepository {
                    override suspend fun enroll(
                        request: StudentEnrollmentRequest,
                        firebaseIdToken: String,
                    ) = Result.success(StudentEnrollmentResult(membresiaId = "mock-membresia"))
                }
            val refreshCoordinator =
                VaiinillaJwtRefreshCoordinator(
                    authRepositoryProvider = Provider { throw UnsupportedOperationException() },
                )
            val vm =
                StudentAuthViewModel(
                    authRepository = auth,
                    enrollmentRepository = enrollmentRepository,
                    authorizedAccessRepository = fakeAuthorizedAccessRepository(),
                    guestSessionStore = guestStore,
                    sessionStore = sessionStore,
                    contextoExchange =
                        ContextoExchanger { _, _, _, _ ->
                            SesionesContextoDataDto(
                                accessToken = "jwt-test",
                                tokenType = "Bearer",
                                expiresIn = 900,
                            )
                        },
                    refreshCoordinator = refreshCoordinator,
                    preferences = preferences,
                    remoteAccessEmailApi = fakeAccessEmailApi(),
                    sessionCleanup =
                        StudentSessionCleanup(
                            auth,
                            guestStore,
                            object : PickupTokenStore {
                                override fun save(
                                    orderId: String,
                                    pickupToken: String,
                                ) = Unit

                                override fun read(orderId: String): String? = null
                            },
                            refreshCoordinator,
                        ),
                )
            vm.refreshGuestVenue()
            vm.updateEmail("ana@test.com")
            vm.updatePassword("secret1")
            var enrolled = false
            vm.login { enrolled = it }
            advanceUntilIdle()
            assertFalse(enrolled)
            assertTrue(
                vm.state.value.errorMessage
                    ?.contains("matrícula", ignoreCase = true) == true,
            )
        }
}

private fun fakeAuthorizedAccessRepository(modes: List<AuthorizedMode> = emptyList()): AuthorizedAccessRepository =
    object : AuthorizedAccessRepository {
        override suspend fun invitation(token: String): Result<AuthorizedInvitation> =
            Result.failure(UnsupportedOperationException())

        override suspend fun acceptInvitation(
            token: String,
            session: StudentAuthSession,
        ): Result<AuthorizedMode> = Result.failure(UnsupportedOperationException())

        override suspend fun authorizedModes(session: StudentAuthSession): Result<List<AuthorizedMode>> =
            Result.success(modes)

        override suspend fun activateMode(
            mode: AuthorizedMode,
            session: StudentAuthSession,
        ): Result<AuthorizedModeContext> = Result.failure(UnsupportedOperationException())

        override suspend fun revokeMode(
            mode: AuthorizedMode,
            session: StudentAuthSession,
        ): Result<Unit> = Result.failure(UnsupportedOperationException())
    }

private fun fakeAccessEmailApi(): AccessEmailApi =
    object : AccessEmailApi {
        override suspend fun sendVerification(firebaseIdToken: String): Result<Unit> = Result.success(Unit)

        override suspend fun sendRecovery(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun currentLegal() =
            Result.success(
                com.vaiinilla.app.data.auth.student.LegalDocuments(
                    termsVersion = "2026-07",
                    termsUrl = "https://example.test/terminos",
                    privacyVersion = "2026-07",
                    privacyUrl = "https://example.test/privacidad",
                ),
            )
    }

private class CountingAccessEmailApi(
    private val shouldBlock: () -> Boolean,
    private val started: CompletableDeferred<Unit>,
    private val allow: CompletableDeferred<Unit>,
) : AccessEmailApi {
    var verificationCalls = 0
    var verificationResult: Result<Unit> = Result.success(Unit)

    override suspend fun sendVerification(firebaseIdToken: String): Result<Unit> {
        verificationCalls++
        if (shouldBlock()) {
            started.complete(Unit)
            allow.await()
        }
        return verificationResult
    }

    override suspend fun sendRecovery(email: String): Result<Unit> = Result.success(Unit)

    override suspend fun currentLegal() =
        Result.success(
            com.vaiinilla.app.data.auth.student.LegalDocuments(
                termsVersion = "2026-07",
                termsUrl = "https://example.test/terminos",
                privacyVersion = "2026-07",
                privacyUrl = "https://example.test/privacidad",
            ),
        )
}
