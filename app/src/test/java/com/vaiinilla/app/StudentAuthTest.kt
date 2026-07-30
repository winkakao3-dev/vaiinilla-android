package com.vaiinilla.app

import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.auth.ContextoExchanger
import com.vaiinilla.app.data.auth.SesionesContextoDataDto
import com.vaiinilla.app.data.auth.student.FixtureStudentAuthRepository
import com.vaiinilla.app.data.auth.student.StudentAuthPreferences
import com.vaiinilla.app.data.discovery.FixtureDiscoveryRepository
import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRepository
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRequest
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentResult
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.ui.auth.student.StudentAuthViewModel
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
            FixtureDiscoveryRepository(TestFixtureSource(), ContractFixtureParser())
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
            ContextoExchanger { _, _ ->
                SesionesContextoDataDto(
                    accessToken = "jwt-test",
                    tokenType = "Bearer",
                    expiresIn = 900,
                )
            }
        viewModel =
            StudentAuthViewModel(
                authRepository = authRepository,
                enrollmentRepository = enrollmentRepository,
                guestSessionStore = guestStore,
                sessionStore = sessionStore,
                contextoExchange = contextoExchange,
                refreshCoordinator =
                    VaiinillaJwtRefreshCoordinator(
                        authRepositoryProvider = Provider { throw UnsupportedOperationException() },
                    ),
                preferences = preferences,
                environment = AppEnvironment(DataSourceMode.MOCK, "https://localhost/"),
                fixtureAuthRepository = authRepository,
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
            viewModel.updateTermsAccepted(true)

            viewModel.register {}
            advanceUntilIdle()

            assertTrue(viewModel.state.value.emailExistsSuggestion)
            assertTrue(
                viewModel.state.value.errorMessage
                    ?.contains("Ya existe") == true,
            )
        }

    @Test
    fun `unverified session blocks checkout`() =
        runTest {
            viewModel.updateName("Ana")
            viewModel.updateEmail("ana@test.com")
            viewModel.updatePassword("secret1")
            viewModel.updateTermsAccepted(true)
            viewModel.register {}
            advanceUntilIdle()

            assertFalse(viewModel.isReadyForCheckout())
        }

    @Test
    fun `verified and enrolled session allows checkout`() =
        runTest {
            authRepository.signUp("ana@test.com", "secret1", "Ana")
            authRepository.markCurrentEmailVerified()
            authRepository.completeMockEnrollment("jwt-test", "est-1")

            assertTrue(viewModel.isReadyForCheckout())
        }

    @Test
    fun `enrollment for another establishment still requires auth`() =
        runTest {
            authRepository.signUp("ana@test.com", "secret1", "Ana")
            authRepository.markCurrentEmailVerified()
            authRepository.completeMockEnrollment("jwt-test", "other-est")

            assertFalse(viewModel.isReadyForCheckout())
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
            val vm =
                StudentAuthViewModel(
                    authRepository = auth,
                    enrollmentRepository = enrollmentRepository,
                    guestSessionStore = guestStore,
                    sessionStore = sessionStore,
                    contextoExchange =
                        ContextoExchanger { _, _ ->
                            SesionesContextoDataDto(
                                accessToken = "jwt-test",
                                tokenType = "Bearer",
                                expiresIn = 900,
                            )
                        },
                    refreshCoordinator =
                        VaiinillaJwtRefreshCoordinator(
                            authRepositoryProvider = Provider { throw UnsupportedOperationException() },
                        ),
                    preferences = preferences,
                    environment = AppEnvironment(DataSourceMode.MOCK, "https://localhost/"),
                    fixtureAuthRepository = auth,
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
