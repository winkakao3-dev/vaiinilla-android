package com.vaiinilla.app.ui.order

import com.vaiinilla.app.data.contract.ContractResponseParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** J04: con los pagos con saldo apagados (`permite_saldo: false`) el carrito no ofrece Saldo. */
class BalancePaymentAllowedTest {
    private val parser = ContractResponseParser()

    @Test
    fun `permite_saldo falso apaga el saldo en el carrito`() {
        val status = parser.parseOperationalStatus(status(""""permite_saldo": false,"""))
        assertFalse(status.allowsBalance)
        assertFalse(OrderFlowUiState(operationalStatus = status).canPayWithBalance)
    }

    @Test
    fun `un servidor que no lo manda y el estado sin cargar siguen ofreciendo saldo`() {
        val status = parser.parseOperationalStatus(status(""))
        assertTrue(status.allowsBalance)
        assertTrue(OrderFlowUiState(operationalStatus = status).canPayWithBalance)
        assertTrue(OrderFlowUiState(operationalStatus = null).canPayWithBalance)
    }

    private fun status(extra: String) =
        """
        {"data": {
          "recibiendo_pedidos": true, "sesion_caja_abierta": true, "caja_en_linea": true,
          "cocina_en_linea": true, "tiempo_estimado_min": 12, $extra
          "consultado_en": "2026-07-20T15:00:00.000Z"
        }, "meta": {}, "error": null}
        """.trimIndent()
}
