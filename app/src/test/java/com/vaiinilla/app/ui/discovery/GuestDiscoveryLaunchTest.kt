package com.vaiinilla.app.ui.discovery

import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.guest.SchoolJoinCodeStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.location.DeviceLocationProvider
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.GeoCoordinates
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.SpaceResolveResult
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import com.vaiinilla.app.domain.repository.DiscoveryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A06, D08: como invitado, loadMySchools termina sin suspender (no hay sesión) y con Main.immediate el
 * finally de launchTracked corría antes de asignar el job: UninitializedPropertyAccessException.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuestDiscoveryLaunchTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun invitadoSinSesionCargaSusEscuelasSinCaerse() =
        // runTest hace fallar la prueba con la excepción que la corrutina del ViewModel no atrapa.
        runTest {
            val app = RuntimeEnvironment.getApplication()
            val viewModel =
                GuestDiscoveryViewModel(
                    discoveryRepository = EmptyDiscovery,
                    guestSessionStore = GuestSessionStore(app),
                    deviceLocationProvider = NoLocation,
                    schoolJoinCodes = SchoolJoinCodeStore(app),
                    accessRepository = NoAccess,
                    authRepository = NoSession,
                )

            viewModel.loadMySchools()
            viewModel.loadMySchools()

            assertTrue(
                viewModel.state.value.mySchools
                    .isEmpty(),
            )
        }

    private object EmptyDiscovery : DiscoveryRepository {
        override fun searchEstablishments(
            query: String,
            limit: Int,
            cursor: String?,
            nearTo: GeoCoordinates?,
        ): Result<Pair<List<PublicEstablishment>, String?>> = Result.success(emptyList<PublicEstablishment>() to null)

        override fun getEstablishment(slug: String): Result<PublicEstablishment> =
            Result.failure(IllegalStateException())

        override fun getGuestCatalog(slug: String): Result<Catalog> = Result.failure(IllegalStateException())

        override fun resolveSpaceToken(token: String): Result<SpaceResolveResult> =
            Result.failure(IllegalStateException())
    }

    private object NoLocation : DeviceLocationProvider {
        override suspend fun currentLocation(): GeoCoordinates? = null
    }

    private object NoAccess : AuthorizedAccessRepository {
        override suspend fun invitation(token: String): Result<AuthorizedInvitation> = error("no se usa")

        override suspend fun acceptInvitation(
            token: String,
            session: StudentAuthSession,
        ): Result<AuthorizedMode> = error("no se usa")

        override suspend fun authorizedModes(session: StudentAuthSession): Result<List<AuthorizedMode>> =
            error("no se usa")

        override suspend fun activateMode(
            mode: AuthorizedMode,
            session: StudentAuthSession,
        ): Result<AuthorizedModeContext> = error("no se usa")

        override suspend fun revokeMode(
            mode: AuthorizedMode,
            session: StudentAuthSession,
        ): Result<Unit> = error("no se usa")
    }

    private object NoSession : StudentAuthRepository {
        override fun peekSession(): StudentAuthSession? = null

        override fun isReadyForCheckout(establishmentId: String?): Boolean = false

        override suspend fun signUp(
            email: String,
            password: String,
            displayName: String,
        ): Result<StudentAuthSession> = error("no se usa")

        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<StudentAuthSession> = error("no se usa")

        override suspend fun reloadSession(): Result<StudentAuthSession?> = Result.success(null)

        override suspend fun getIdToken(forceRefresh: Boolean): Result<String> = error("no se usa")

        override suspend fun signOut() = Unit
    }
}
