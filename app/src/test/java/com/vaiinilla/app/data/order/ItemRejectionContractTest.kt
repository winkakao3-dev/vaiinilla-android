package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.ItemRefundMethod
import com.vaiinilla.app.domain.model.OrderItemRejection
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.domain.model.rejectedItemsHint
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ItemRejectionContractTest {
    @Test
    fun `lee el rechazo del articulo y arma el aviso para el cliente`() {
        val base = ScreenshotFixtures.sampleOrder()
        val first = base.items.first()
        val order =
            base.copy(
                items =
                    listOf(
                        first.copy(
                            id = 1,
                            productName = "Torta",
                            rejection = OrderItemRejection("Se terminó el pan", "80.00"),
                        ),
                        first.copy(id = 2, productName = "Tacos"),
                    ),
            )
        assertEquals(listOf("Tacos"), order.activeItems.map { it.productName })
        assertEquals("Se quitó Torta: Se terminó el pan.", order.rejectedItemsHint)
        assertNull(base.rejectedItemsHint)
    }

    @Test
    fun `el contrato trae rechazo por articulo`() {
        val raw =
            ScreenshotFixtures.rawCreatedOrder().replaceFirst(
                "\"opciones\"",
                "\"rechazo\":{\"motivo\":\"Sin pan\",\"monto\":\"80.00\"},\"opciones\"",
            )
        val parsed = OrderContractJson().parseOrderDetail(raw)
        assertEquals(OrderItemRejection("Sin pan", "80.00"), parsed.items.first().rejection)
    }

    @Test
    fun `el cuerpo para quitar un articulo lleva solo motivo y version`() {
        val body = Json.parseToJsonElement(OrderContractJson().encodeItemRejection(3, "  Sin pan  ")).jsonObject

        assertEquals(setOf("version_esperada", "motivo"), body.keys)
        assertEquals("3", body.getValue("version_esperada").jsonPrimitive.content)
        assertEquals("Sin pan", body.getValue("motivo").jsonPrimitive.content)
    }

    @Test
    fun `la respuesta de quitar un articulo trae el pedido y la devolucion manual`() {
        val pedido = Json.parseToJsonElement(ScreenshotFixtures.rawCreatedOrder()).jsonObject.getValue("data")
        val raw =
            buildJsonObject {
                put(
                    "data",
                    buildJsonObject {
                        put("pedido", pedido)
                        put(
                            "rechazo",
                            buildJsonObject {
                                put("pedido_item_id", 7)
                                put("motivo", "Sin pan")
                                put("monto", "80.00")
                                put("metodo_reembolso", "manual")
                                put("stripe_refund_id", JsonNull)
                            },
                        )
                        put("devolucion", buildJsonObject { put("metodo_original", "efectivo+terminal") })
                    },
                )
                put("meta", JsonObject(emptyMap()))
                put("error", JsonNull)
            }.toString()

        val outcome = OrderContractJson().parseItemRejection(raw)

        assertEquals(7, outcome.itemId)
        assertEquals("80.00", outcome.amount)
        assertEquals(ItemRefundMethod.MANUAL, outcome.refundMethod)
        assertEquals("efectivo+terminal", outcome.manualRefundOriginalMethod)
        assertEquals(ScreenshotFixtures.sampleOrder().summary.folio, outcome.order.summary.folio)
    }
}
