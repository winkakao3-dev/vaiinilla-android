package com.vaiinilla.app.ui.parentmode

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Mis hijos» de la cartera del tutor (apoyo de W06 y W07): cargar, aceptar el aviso de privacidad, altas y bajas, y
 * que al cerrar sesión no quede nada del tutor anterior. Corre en la JVM; no cuenta como recorrido en teléfono.
 */
class RecorridosMisHijosTest {
    private val ana = ParentFixtures.ana
    private val tilin = ParentFixtures.tilin

    private fun state(
        guardianship: FakeGuardianship,
        catalog: FakeCatalog = FakeCatalog(),
        onNames: (Map<String, String>) -> Unit = {},
    ) = ChildrenState(ParentFixtures.services(guardianship, catalog = catalog), onNames)

    @Test
    fun cargarTraeALosHijosYAvisaSusNombres() =
        runBlocking {
            var names: Map<String, String> = emptyMap()
            val state = state(FakeGuardianship(listOf(ana, tilin))) { names = it }
            assertFalse(state.hasLoaded)
            state.load()
            assertTrue(state.hasLoaded)
            assertEquals(listOf("u-ana", "u-tilin"), state.children.map { it.userId })
            assertEquals(setOf("u-ana", "u-tilin"), names.keys)
            assertNull(state.error)
        }

    @Test
    fun siNoCargaYNoHayHijosDiceQueNoPudoTraerlos() =
        runBlocking {
            val failing =
                object : FakeGuardianship() {
                    override fun dependents() =
                        Result.failure<List<com.vaiinilla.app.domain.guardianship.Dependent>>(
                            java.io.IOException("sin red"),
                        )
                }
            val state = state(failing)
            state.load()
            assertTrue(state.hasLoaded)
            assertTrue(state.children.isEmpty())
            assertEquals(true, state.error?.isNotBlank())
        }

    @Test
    fun siNoCargaPeroYaHabiaHijosLaListaSeConservaSinError() =
        runBlocking {
            val failing =
                object : FakeGuardianship(listOf(ana)) {
                    override fun dependents() =
                        Result.failure<List<com.vaiinilla.app.domain.guardianship.Dependent>>(
                            java.io.IOException("sin red"),
                        )
                }
            val again = ChildrenState(ParentFixtures.services(failing))
            again.upsert(ana)
            again.load()
            assertEquals(1, again.children.size)
            assertNull(again.error)
        }

    @Test
    fun aceptarElAvisoDePrivacidadQuitaLoQueFaltabaDeConsentimiento() =
        runBlocking {
            val state = state(FakeGuardianship(listOf(ana, tilin)))
            state.load()
            assertEquals(listOf("u-ana"), state.childrenNeedingConsent.map { it.userId })
            state.acceptPrivacyNotice("v1", "key-1")
            assertTrue(state.childrenNeedingConsent.isEmpty())
        }

    @Test
    fun unHijoNuevoSeAgregaYUnoQueYaEstabaSeActualizaSinDuplicarse() {
        val state = state(FakeGuardianship())
        state.upsert(ana)
        state.upsert(tilin)
        state.upsert(ana.copy(group = "4°A"))
        assertEquals(2, state.children.size)
        assertEquals("4°A", state.children.first { it.userId == "u-ana" }.group)
        state.remove("u-ana")
        assertEquals(listOf("u-tilin"), state.children.map { it.userId })
    }

    @Test
    fun alCerrarSesionNoQuedaNadaDelTutorAnterior() =
        runBlocking {
            val state = state(FakeGuardianship(listOf(ana)))
            state.load()
            state.loadParentAccess()
            state.offeredConsent = true
            state.reset()
            assertTrue(state.children.isEmpty())
            assertFalse(state.hasLoaded)
            assertNull(state.isParentUnlocked)
            assertFalse(state.offeredConsent)
        }

    @Test
    fun siNoSePuedeConsultarElDesbloqueoDelTutorSeDejaPasar() =
        runBlocking {
            val state = state(FakeGuardianship(), catalog = FakeCatalog(status = null))
            state.loadParentAccess()
            assertEquals(true, state.isParentUnlocked)
        }
}
