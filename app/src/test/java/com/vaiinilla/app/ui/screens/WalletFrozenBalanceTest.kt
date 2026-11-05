package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletFrozenBalanceTest {
    @Test
    fun `saldo congelado lo dice y gana al aviso de recargas apagadas`() {
        val congelado = "Tu saldo está congelado: este negocio no acepta pagos con saldo por ahora."
        assertEquals(congelado, walletRechargeNote(hasData = true, canRecharge = true, frozen = true))
        assertEquals(congelado, walletRechargeNote(hasData = true, canRecharge = false, frozen = true))
        assertEquals(
            "Este lugar ya no recibe recargas de saldo.",
            walletRechargeNote(hasData = true, canRecharge = false, frozen = false),
        )
        assertNull(walletRechargeNote(hasData = true, canRecharge = true, frozen = false))
        assertNull(walletRechargeNote(hasData = false, canRecharge = true, frozen = true))
    }
}
