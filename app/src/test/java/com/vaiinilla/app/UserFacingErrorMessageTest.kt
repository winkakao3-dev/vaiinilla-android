package com.vaiinilla.app

import com.vaiinilla.app.core.network.toUserFacingMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class UserFacingErrorMessageTest {
    @Test
    fun `hides raw host resolution details`() {
        val error = UnknownHostException("Unable to resolve host localhost.invalid")

        assertEquals(
            "No pudimos conectar con Vaiinilla. Revisa tu conexión e inténtalo de nuevo.",
            error.toUserFacingMessage("Fallback"),
        )
    }

    @Test
    fun `hides timeout details`() {
        assertEquals(
            "La conexión tardó demasiado. Inténtalo de nuevo.",
            SocketTimeoutException("timeout").toUserFacingMessage("Fallback"),
        )
    }

    @Test
    fun `preserves an api message`() {
        assertEquals(
            "El código ya fue utilizado.",
            IllegalStateException("El código ya fue utilizado.").toUserFacingMessage("Fallback"),
        )
    }

    @Test
    fun `uses fallback when error has no message`() {
        assertEquals("Fallback", IllegalStateException().toUserFacingMessage("Fallback"))
    }
}
