package com.vaiinilla.app

import com.vaiinilla.app.core.network.ApiClientException
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

    @Test
    fun `hides raw server error codes for 5xx api failures`() {
        val error =
            ApiClientException(
                code = "HTTP_503",
                message = "La API respondió con código 503.",
                httpStatus = 503,
            )

        assertEquals(
            "Tuvimos un problema en el servidor. Intenta de nuevo en unos momentos.",
            error.toUserFacingMessage("Fallback"),
        )
    }

    @Test
    fun `preserves a 4xx api message instead of the generic server error copy`() {
        val error =
            ApiClientException(
                code = "VALIDATION",
                message = "El código ya fue utilizado.",
                httpStatus = 422,
            )

        assertEquals("El código ya fue utilizado.", error.toUserFacingMessage("Fallback"))
    }
}
