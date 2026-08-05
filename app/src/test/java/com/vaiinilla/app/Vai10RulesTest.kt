package com.vaiinilla.app

import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Vai10RulesTest {
    private val catalog =
        ContractResponseParser().parseCatalog(
            TestFixtureSource().read("fixtures/catalog.json"),
        )
    private val burrito = catalog.products.first { it.id == 103 }

    @Test
    fun `required option groups must satisfy cardinality`() {
        assertTrue(runCatching { ContractRules.validateSelections(burrito, setOf(310, 314)) }.isSuccess)
        assertTrue(runCatching { ContractRules.validateSelections(burrito, setOf(310)) }.isFailure)
        assertTrue(runCatching { ContractRules.validateSelections(burrito, setOf(310, 311, 314)) }.isFailure)
        assertTrue(runCatching { ContractRules.validateSelections(burrito, setOf(310, 314, 999)) }.isFailure)
    }

    @Test
    fun `preview uses BigDecimal and preserves two positions`() {
        assertEquals("82.00", Money.productUnitPreview(burrito, setOf(310, 314, 317)))
    }

    @Test
    fun `line preview multiplies configured unit price by quantity`() {
        val line =
            CartLine(
                product = burrito,
                quantity = 2,
                selectedOptionIds = setOf(310, 314, 317),
            )
        assertEquals("164.00", Money.cartLinePreview(line))
    }

    @Test
    fun `request enforces line and quantity limits`() {
        val valid = request(quantity = 20)
        assertTrue(runCatching { ContractRules.validateCreateOrderRequest(valid) }.isSuccess)
        assertTrue(runCatching { ContractRules.validateCreateOrderRequest(request(quantity = 0)) }.isFailure)
        assertTrue(runCatching { ContractRules.validateCreateOrderRequest(request(quantity = 21)) }.isFailure)
        assertTrue(
            runCatching {
                ContractRules.validateCreateOrderRequest(valid.copy(items = emptyList()))
            }.isFailure,
        )
        assertTrue(
            runCatching {
                ContractRules.validateCreateOrderRequest(valid.copy(items = List(51) { valid.items.first() }))
            }.isFailure,
        )
    }

    @Test
    fun `take away requires null space and cash`() {
        val valid = request(quantity = 1)
        assertTrue(runCatching { ContractRules.validateCreateOrderRequest(valid) }.isSuccess)
        assertTrue(
            runCatching {
                ContractRules.validateCreateOrderRequest(valid.copy(spaceId = 701))
            }.isFailure,
        )
    }

    private fun request(quantity: Int): CreateOrderRequest =
        CreateOrderRequest(
            paymentMethod = PaymentMethod.CASH,
            destination = OrderDestination.TAKE_AWAY,
            spaceId = null,
            kitchenNotes = "",
            items =
                listOf(
                    CreateOrderItem(
                        productId = 103,
                        quantity = quantity,
                        optionIds = listOf(310, 314),
                    ),
                ),
        )
}
