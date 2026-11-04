package com.vaiinilla.app.domain.model

import com.vaiinilla.app.data.admin.AdminBusinessJson
import com.vaiinilla.app.domain.admin.StoreFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantModeTest {
    @Test
    fun `desconocido vale los dos y solo personal apaga el de clientes`() {
        assertEquals(AssistantMode.BOTH, AssistantMode.fromWire(null))
        assertFalse(AssistantMode.STAFF.forCustomers)
        assertTrue(AssistantMode.STAFF.forStaff)
        assertFalse(AssistantMode.NONE.forStaff)
    }

    @Test
    fun `el flujo manda asistente_modo solo si el servidor lo conoce`() {
        val json = AdminBusinessJson()
        val base = StoreFlow("boliche", false, true, true, 5, emptyList())
        assertFalse("asistente_modo" in Json.parseToJsonElement(json.encodeFlow(base)).jsonObject)
        val body = Json.parseToJsonElement(json.encodeFlow(base.copy(assistantMode = AssistantMode.STAFF))).jsonObject
        assertEquals("personal", body["asistente_modo"]!!.jsonPrimitive.content)
    }
}
