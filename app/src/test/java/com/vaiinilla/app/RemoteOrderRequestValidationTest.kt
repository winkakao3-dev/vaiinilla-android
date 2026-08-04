package com.vaiinilla.app

import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteOrderRequestValidationTest {
    private val item = CreateOrderItem(productId = 101, quantity = 1, optionIds = emptyList())

    @Test
    fun `remote accepts cash order for a real space id`() {
        val result =
            runCatching {
                ContractRules.validateRemoteOrderRequest(
                    CreateOrderRequest(
                        paymentMethod = PaymentMethod.CASH,
                        destination = OrderDestination.IN_SPACE,
                        spaceId = 987654,
                        kitchenNotes = "",
                        items = listOf(item),
                    ),
                )
            }

        assertTrue(result.isSuccess)
    }

    @Test
    fun `remote rejects a table order without a resolved space`() {
        val result =
            runCatching {
                ContractRules.validateRemoteOrderRequest(
                    CreateOrderRequest(
                        paymentMethod = PaymentMethod.CASH,
                        destination = OrderDestination.IN_SPACE,
                        spaceId = null,
                        kitchenNotes = "",
                        items = listOf(item),
                    ),
                )
            }

        assertTrue(result.isFailure)
    }

    @Test
    fun `remote rejects payment methods outside Entrega 01`() {
        val result =
            runCatching {
                ContractRules.validateRemoteOrderRequest(
                    CreateOrderRequest(
                        paymentMethod = PaymentMethod.CARD,
                        destination = OrderDestination.TAKE_AWAY,
                        spaceId = null,
                        kitchenNotes = "",
                        items = listOf(item),
                    ),
                )
            }

        assertTrue(result.isFailure)
    }
}
