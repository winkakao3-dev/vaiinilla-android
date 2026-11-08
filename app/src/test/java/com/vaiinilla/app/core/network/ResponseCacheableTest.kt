package com.vaiinilla.app.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseCacheableTest {
    @Test
    fun `guarda la consulta sin parametros de las rutas con pintado instantaneo`() {
        assertTrue(isCacheable("pedidos", emptyMap()))
        assertTrue(isCacheable("wallets/me", emptyMap()))
        assertTrue(isCacheable("catalogo", emptyMap()))
    }

    @Test
    fun `no guarda las consultas con parametros porque cada valor seria una entrada nueva`() {
        assertFalse(isCacheable("pedidos", mapOf("actualizado_desde" to "2026-11-06T10:00:00Z")))
        assertFalse(isCacheable("pedidos", mapOf("cursor" to "abc")))
    }

    @Test
    fun `no guarda rutas fuera de la lista`() {
        assertFalse(isCacheable("estado-operativo", emptyMap()))
    }
}
