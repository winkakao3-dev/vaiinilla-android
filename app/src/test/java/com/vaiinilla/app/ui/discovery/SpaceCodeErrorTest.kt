package com.vaiinilla.app.ui.discovery

import com.vaiinilla.app.core.network.ApiClientException
import org.junit.Assert.assertEquals
import org.junit.Test

/** J05: un código de mesa desactivado y uno que no existe dicen qué hacer en vez de "No se pudo resolver el espacio.". */
class SpaceCodeErrorTest {
    @Test
    fun `a disabled code says it is not active`() {
        // Lo que responde api-dev a un código desactivado (POST /publico/espacios/resolver).
        val error = ApiClientException("SPACE_NOT_ACTIVE", "El espacio no está activo.", 422)
        assertEquals(SpaceCodeError.NOT_ACTIVE, SpaceCodeError.message(error))
    }

    @Test
    fun `an unknown code says to check it`() {
        // Lo que responde api-dev a un token que no existe ("9999").
        val error = ApiClientException("ESTABLISHMENT_NOT_FOUND", "No se pudo resolver el espacio.", 404)
        assertEquals(SpaceCodeError.NOT_FOUND, SpaceCodeError.message(error))
    }

    @Test
    fun `any other server message is shown as is`() {
        val error = ApiClientException("VALIDATION_ERROR", "El campo 'token' es obligatorio.", 400)
        assertEquals("El campo 'token' es obligatorio.", SpaceCodeError.message(error))
    }
}
