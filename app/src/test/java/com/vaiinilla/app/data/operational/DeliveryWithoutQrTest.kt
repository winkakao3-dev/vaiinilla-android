package com.vaiinilla.app.data.operational

import com.vaiinilla.app.data.order.OrderDetailDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** C01/D09/I03/M07: un pedido de una cuenta abierta (`entrega_sin_qr`) se entrega sin escanear. */
class DeliveryWithoutQrTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `the board order carries entrega_sin_qr and skips the QR`() {
        // Lo que manda api-dev en GET /espacios/tablero (Mesa 1 del sandbox, 23 oct).
        val raw =
            """
            {"espacio":{"id":1,"nombre":"Mesa 1","tipo":"mesa"},"llamada":null,"pedidos":[{"id":"21abe1d9",
            "folio":16,"destino":"en_espacio","sesion_espacio_id":"4929b7ed","sesion_espacio_estado":"abierta",
            "entrega_sin_qr":true,"estado":"listo","version":3,"pago_diferido":false,"pago_pendiente":false,
            "cliente":{"nombre":"Agente Cliente"},"items_resumen":"1× Café americano",
            "actualizado_en":"2026-10-23T15:28:13.527Z"}],"rechazados_recientes":[]}
            """.trimIndent()
        val order =
            json
                .decodeFromString<BoardTableDto>(raw)
                .toDomain()
                .orders
                .single()
        assertTrue(order.deliveryWithoutQr)
        assertFalse(order.needsQrToDeliver(deliveryRequiresQr = true))
    }

    @Test
    fun `an older server without the field keeps asking for the QR`() {
        val raw =
            """
            {"id":"x","folio":3,"estado":"listo","version":1,"items_resumen":"1× Galleta",
            "actualizado_en":"2026-10-23T15:28:13.527Z"}
            """.trimIndent()
        val order = json.decodeFromString<BoardOrderDto>(raw).toDomain()
        assertFalse(order.deliveryWithoutQr)
        assertTrue(order.needsQrToDeliver(deliveryRequiresQr = true))
        assertFalse(order.needsQrToDeliver(deliveryRequiresQr = false))
    }

    @Test
    fun `an order on the table tab is delivered without collecting first`() {
        val raw =
            """
            {"id":"x","folio":7,"estado":"listo","version":2,"items_resumen":"1× Café americano",
            "actualizado_en":"2026-10-27T15:28:13.527Z","entrega_sin_qr":true,
            "pago_pendiente":true,"pago_diferido":true}
            """.trimIndent()
        val order = json.decodeFromString<BoardOrderDto>(raw).toDomain()
        assertTrue(order.paymentPending)
        assertTrue(order.payAtEnd)
        assertFalse(order.mustCollectBeforeDelivery)
    }

    @Test
    fun `an unpaid order outside a tab still has to be collected`() {
        val raw =
            """
            {"id":"x","folio":8,"estado":"listo","version":2,"items_resumen":"1× Galleta",
            "actualizado_en":"2026-10-27T15:28:13.527Z","pago_pendiente":true}
            """.trimIndent()
        val order = json.decodeFromString<BoardOrderDto>(raw).toDomain()
        assertFalse(order.payAtEnd)
        assertTrue(order.mustCollectBeforeDelivery)
    }

    @Test
    fun `the order detail reads entrega_sin_qr`() {
        val dto = json.decodeFromString<OrderDetailDto>(ORDER)
        assertTrue(dto.deliveryWithoutQr)
    }

    private companion object {
        // GET /pedidos/{id} del cliente en api-dev, recortado a lo que lee la app.
        const val ORDER =
            """{"id":"21abe1d9","folio":16,"fecha_operativa":"2026-10-23","estado":"listo","metodo_pago":"efectivo",
            "destino":"en_espacio","espacio":{"id":1,"nombre":"Mesa 1","tipo":"mesa"},"entrega_sin_qr":true,
            "subtotal":"30.00","ahorro_combinado":"0.00","cashback_otorgado":"0.00","total":"30.00","version":3,
            "creado_en":"2026-10-23T15:27:21.328Z","actualizado_en":"2026-10-23T15:28:13.527Z"}"""
    }
}
