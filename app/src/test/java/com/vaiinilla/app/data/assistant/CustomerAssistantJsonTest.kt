package com.vaiinilla.app.data.assistant

import com.vaiinilla.app.domain.assistant.CustomerAssistantTurn
import com.vaiinilla.app.domain.repository.CallReason
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerAssistantJsonTest {
    private val json = CustomerAssistantJson()

    @Test
    fun `manda la platica con los roles del servidor y solo los ultimos mensajes`() {
        val turns =
            (1..45).map { CustomerAssistantTurn(fromCustomer = it % 2 == 1, text = "m$it") }
        val mensajes = Json.parseToJsonElement(json.encodeTurns(turns)).jsonObject["mensajes"]!!.jsonArray
        assertEquals(CustomerAssistantJson.MAX_MESSAGES, mensajes.size)
        assertEquals("cliente", mensajes.last().jsonObject["rol"]!!.jsonPrimitive.content)
        assertEquals("m45", mensajes.last().jsonObject["texto"]!!.jsonPrimitive.content)
        assertEquals("asistente", mensajes[mensajes.size - 2].jsonObject["rol"]!!.jsonPrimitive.content)
    }

    @Test
    fun `lee tarjetas, pedido y la oferta de llamar al mesero`() {
        val reply =
            json.parseReply(
                """
                {"data":{"mensaje":" Así queda ",
                  "productos":[{"producto_id":10,"nombre":"Nachos","precio":"95.00","imagen_url":null,
                    "disponible":true,"requiere_opciones":false,"minutos":10},
                    {"producto_id":11,"nombre":"Alitas","precio":"110.00","disponible":false}],
                  "pedido":{"lineas":[{"producto_id":12,"nombre":"Michelada","precio":"70.00",
                    "requiere_opciones":true,"cantidad":2,"nota":" bien fría "}],
                    "total":"140.00","no_disponibles":["Alitas"]},
                  "llamar_mesero":{"motivo":"utensilios","detalle":"Servilletas"},
                  "opciones":["Sí","", "Cambiar algo"],"quedan_mensajes":37}}
                """.trimIndent(),
            )
        assertEquals("Así queda", reply.message)
        assertEquals(listOf(10, 11), reply.products.map { it.productId })
        assertNull(reply.products[0].imageUrl)
        assertFalse(reply.products[1].available)
        val line = reply.order!!.lines.single()
        assertEquals(2, line.quantity)
        assertEquals("bien fría", line.note)
        assertTrue(line.product.needsOptions)
        assertEquals(listOf("Alitas"), reply.order!!.unavailable)
        assertEquals(CallReason.UTENSILIOS, reply.waiter!!.reason)
        assertEquals(listOf("Sí", "Cambiar algo"), reply.options)
        assertEquals(37, reply.remainingMessages)
    }

    @Test
    fun `ignora un motivo de mesero desconocido y un pedido vacio`() {
        val reply =
            json.parseReply(
                """{"data":{"mensaje":"Hola","pedido":{"lineas":[],"no_disponibles":[]},"llamar_mesero":{"motivo":"x"}}}""",
            )
        assertNull(reply.order)
        assertNull(reply.waiter)
        assertTrue(reply.products.isEmpty())
    }
}
