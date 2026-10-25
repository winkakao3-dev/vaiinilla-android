package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.operational.itemRejectedNotice
import com.vaiinilla.app.ui.operational.kitchenActionNotice
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

/** Dónde quedó el dinero al cancelar o rechazar un pedido pagado, y que una lista no se rompe por un valor nuevo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RefundOutcomeContractTest {
    private val envelope = Json.parseToJsonElement(ScreenshotFixtures.rawCreatedOrder()).jsonObject
    private val order: JsonElement = envelope.getValue("data")
    private val meta: JsonElement = envelope.getValue("meta")
    private val contract = OrderContractJson()

    private fun envelopeWith(vararg extra: Pair<String, JsonElement>): String =
        buildJsonObject {
            put(
                "data",
                buildJsonObject {
                    put("pedido", order)
                    extra.forEach { (key, value) -> put(key, value) }
                },
            )
            put("meta", meta)
        }.toString()

    private fun amount(value: String): JsonObject = buildJsonObject { put("monto", JsonPrimitive(value)) }

    @Test
    fun `cancelar un pedido pagado en efectivo dice cuanto entregar`() {
        val parsed = contract.parseCancelledOrder(envelopeWith("efectivo_devuelto" to amount("120.00")))
        assertEquals(BigDecimal("120.00"), parsed.refund?.cashHandedBack)
        val canceled = parsed.copy(summary = parsed.summary.copy(state = OrderState.CANCELED, folio = 7))
        assertEquals("Comanda #7 rechazada. Entrega \$120 en efectivo al cliente.", kitchenActionNotice(canceled))
    }

    @Test
    fun `una devolucion pendiente manda a Devoluciones`() {
        val parsed = contract.parseCancelledOrder(envelopeWith("devolucion_pendiente" to amount("45.50")))
        assertEquals(BigDecimal("45.50"), parsed.refund?.pendingForCashier)
    }

    @Test
    fun `sin dinero que mover no hay aviso extra`() {
        val parsed = contract.parseCancelledOrder(envelopeWith("efectivo_devuelto" to JsonNull))
        assertNull(parsed.refund)
    }

    @Test
    fun `al quitar un articulo la devolucion es la tarea de Caja y el saldo va aparte`() {
        val pending = contract.parseRejectedItem(envelopeWith("devolucion" to amount("30.00")))
        assertEquals(BigDecimal("30.00"), pending.refund?.pendingForCashier)
        val balance = contract.parseRejectedItem(envelopeWith("saldo_devuelto" to amount("30.00")))
        assertEquals(BigDecimal("30.00"), balance.refund?.returnedToBalance)
        assertEquals(
            "Se quitó un artículo de la comanda #3472. Se regresaron \$30 a su saldo.",
            itemRejectedNotice(balance),
        )
    }

    @Test
    fun `un pedido con un valor desconocido no tira la lista entera`() {
        val unknown = order.jsonObject.toMutableMap().apply { put("metodo_pago", JsonPrimitive("bitcoin")) }
        val raw =
            buildJsonObject {
                put("data", JsonArray(listOf(JsonObject(unknown), order)))
                put("meta", meta)
            }.toString()
        assertEquals(1, contract.parseOrderList(raw).size)
        assertEquals(1, contract.parseOrderListPage(raw).orders.size)
    }
}
