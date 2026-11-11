package com.vaiinilla.app

import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.domain.model.ContractRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractRulesTest {
    private val source = TestFixtureSource()
    private val parser = ContractResponseParser()

    @Test
    fun `canonical JSON catalog satisfies contract invariants`() {
        val catalog = parser.parseCatalog(source.read("fixtures/catalog.json"))
        ContractRules.validateCatalog(catalog)
        assertEquals(3, catalog.products.size)
        assertEquals("fixture://jamaica", catalog.products.first().imageUrl)
    }

    @Test
    fun `canonical operational status satisfies contract invariants`() {
        val status = parser.parseOperationalStatus(source.read("fixtures/operational_status.json"))
        ContractRules.validateOperationalStatus(status)
        assertTrue(status.acceptingOrders)
    }

    @Test
    fun `a store with nothing on sale sends no estimated time and its status still reads`() {
        val status =
            parser.parseOperationalStatus(
                """
                {"data":{"recibiendo_pedidos":false,"sesion_caja_abierta":true,"caja_en_linea":false,
                "cocina_en_linea":false,"tiempo_estimado_min":null,"tipo":"hotel",
                "consultado_en":"2026-11-11T22:56:54.638Z"},"meta":{},"error":null}
                """.trimIndent(),
            )

        ContractRules.validateOperationalStatus(status)
        assertEquals("hotel", status.businessType)
        assertEquals(0, status.estimatedTimeMinutes)
    }
    @Test
    fun `money uses decimal string with two positions`() {
        assertTrue(ContractRules.isValidMoney("26.00"))
        assertTrue(ContractRules.isValidMoney("0.00"))
        assertFalse(ContractRules.isValidMoney("26"))
        assertFalse(ContractRules.isValidMoney("26.0"))
        assertFalse(ContractRules.isValidMoney("\$26.00"))
    }
}
