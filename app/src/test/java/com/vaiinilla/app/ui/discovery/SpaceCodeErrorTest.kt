package com.vaiinilla.app.ui.discovery

import com.vaiinilla.app.core.network.ApiClientException
import org.junit.Assert.assertEquals
import org.junit.Test

/** J05: un código de mesa desactivado dice qué hacer en vez de "No se pudo resolver el espacio.". */
class SpaceCodeErrorTest {
    @Test
    fun `a disabled or unknown code is explained`() {
        // Lo que responde api-dev a un código desactivado (POST /publico/espacios/resolver).
        val error = ApiClientException("ESTABLISHMENT_NOT_FOUND", "No se pudo resolver el espacio.", 404)
        assertEquals(SpaceCodeError.NOT_ACTIVE, SpaceCodeError.message(error))
    }

    @Test
    fun `any other server message is shown as is`() {
        val error = ApiClientException("VALIDATION_ERROR", "El campo 'token' es obligatorio.", 400)
        assertEquals("El campo 'token' es obligatorio.", SpaceCodeError.message(error))
    }
}
