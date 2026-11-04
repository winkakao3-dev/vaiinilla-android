package com.vaiinilla.app.data.admin

import com.vaiinilla.app.domain.admin.AssistantMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreAssistantJsonTest {
    private val json = StoreAssistantJson()

    @Test
    fun `manda la platica con los roles del servidor`() {
        val body =
            Json
                .parseToJsonElement(
                    json.encodeMessages(
                        listOf(AssistantMessage(false, "¿Qué negocio es?"), AssistantMessage(true, "Un boliche")),
                    ),
                ).jsonObject
        val mensajes = body["mensajes"]!!.jsonArray
        assertEquals("asistente", mensajes[0].jsonObject["rol"]!!.jsonPrimitive.content)
        assertEquals("dueno", mensajes[1].jsonObject["rol"]!!.jsonPrimitive.content)
        assertEquals("Un boliche", mensajes[1].jsonObject["texto"]!!.jsonPrimitive.content)
    }

    @Test
    fun `lee una pregunta sin propuesta`() {
        val reply =
            json.parseReply(
                """{"data":{"mensaje":"¿Pagan al final?","opciones":["Sí","No"],"propuesta":null,"quedan_mensajes":37}}""",
            )
        assertEquals("¿Pagan al final?", reply.message)
        assertEquals(listOf("Sí", "No"), reply.options)
        assertNull(reply.proposal)
        assertEquals(37, reply.remainingMessages)
    }

    @Test
    fun `lee la propuesta en palabras y guarda el cuerpo del PATCH tal cual`() {
        val reply =
            json.parseReply(
                """
                {"data":{"mensaje":"Así quedaría.","opciones":[],"quedan_mensajes":20,
                "propuesta":{"cambios":{"tipo":"bar","permite_pago_al_final":true,"entrega_en_espacio":"ambas",
                "franjas_pedido":[{"desde":"16:00","hasta":"01:00"}],"gracia_liberacion_min":10,"fondo_sugerido":"500.00"},
                "motivos":{"tipo":"Tienes barra."}}}}
                """.trimIndent(),
            )
        val proposal = reply.proposal!!
        val byField = proposal.changes.associateBy { it.field }
        assertEquals("Bar o cantina", byField.getValue("tipo").value)
        assertEquals("Tienes barra.", byField.getValue("tipo").reason)
        assertEquals("Al final, con cuenta abierta", byField.getValue("permite_pago_al_final").value)
        assertEquals("Las dos: el cliente elige", byField.getValue("entrega_en_espacio").value)
        assertEquals("16:00 a 01:00", byField.getValue("franjas_pedido").value)
        assertEquals("10 min", byField.getValue("gracia_liberacion_min").value)
        assertEquals("$500.00", byField.getValue("fondo_sugerido").value)
        val patch = Json.parseToJsonElement(proposal.patchBody).jsonObject
        assertEquals("bar", patch["tipo"]!!.jsonPrimitive.content)
        assertEquals(6, patch.size)
    }
}
