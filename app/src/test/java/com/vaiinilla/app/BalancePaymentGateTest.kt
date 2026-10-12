package com.vaiinilla.app

import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.canPayWithBalance
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El saldo Vaiinilla se ofrece solo si el dueño no lo apagó (`permite_saldo`). */
class BalancePaymentGateTest {
    private val parser = ContractResponseParser()

    private fun status(allowsBalance: Boolean?): String {
        val field = allowsBalance?.let { ""","permite_saldo":$it""" }.orEmpty()
        return """
            {"data":{"recibiendo_pedidos":true,"sesion_caja_abierta":true,"caja_en_linea":true,
            "cocina_en_linea":true,"tiempo_estimado_min":5,"consultado_en":"2026-10-11T12:00:00.000Z"$field},
            "meta":{},"error":null}
            """.trimIndent()
    }

    @Test
    fun `the balance is offered only when the owner keeps it on`() {
        assertTrue(parser.parseOperationalStatus(status(true)).allowsBalance)
        assertFalse(parser.parseOperationalStatus(status(false)).allowsBalance)
    }

    @Test
    fun `an older server that does not send it keeps the balance available`() {
        assertTrue(parser.parseOperationalStatus(status(null)).allowsBalance)
    }

    @Test
    fun `the checkout hides balance only when the owner turned it off`() {
        assertFalse(OrderFlowUiState(operationalStatus = statusWith(allowsBalance = false)).canPayWithBalance)
        assertTrue(OrderFlowUiState(operationalStatus = statusWith(allowsBalance = true)).canPayWithBalance)
        assertTrue(OrderFlowUiState().canPayWithBalance)
    }

    private fun statusWith(allowsBalance: Boolean) =
        OperationalStatus(
            acceptingOrders = true,
            cashSessionOpen = true,
            cashierOnline = true,
            kitchenOnline = true,
            estimatedTimeMinutes = 5,
            consultedAt = "2026-10-11T12:00:00.000Z",
            allowsBalance = allowsBalance,
        )
}
