package com.vaiinilla.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Precios y efectivo sin perder decimales (G10, B04, G02): "12.5" no se vuelve 125 y "200" se acepta. */
class MoneyInputTest {
    @Test
    fun `mientras se escribe se queda el punto y la coma cuenta como punto`() {
        assertEquals("12.5", MoneyInput.sanitize("12.5"))
        assertEquals("12.5", MoneyInput.sanitize("12,5"))
        assertEquals("200", MoneyInput.sanitize("200"))
        assertEquals("12.", MoneyInput.sanitize("12,"))
        assertEquals("12.50", MoneyInput.sanitize("12.505"))
        assertEquals("1.25", MoneyInput.sanitize("1.2.5"))
        assertEquals("1250.50", MoneyInput.sanitize("$1,250.50"))
        assertEquals("12345678", MoneyInput.sanitize("123456789"))
    }

    @Test
    fun `la coma con tres digitos es de miles y no un decimal`() {
        assertEquals("1250", MoneyInput.sanitize("1,250"))
        assertEquals("1250000", MoneyInput.sanitize("1,250,000"))
        assertEquals("1.25", MoneyInput.sanitize("1,25"))
        assertEquals("0.50", MoneyInput.sanitize("0,500"))
        assertEquals("1000.00", MoneyInput.toWire("$1,000"))
    }

    @Test
    fun `al backend va con dos decimales`() {
        assertEquals("200.00", MoneyInput.toWire("200"))
        assertEquals("12.50", MoneyInput.toWire("12.5"))
        assertEquals("12.50", MoneyInput.toWire("12,5"))
        assertEquals("200.00", MoneyInput.toWire("200.00"))
        assertEquals("12.00", MoneyInput.toWire("12."))
        assertEquals("0.50", MoneyInput.toWire(".5"))
        assertEquals("7.00", MoneyInput.toWire("007"))
        assertEquals("80.00", MoneyInput.toWire(" 80 "))
    }

    @Test
    fun `sin monto no hay nada que mandar`() {
        assertNull(MoneyInput.toWire(""))
        assertNull(MoneyInput.toWire("."))
        assertNull(MoneyInput.toWire("abc"))
    }

    @Test
    fun `lo que se manda cumple el contrato de dinero`() {
        listOf("200", "12,5", "0.5", "65", "1250,75").forEach { typed ->
            val wire = MoneyInput.toWire(typed)!!
            assertEquals(typed, true, ContractRules.isValidMoney(wire))
        }
    }
}
