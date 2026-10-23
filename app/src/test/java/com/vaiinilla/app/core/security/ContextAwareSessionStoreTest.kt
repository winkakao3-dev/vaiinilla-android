package com.vaiinilla.app.core.security

import com.vaiinilla.app.core.notifications.unregisterPath
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Base64

/** K03, M05, M06: el token FCM se vuelve a registrar cuando entra otra cuenta, no en cada refresh. */
class ContextAwareSessionStoreTest {
    private class MemoryStore : SecureSessionStore {
        var token: String? = null

        override fun saveAccessToken(token: String) {
            this.token = token
        }

        override fun readAccessToken(): String? = token

        override fun clear() {
            token = null
        }
    }

    private fun jwt(
        uid: String,
        exp: Long,
    ): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val payload =
            """{"uid":"$uid","rol":"cliente","establecimiento_id":"esc-1","membresia_id":"m-$uid","exp":$exp}"""
        return "h." + encoder.encodeToString(payload.toByteArray()) + ".s"
    }

    @Test
    fun cambiarDeCuentaVuelveARegistrarYUnRefreshNo() {
        var registros = 0
        val store = ContextAwareSessionStore(MemoryStore()) { registros += 1 }

        store.saveAccessToken(jwt("mama", 1))
        store.saveAccessToken(jwt("mama", 2))
        assertEquals(1, registros)

        store.saveAccessToken(jwt("papa", 3))
        assertEquals(2, registros)
        assertEquals(jwt("papa", 3), store.readAccessToken())
    }

    @Test
    fun trasCerrarSesionLaMismaCuentaSeRegistraOtraVez() {
        var registros = 0
        val store = ContextAwareSessionStore(MemoryStore()) { registros += 1 }

        store.saveAccessToken(jwt("papa", 1))
        store.clear()
        store.saveAccessToken(jwt("papa", 2))

        assertEquals(2, registros)
    }

    @Test
    fun laRutaDeBajaEscapaElTokenFcm() {
        assertEquals("devices/abc%3AAPA91b-x_y", unregisterPath("abc:APA91b-x_y"))
    }
}
