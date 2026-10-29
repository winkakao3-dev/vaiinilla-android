package com.vaiinilla.app.ui.components

import com.vaiinilla.app.data.order.OrderDetailDto
import com.vaiinilla.app.data.order.toDomain
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PaymentPart
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargedPaymentLabelTest {
    private fun summary(
        chargedMethod: String? = null,
        parts: List<PaymentPart> = emptyList(),
        payAtEnd: Boolean = false,
    ) = OrderSummary(
        id = "p1",
        folio = 12,
        operationalDate = "2026-10-29",
        state = OrderState.DELIVERED,
        paymentMethod = PaymentMethod.CASH,
        destination = OrderDestination.IN_SPACE,
        space = null,
        subtotal = "100.00",
        combinedSavings = "0.00",
        cashbackAwarded = "0.00",
        total = "100.00",
        version = 3,
        createdAt = "2026-10-29T18:00:00Z",
        updatedAt = "2026-10-29T18:10:00Z",
        payAtEnd = payAtEnd,
        chargedMethod = chargedMethod,
        paymentsByMethod = parts,
    )

    @Test
    fun `un cobro en partes dice Mixto y cada parte con su metodo`() {
        val mixto =
            summary(
                chargedMethod = "mixto",
                parts = listOf(PaymentPart("efectivo", "66.67"), PaymentPart("terminal", "33.33")),
            )

        assertEquals("Mixto · Efectivo $66.67 + Terminal $33.33", chargedPaymentLabel(mixto))
        assertEquals("Mixto", chargedPaymentLabel(summary(chargedMethod = "mixto")))
    }

    @Test
    fun `un pedido en efectivo cobrado con terminal dice Terminal`() {
        assertEquals("Terminal", chargedPaymentLabel(summary(chargedMethod = "terminal")))
        assertEquals("Terminal", orderPaymentLabel(summary(chargedMethod = "terminal")))
    }

    @Test
    fun `sin metodo cobrado se queda con el metodo del pedido`() {
        assertNull(chargedPaymentLabel(summary()))
        assertEquals("Efectivo", orderPaymentLabel(summary()))
        assertEquals("Cuenta pagada", orderPaymentLabel(summary(chargedMethod = "terminal", payAtEnd = true)))
    }

    @Test
    fun `el detalle lee metodo_pago_cobrado y pagos_por_metodo, y un servidor anterior no los manda`() {
        val json = Json { ignoreUnknownKeys = true }
        val base =
            """
            "id":"p1","folio":12,"fecha_operativa":"2026-10-29","estado":"entregado","metodo_pago":"efectivo",
            "destino":"en_espacio","subtotal":"100.00","ahorro_combinado":"0.00","cashback_otorgado":"0.00",
            "total":"100.00","version":3,"creado_en":"2026-10-29T18:00:00Z","actualizado_en":"2026-10-29T18:10:00Z"
            """.trimIndent()

        val nuevo =
            json
                .decodeFromString<OrderDetailDto>(
                    """{$base,"metodo_pago_cobrado":"mixto","pagos_por_metodo":[{"metodo":"terminal","monto":"33.33"}]}""",
                ).toDomain()
                .summary
        val anterior = json.decodeFromString<OrderDetailDto>("{$base}").toDomain().summary

        assertEquals("mixto", nuevo.chargedMethod)
        assertEquals(listOf(PaymentPart("terminal", "33.33")), nuevo.paymentsByMethod)
        assertNull(anterior.chargedMethod)
        assertEquals(emptyList<PaymentPart>(), anterior.paymentsByMethod)
    }
}
