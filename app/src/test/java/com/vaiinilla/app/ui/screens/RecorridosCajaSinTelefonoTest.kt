package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.ui.operational.CashierMoneyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Recorridos de Caja (N05, N06, Q02) en la JVM: las cuentas del fondo, el conteo y el cuadre del cierre.
 * Prueba la aritmética y las reglas que usa la pantalla; no cuenta como recorrido en teléfono.
 */
class RecorridosCajaSinTelefonoTest {
    @Test
    fun n05_elFondoQueCuentaElCajeroSaleConDosDecimales() {
        assertEquals("500.00", CashierMoneyRules.openingWireAmount("500"))
        assertEquals("500.50", CashierMoneyRules.openingWireAmount("500,5"))
        assertNull(CashierMoneyRules.openingWireAmount("abc"))
        assertNull(CashierMoneyRules.openingWireAmount(""))
    }

    @Test
    fun n06_contarBilleteXBilleteSumaBilletesYMonedas() {
        val counts = mapOf(500 to 2, 100 to 3, 20 to 1)
        assertEquals("1320.00", billCountTotal(counts, ""))
        assertEquals("1332.50", billCountTotal(counts, "12.5"))
        assertEquals("0.00", billCountTotal(emptyMap(), ""))
        // Un conteo negativo no resta del cajón.
        assertEquals("1000.00", billCountTotal(mapOf(500 to 2, 100 to -3), ""))
        assertNull(billCountTotal(counts, "doce"))
    }

    @Test
    fun q02_elCorteCuadraContraElArqueo() {
        assertEquals(0, BigDecimal("0.00").compareTo(cashCountDifference("1332.50", "1332.50")))
        assertEquals(0, BigDecimal("32.50").compareTo(cashCountDifference("1332.50", "1300.00")))
        assertEquals(0, BigDecimal("-50.00").compareTo(cashCountDifference("1250.00", "1300.00")))
        assertNull(cashCountDifference(null, "1300.00"))
        assertNull(cashCountDifference("1300.00", null))
        assertEquals("1332.50", normalizeCountedCash("1332.5"))
        assertNull(normalizeCountedCash("abc"))
    }
}
