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
    fun `money uses decimal string with two positions`() {
        assertTrue(ContractRules.isValidMoney("26.00"))
        assertTrue(ContractRules.isValidMoney("0.00"))
        assertFalse(ContractRules.isValidMoney("26"))
        assertFalse(ContractRules.isValidMoney("26.0"))
        assertFalse(ContractRules.isValidMoney("\$26.00"))
    }
}
