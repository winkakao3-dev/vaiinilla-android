package com.vaiinilla.app

import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * La sesión se conserva al cerrar la app (A07): el lugar y la mesa siguen donde estaban al abrirla de nuevo, y
 * cerrar sesión los borra. Un store nuevo sobre el mismo disco hace de «abrir la app otra vez». Corre en la JVM
 * con Robolectric; no cuenta como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecorridosSesionPersisteTest {
    private val app = RuntimeEnvironment.getApplication()

    private val venue =
        GuestVenueContext(
            establishment =
                PublicEstablishment(
                    id = "est-usagi",
                    name = "USAGI",
                    slug = "usagi",
                    clientIdLabel = "Nombre",
                    clientIdRequired = false,
                ),
            space = PublicSpace(id = 2, name = "Mesa 2", type = "mesa"),
        )

    @Before
    fun setUp() {
        GuestSessionStore(app, InMemoryGuestKeyVault()).clearAll()
    }

    @Test
    fun a07_alAbrirLaAppOtraVezSigueEnElMismoLugarYLaMismaMesa() {
        GuestSessionStore(app, InMemoryGuestKeyVault()).saveVenue(venue)
        val reopened = GuestSessionStore(app, InMemoryGuestKeyVault())
        assertEquals("USAGI", reopened.readVenue()?.establishment?.name)
        assertEquals("Mesa 2", reopened.readVenue()?.space?.name)
    }

    @Test
    fun a07_alCerrarSesionNoQuedaNiElLugarNiLaLlaveDelInvitado() {
        val vault = InMemoryGuestKeyVault()
        val store = GuestSessionStore(app, vault)
        store.saveVenue(venue)
        store.saveGuest("Robot", "llave-1")
        assertEquals("llave-1", store.readGuestKey())
        store.clearAll()
        val reopened = GuestSessionStore(app, vault)
        assertNull(reopened.readVenue())
        assertNull(reopened.readGuestKey())
        assertNull(reopened.readGuestName())
    }

    @Test
    fun a07_cambiarDeMesaGuardaLaNuevaYNoLaVieja() {
        val store = GuestSessionStore(app, InMemoryGuestKeyVault())
        store.saveVenue(venue)
        store.saveVenue(venue.copy(space = PublicSpace(id = 5, name = "Mesa 5", type = "mesa")))
        assertEquals("Mesa 5", GuestSessionStore(app, InMemoryGuestKeyVault()).readVenue()?.space?.name)
    }
}
