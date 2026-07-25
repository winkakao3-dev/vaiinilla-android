package com.vaiinilla.app.ui.demo

import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.data.fixture.FixtureSource
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.DemoCheckoutFixtures
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.operational.OperationalViewModel
import com.vaiinilla.app.ui.order.OrderFlowViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoGallerySeeder @Inject constructor(
    private val fixtureSource: FixtureSource,
    private val parser: ContractFixtureParser,
    private val orderJson: OrderContractJson,
) {
    fun catalog(): Catalog = parser.parseCatalog(fixtureSource.read(CATALOG_PATH))

    fun operationalStatus(): OperationalStatus =
        parser.parseOperationalStatus(fixtureSource.read(OPERATIONAL_STATUS_PATH))

    fun sampleOrder(
        state: OrderState = OrderState.PENDING_PAYMENT,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        destination: OrderDestination = OrderDestination.TAKE_AWAY,
        spaceId: Int = DemoCheckoutFixtures.DEFAULT_SPACE.id,
    ): OrderDetail {
        val order = orderJson.parseOrderDetail(fixtureSource.read(CREATED_ORDER_PATH))
        val space = if (destination == OrderDestination.IN_SPACE) {
            DemoCheckoutFixtures.spaceForId(spaceId)?.let { demoSpace ->
                OrderSpace(
                    id = demoSpace.id,
                    name = demoSpace.name,
                    type = DemoCheckoutFixtures.SPACE_TYPE,
                )
            }
        } else {
            null
        }
        return order.copy(
            summary = order.summary.copy(
                state = state,
                paymentMethod = paymentMethod,
                destination = destination,
                space = space,
            ),
        )
    }

    fun seedCatalogCleared(orderFlowViewModel: OrderFlowViewModel) {
        orderFlowViewModel.applyGalleryCatalog(
            searchQuery = "",
            openFirstProduct = false,
        )
    }

    fun seedCatalogEmptySearch(orderFlowViewModel: OrderFlowViewModel) {
        orderFlowViewModel.applyGalleryCatalog(
            searchQuery = "zzzsinresultados",
            openFirstProduct = false,
        )
    }

    fun seedCatalogProductSheet(orderFlowViewModel: OrderFlowViewModel) {
        orderFlowViewModel.applyGalleryCatalog(
            searchQuery = "",
            openFirstProduct = true,
        )
    }

    fun seedCatalogActiveOrder(
        orderFlowViewModel: OrderFlowViewModel,
        operationalViewModel: OperationalViewModel,
    ) {
        val order = sampleOrder(
            state = OrderState.PREPARING,
            paymentMethod = PaymentMethod.CASH,
        )
        orderFlowViewModel.applyGalleryCatalog()
        operationalViewModel.applyGalleryClientOrders(listOf(order), order.summary.id)
    }

    fun seedCartEmpty(orderFlowViewModel: OrderFlowViewModel) {
        orderFlowViewModel.applyGalleryCatalog()
    }

    fun seedCartWithFirstProduct(orderFlowViewModel: OrderFlowViewModel) {
        orderFlowViewModel.seedCartWithFirstProduct()
    }

    fun seedCheckout(
        orderFlowViewModel: OrderFlowViewModel,
        destination: OrderDestination,
        payment: PaymentMethod,
        spaceId: Int = DemoCheckoutFixtures.DEFAULT_SPACE.id,
    ) {
        orderFlowViewModel.seedCheckout(destination, payment, spaceId)
    }

    fun seedConfirmation(
        orderFlowViewModel: OrderFlowViewModel,
        payment: PaymentMethod,
    ) {
        val order = sampleOrder(
            state = if (payment == PaymentMethod.CASH) {
                OrderState.PENDING_PAYMENT
            } else {
                OrderState.PAID
            },
            paymentMethod = payment,
        )
        orderFlowViewModel.seedCreatedOrder(order)
    }

    fun seedTrackingEmpty(operationalViewModel: OperationalViewModel) {
        operationalViewModel.applyGalleryClientOrders(emptyList(), selectedOrderId = null)
    }

    fun seedTrackingOrder(
        operationalViewModel: OperationalViewModel,
        state: OrderState,
        payment: PaymentMethod = PaymentMethod.CASH,
    ) {
        val order = sampleOrder(state = state, paymentMethod = payment)
        operationalViewModel.applyGalleryClientOrders(listOf(order), order.summary.id)
    }

    fun firstCartLine(catalog: Catalog): CartLine? {
        val firstProduct = catalog.products.firstOrNull() ?: return null
        val defaultOptionIds = firstProduct.optionGroups
            .firstOrNull()
            ?.options
            ?.firstOrNull()
            ?.id
            ?.let { setOf(it) }
            ?: emptySet()
        return CartLine(
            product = firstProduct,
            quantity = 1,
            selectedOptionIds = defaultOptionIds,
        )
    }

    private companion object {
        const val CATALOG_PATH = "fixtures/catalog.json"
        const val OPERATIONAL_STATUS_PATH = "fixtures/operational_status.json"
        const val CREATED_ORDER_PATH = "fixtures/created_order.json"
    }
}
