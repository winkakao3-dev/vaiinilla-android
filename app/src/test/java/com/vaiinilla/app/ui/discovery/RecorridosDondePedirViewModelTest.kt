package com.vaiinilla.app.ui.discovery

import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.guest.LastOrderedPlaceStore
import com.vaiinilla.app.data.guest.SchoolJoinCodeStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.location.DeviceLocationProvider
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.GeoCoordinates
import com.vaiinilla.app.domain.model.LastOrderedPlace
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.SpaceResolveResult
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import com.vaiinilla.app.domain.repository.DiscoveryRepository
import com.vaiinilla.app.ui.operational.StaffHarness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

private class RecordingDiscovery : DiscoveryRepository {
    val searches = CopyOnWriteArrayList<Pair<String, GeoCoordinates?>>()
    val places = listOf(place("p1", "Boliche Strike"), place("p2", "Pastelería Vaini"))

    override fun searchEstablishments(
        query: String,
        limit: Int,
        cursor: String?,
        nearTo: GeoCoordinates?,
    ): Result<Pair<List<PublicEstablishment>, String?>> {
        searches += query to nearTo
        return Result.success(places to null)
    }

    override fun getEstablishment(slug: String): Result<PublicEstablishment> = Result.failure(IllegalStateException())

    override fun getGuestCatalog(slug: String): Result<Catalog> = Result.failure(IllegalStateException())

    override fun resolveSpaceToken(token: String): Result<SpaceResolveResult> = Result.failure(IllegalStateException())
}

private fun place(
    id: String,
    name: String,
) = PublicEstablishment(id = id, name = name, slug = id, clientIdLabel = "Nombre", clientIdRequired = false)

private class FixedLocation(
    private val result: () -> GeoCoordinates?,
) : DeviceLocationProvider {
    override suspend fun currentLocation(): GeoCoordinates? = result()
}

private inline fun <reified T : Any> untouched(): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        throw UnsupportedOperationException(T::class.java.simpleName + "." + method.name)
    } as T

/**
 * Dónde pedir (W05): «Pediste aquí» con el último lugar y «Cerca de ti» con la ubicación, su permiso negado y
 * su fallo. Corre en la JVM con Robolectric; no usa el GPS real ni cuenta como teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecorridosDondePedirViewModelTest {
    private val discovery = RecordingDiscovery()
    private val app = RuntimeEnvironment.getApplication()

    private fun viewModel(location: () -> GeoCoordinates? = { null }): GuestDiscoveryViewModel {
        StaffHarness.installMain()
        val vm =
            GuestDiscoveryViewModel(
                discoveryRepository = discovery,
                guestSessionStore = GuestSessionStore(app),
                deviceLocationProvider = FixedLocation(location),
                schoolJoinCodes = SchoolJoinCodeStore(app),
                accessRepository = untouched<AuthorizedAccessRepository>(),
                authRepository = untouched<StudentAuthRepository>(),
                lastOrderedPlaces = LastOrderedPlaceStore(app),
            )
        StaffHarness.awaitUntil("la primera búsqueda") { !vm.state.value.loading && vm.state.value.establishments.isNotEmpty() }
        return vm
    }

    @Test
    fun w05_alEntrarMuestraLosNegociosDeLaBusquedaSinUbicacion() {
        val vm = viewModel()
        assertEquals(listOf("Boliche Strike", "Pastelería Vaini"), vm.state.value.establishments.map { it.name })
        assertEquals("" to null, discovery.searches.first())
    }

    @Test
    fun w05_pedisteAquiMuestraElUltimoLugarYSeVaciaAlOlvidarlo() {
        val store = LastOrderedPlaceStore(app)
        store.clear()
        val vm = viewModel()
        assertNull(vm.state.value.lastPlace)
        store.record(LastOrderedPlace(place("p1", "Boliche Strike"), "Cerveza", 2, Instant.now()))
        vm.loadLastPlace()
        assertEquals("Boliche Strike", vm.state.value.lastPlace?.establishment?.name)
        assertTrue(vm.state.value.lastPlace!!.summary().endsWith("Cerveza y 2 más"))
        store.clear()
        vm.loadLastPlace()
        assertNull(vm.state.value.lastPlace)
    }

    @Test
    fun w05_cercaDeTiBuscaConLaUbicacionYSeApagaVolviendoALaBusquedaNormal() {
        val here = GeoCoordinates(28.63, -106.07)
        val vm = viewModel { here }
        vm.enableNearby()
        StaffHarness.awaitUntil("ordenado por cercanía") { vm.state.value.nearbyOrigin == here && discovery.searches.any { it.second == here } }
        assertEquals(false, vm.state.value.locating)
        assertNull(vm.state.value.locationMessage)
        vm.disableNearby()
        StaffHarness.awaitUntil("búsqueda normal") { vm.state.value.nearbyOrigin == null && discovery.searches.last().second == null }
    }

    @Test
    fun w05_sinUbicacionDisponibleSeDiceYNoSeBuscaPorCercania() {
        val vm = viewModel { null }
        vm.enableNearby()
        StaffHarness.awaitUntil("el aviso") { vm.state.value.locationMessage != null && !vm.state.value.locating }
        assertTrue(vm.state.value.locationMessage!!.contains("GPS"))
        assertNull(vm.state.value.nearbyOrigin)
        assertTrue(discovery.searches.none { it.second != null })
    }

    @Test
    fun w05_siElGpsLanzaUnErrorTambienSeDice() {
        val vm = viewModel { throw SecurityException("sin permiso") }
        vm.enableNearby()
        StaffHarness.awaitUntil("el aviso") { vm.state.value.locationMessage != null && !vm.state.value.locating }
        assertNull(vm.state.value.nearbyOrigin)
    }

    @Test
    fun w05_negarElPermisoDejaElMensajeYLaBusquedaPorNombreSigueSirviendo() {
        val vm = viewModel()
        vm.onNearbyPermissionDenied()
        assertNotNull(vm.state.value.locationMessage)
        assertTrue(vm.state.value.locationMessage!!.contains("nombre"))
        assertNull(vm.state.value.nearbyOrigin)
        vm.updateQuery("boliche")
        StaffHarness.awaitUntil("la búsqueda por nombre") { discovery.searches.any { it.first == "boliche" } }
    }
}
