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
    fun `el contexto va primero como mensaje del cliente y la platica cabe en el tope`() {
        val turns =
            (1..45).map { CustomerAssistantTurn(fromCustomer = it % 2 == 1, text = "m$it") }
        val mensajes =
            Json.parseToJsonElement(json.encodeTurns(turns, context = " Estoy en Mesa 3. ")).jsonObject["mensajes"]!!.jsonArray
        assertEquals(CustomerAssistantJson.MAX_MESSAGES, mensajes.size)
        assertEquals("cliente", mensajes.first().jsonObject["rol"]!!.jsonPrimitive.content)
        assertEquals("Estoy en Mesa 3.", mensajes.first().jsonObject["texto"]!!.jsonPrimitive.content)
        assertEquals("m45", mensajes.last().jsonObject["texto"]!!.jsonPrimitive.content)
        val sinContexto = Json.parseToJsonElement(json.encodeTurns(turns.take(1), context = "  ")).jsonObject["mensajes"]!!.jsonArray
        assertEquals(1, sinContexto.size)
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

    @Test
    fun `manda el lugar del menu y lee lo que pide elegir opciones`() {
        val body = Json.parseToJsonElement(json.encodeTurns(emptyList(), "est-1")).jsonObject
        assertEquals("est-1", body["establecimiento_id"]!!.jsonPrimitive.content)
        val order =
            json
                .parseReply(
                    """
                    {"data":{"mensaje":"Va","pedido":{"lineas":[],"total":"0.00","no_disponibles":[],
                    "por_elegir":[{"producto_id":12,"nombre":"Michelada","precio":"70.00","requiere_opciones":true}]}}}
                    """.trimIndent(),
                ).order!!
        assertEquals(listOf("Michelada"), order.toChoose)
        assertTrue(order.lines.isEmpty())
    }

    @Test
    fun `lee las pistas del servidor como tarjetas, sin repetir y maximo cuatro`() {
        val reply =
            json.parseReply(
                """
                {"data":{"mensaje":"Hay dos libres","pistas":[
                  {"espacio_id":7,"nombre":"Pista 1","libre_ahora":true,"libre_hasta":"18:00","ocupada_hasta":null,
                    "precio_hora":{"efectivo_saldo":"250.00","tarjeta":"262.50"},"capacidad":6},
                  {"espacio_id":"8","nombre":"Pista 2","libre_ahora":false,"libre_hasta":null,"ocupada_hasta":"19:30",
                    "precio_hora":{"efectivo_saldo":"250.00","tarjeta":"250.00"},"capacidad":null},
                  {"espacio_id":7,"nombre":"Pista 1 otra vez"},
                  {"nombre":"Sin id"},
                  {"espacio_id":9,"nombre":"Pista 3","precio_hora":"300.00"},
                  {"espacio_id":10,"nombre":"Pista 4"},
                  {"espacio_id":11,"nombre":"Pista 5"}]}}
                """.trimIndent(),
            )
        assertEquals(listOf(7, 8, 9, 10), reply.courts.map { it.courtId })
        val first = reply.courts[0]
        assertTrue(first.freeNow)
        assertEquals("18:00", first.freeUntil)
        assertEquals("250.00", first.pricePerHour)
        assertEquals("262.50", first.cardPricePerHour)
        assertEquals(6, first.capacity)
        assertEquals("19:30", reply.courts[1].busyUntil)
        assertNull(reply.courts[1].capacity)
        assertEquals("300.00", reply.courts[2].pricePerHour)
        assertNull(reply.courts[3].pricePerHour)
    }

    @Test
    fun `sin pistas la respuesta trae la lista vacia`() {
        assertTrue(json.parseReply("""{"data":{"mensaje":"Hola"}}""").courts.isEmpty())
    }
}
