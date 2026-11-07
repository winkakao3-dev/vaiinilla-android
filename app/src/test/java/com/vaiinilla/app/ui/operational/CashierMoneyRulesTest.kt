package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.data.wallet.PendingWalletReload
import com.vaiinilla.app.domain.repository.WalletRepositoryException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashierMoneyRulesTest {
    private fun valid(check: CashierMoneyRules.CollectionCheck): String =
        (check as CashierMoneyRules.CollectionCheck.Valid).wireAmount

    private fun invalid(check: CashierMoneyRules.CollectionCheck): String =
        (check as CashierMoneyRules.CollectionCheck.Invalid).message

    @Test
    fun `el cobro normaliza lo que escribe el cajero a dos decimales`() {
        assertEquals("200.00", valid(CashierMoneyRules.checkCollection("200", cashDue = null, tip = null)))
        assertEquals("200.50", valid(CashierMoneyRules.checkCollection("200,5", cashDue = null, tip = null)))
    }

    @Test
    fun `un monto que no se entiende no sale`() {
        assertEquals(
            "Escribe el efectivo recibido (ej. 100 o 100.50).",
            invalid(CashierMoneyRules.checkCollection("abc", cashDue = "50.00", tip = null)),
        )
        assertEquals(
            "Escribe el efectivo recibido (ej. 100 o 100.50).",
            invalid(CashierMoneyRules.checkCollection("", cashDue = null, tip = null)),
        )
    }

    @Test
    fun `el efectivo debe cubrir el total y la propina`() {
        assertEquals(
            "El efectivo recibido no cubre el total del pedido más la propina.",
            invalid(CashierMoneyRules.checkCollection("100", cashDue = "100.00", tip = "5.00")),
        )
        assertEquals("105.00", valid(CashierMoneyRules.checkCollection("105", cashDue = "100.00", tip = "5.00")))
        assertEquals("100.00", valid(CashierMoneyRules.checkCollection("100", cashDue = "100.00", tip = null)))
    }

    @Test
    fun `sin total conocido decide el servidor y el cobro sale`() {
        assertEquals("1.00", valid(CashierMoneyRules.checkCollection("1", cashDue = null, tip = "9.00")))
    }

    @Test
    fun `una propina que no se lee cuenta como cero`() {
        assertEquals("100.00", valid(CashierMoneyRules.checkCollection("100", cashDue = "100.00", tip = "x")))
    }

    @Test
    fun `la recarga pide un monto positivo con dos decimales`() {
        assertEquals(
            CashierMoneyRules.ReloadCheck.Valid("100.00"),
            CashierMoneyRules.checkReload(" 100.00 "),
        )
        listOf("0.00", "100", "-5.00", "", "1,50").forEach { raw ->
            val check = CashierMoneyRules.checkReload(raw)
            assertTrue(raw, check is CashierMoneyRules.ReloadCheck.Invalid)
        }
        assertEquals(
            "El monto debe ser positivo y tener dos decimales (ej. 100.00).",
            (CashierMoneyRules.checkReload("0.00") as CashierMoneyRules.ReloadCheck.Invalid).message,
        )
    }

    @Test
    fun `un movimiento del cajon pide monto mayor a cero y motivo de 3 a 160 letras`() {
        assertNull(CashierMoneyRules.drawerMovementError("20.00", "Cambio para el turno"))
        val expected = "Escribe un monto mayor a cero y un motivo de 3 a 160 letras."
        assertEquals(expected, CashierMoneyRules.drawerMovementError("0", "Cambio para el turno"))
        assertEquals(expected, CashierMoneyRules.drawerMovementError("abc", "Cambio para el turno"))
        assertEquals(expected, CashierMoneyRules.drawerMovementError("20.00", "ab"))
        assertEquals(expected, CashierMoneyRules.drawerMovementError("20.00", "x".repeat(161)))
    }

    @Test
    fun `la caja se abre con cero pero no con texto`() {
        assertEquals("0.00", CashierMoneyRules.openingWireAmount("0"))
        assertEquals("500.00", CashierMoneyRules.openingWireAmount("500"))
        assertNull(CashierMoneyRules.openingWireAmount("mucho"))
    }

    @Test
    fun `el aviso de recarga pendiente dice cuanto y a quien`() {
        val pending = PendingWalletReload("u1", "100.00", "k1", storedAtEpochMs = 1L, userName = "Ana")
        val message = CashierMoneyRules.pendingReloadBlockMessage(pending)
        assertTrue(message.startsWith("Hay una recarga anterior sin confirmar ("))
        assertTrue(message.contains(" a Ana"))
        assertTrue(message.endsWith("Confírmala o descártala antes de hacer otra."))
    }

    @Test
    fun `solo un 4xx definitivo descarta la recarga pendiente`() {
        fun failure(status: Int?) = WalletRepositoryException("x", "x", status)
        assertTrue(isDefinitiveWalletReloadFailure(failure(400)))
        assertTrue(isDefinitiveWalletReloadFailure(failure(409)))
        assertFalse(isDefinitiveWalletReloadFailure(failure(408)))
        assertFalse(isDefinitiveWalletReloadFailure(failure(429)))
        assertFalse(isDefinitiveWalletReloadFailure(failure(500)))
        assertFalse(isDefinitiveWalletReloadFailure(failure(null)))
        assertFalse(isDefinitiveWalletReloadFailure(IllegalStateException("red")))
    }

    @Test
    fun `una recarga de mas de media hora o sin hora ya no se repite sola`() {
        val now = 10_000_000L

        fun pending(at: Long) = PendingWalletReload("u", "10.00", "k", storedAtEpochMs = at)
        assertFalse(isStalePendingReload(pending(now - 60_000), now))
        assertFalse(isStalePendingReload(pending(now - 30 * 60_000L), now))
        assertTrue(isStalePendingReload(pending(now - 30 * 60_000L - 1), now))
        assertTrue(isStalePendingReload(pending(0L), now))
    }
}
