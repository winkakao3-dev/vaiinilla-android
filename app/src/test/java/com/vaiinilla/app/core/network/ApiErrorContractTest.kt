package com.vaiinilla.app.core.network

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Contrato de errores app <-> backend: el sobre `{data, meta, error{code,message,details}}` que manda la API
 * y la forma en que la app lo convierte en `ApiClientException`. Si el backend cambia el sobre, falla aquí
 * antes de que lo note un cliente.
 */
class ApiErrorContractTest {
    private val parser = ApiResponseParser()

    private fun envelope(
        code: String,
        message: String,
        details: String = "null",
    ) = """{"data":null,"meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},""" +
        """"error":{"code":"$code","message":"$message","details":$details}}"""

    @Test
    fun `el codigo y el mensaje del backend llegan intactos`() {
        listOf(400, 401, 403, 404, 409, 422, 500).forEach { status ->
            val error = parser.parseError(envelope("PEDIDO_CERRADO", "El pedido ya no admite cambios."), status)
            assertEquals("PEDIDO_CERRADO", error.code)
            assertEquals("El pedido ya no admite cambios.", error.message)
            assertEquals(status, error.httpStatus)
            assertNull(error.details)
        }
    }

    @Test
    fun `details viaja completo para que la app lea intentos_restantes`() {
        val error =
            parser.parseError(
                envelope("PIN_INCORRECTO", "PIN incorrecto.", """{"intentos_restantes":2}"""),
                httpStatus = 403,
            )
        assertEquals(
            "2",
            error.details!!
                .jsonObject
                .getValue("intentos_restantes")
                .jsonPrimitive.content,
        )
    }

    @Test
    fun `Retry-After de un 429 se conserva`() {
        val error =
            parser.parseError(
                envelope("DEMASIADOS_INTENTOS", "Espera."),
                httpStatus = 429,
                retryAfterSeconds = 30,
            )
        assertEquals(30L, error.retryAfterSeconds)
        assertEquals(429, error.httpStatus)
    }

    @Test
    fun `un cuerpo que no es el sobre cae a HTTP_status sin romper`() {
        listOf("", "<html>Bad Gateway</html>", "not json", """{"error":"texto"}""").forEach { raw ->
            val error = parser.parseError(raw, httpStatus = 502)
            assertEquals("HTTP_502", error.code)
            assertNotNull(error.message)
            assertEquals(502, error.httpStatus)
        }
    }

    @Test
    fun `campos nuevos del backend no rompen la lectura`() {
        val raw =
            """{"data":null,"meta":{"page":1,"total_pages":1,"total_items":0,"cursor":null,"nuevo":true},""" +
                """"error":{"code":"X","message":"m","details":null,"campo_futuro":1},"extra":[]}"""
        assertEquals("X", parser.parseError(raw, httpStatus = 400).code)
    }
}
