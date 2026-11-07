package com.vaiinilla.app.core.network

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiErrorMapperTest {
    private fun api(
        status: Int,
        code: String = "HTTP_$status",
        message: String = "mensaje del servidor",
    ) = ApiClientException(code = code, message = message, httpStatus = status)

    @Test
    fun `cada fallo de transporte tiene su tipo`() {
        assertEquals(ApiFailure.NoConnection, ApiErrorMapper.map(UnknownHostException("x")))
        assertEquals(ApiFailure.Timeout, ApiErrorMapper.map(SocketTimeoutException("timeout")))
        assertEquals(ApiFailure.ServerUnreachable, ApiErrorMapper.map(ConnectException("refused")))
    }

    @Test
    fun `un JSON malformado es una respuesta que no se entiende`() {
        assertEquals(
            ApiFailure.BadResponse,
            ApiErrorMapper.map(SerializationException("Unexpected JSON token at offset 4")),
        )
        assertEquals(
            ApiFailure.BadResponse,
            ApiErrorMapper.map(IllegalStateException("envuelto", SerializationException("raro"))),
        )
    }

    @Test
    fun `un 401 es sesion caducada y un 5xx es del servidor`() {
        val expired = ApiErrorMapper.map(api(401, "UNAUTHENTICATED"))
        assertTrue(expired is ApiFailure.Rejected && expired.sessionExpired)
        assertEquals("Tu sesión caducó. Vuelve a iniciar sesión.", expired.message())

        val server = ApiErrorMapper.map(api(503))
        assertTrue(server is ApiFailure.ServerError)
        assertEquals("Tuvimos un problema en el servidor. Intenta de nuevo en unos momentos.", server.message())
    }

    @Test
    fun `un 4xx conserva el mensaje del servidor y el retry-after`() {
        val failure = ApiErrorMapper.map(ApiClientException("RATE", "Espera un poco", 429, retryAfterSeconds = 30))
        assertTrue(failure is ApiFailure.Rejected && failure.retryAfterSeconds == 30L)
        assertEquals("Espera un poco", failure.message())
    }

    @Test
    fun `sin sesion y errores desconocidos tambien salen tipados`() {
        assertTrue(ApiErrorMapper.map(MissingAccessTokenException()) is ApiFailure.NotSignedIn)
        assertEquals("Fallback", ApiErrorMapper.map(IllegalStateException()).message("Fallback"))
        assertEquals("Fallback", ApiErrorMapper.map(null).message("Fallback"))
    }

    @Test
    fun `toApiResult nunca lanza`() {
        val ok = Result.success(7).toApiResult()
        assertEquals(ApiResult.Success(7), ok)
        val bad = Result.failure<Int>(SocketTimeoutException("timeout")).toApiResult()
        assertEquals(ApiResult.Failure(ApiFailure.Timeout), bad)
    }

    @Test
    fun `mapApiFailure traduce solo el error del servidor`() {
        val mapped = Result.failure<Int>(api(409, "CONFLICT")).mapApiFailure { IllegalArgumentException(it.code) }
        assertEquals("CONFLICT", mapped.exceptionOrNull()?.message)
        val untouched = Result.failure<Int>(SocketTimeoutException("t")).mapApiFailure { IllegalArgumentException() }
        assertTrue(untouched.exceptionOrNull() is SocketTimeoutException)
        assertEquals(1, Result.success(1).mapApiFailure { IllegalArgumentException() }.getOrNull())
    }
}
