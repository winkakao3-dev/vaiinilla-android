package com.vaiinilla.app.ui.order

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.contract.ContractResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Con «Pago en caja» apagado (`permite_pago_en_caja: false`) el carrito no lo ofrece. */
class CashAtRegisterAllowedTest {
    private val parser = ContractResponseParser()

    @Test
    fun `permite_pago_en_caja falso quita Pago en caja del carrito`() {
        val status = parser.parseOperationalStatus(status(""""permite_pago_en_caja": false,"""))
        assertFalse(status.allowsCashAtRegister)
        assertFalse(OrderFlowUiState(operationalStatus = status).canPayAtRegister)
    }

    @Test
    fun `un servidor que no lo manda y el estado sin cargar siguen ofreciendo Pago en caja`() {
        val status = parser.parseOperationalStatus(status(""))
        assertTrue(status.allowsCashAtRegister)
        assertTrue(OrderFlowUiState(operationalStatus = status).canPayAtRegister)
        assertTrue(OrderFlowUiState(operationalStatus = null).canPayAtRegister)
    }

    @Test
    fun `el 422 del servidor se dice en espanol`() {
        val error = ApiClientException("CASH_AT_REGISTER_NOT_ALLOWED", "CASH_AT_REGISTER_NOT_ALLOWED", 422)
        assertEquals(
            "Este negocio ya no recibe pagos en caja desde la app. Paga con tarjeta o saldo.",
            error.toUserFacingMessage(),
        )
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
