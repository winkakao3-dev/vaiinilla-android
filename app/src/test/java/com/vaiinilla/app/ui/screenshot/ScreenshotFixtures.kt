package com.vaiinilla.app.ui.screenshot

import com.vaiinilla.app.TestFixtureSource
import com.vaiinilla.app.data.catalog.FixtureCatalogRepository
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.wallet.WalletUiState

object ScreenshotFixtures {
    private val repository = FixtureCatalogRepository(TestFixtureSource(), ContractResponseParser())
    private val orderJson = OrderContractJson()
    private val fixtureSource = TestFixtureSource()

    fun catalog(): Catalog = repository.getCatalog().getOrThrow()

    fun operationalStatus(): OperationalStatus = repository.getOperationalStatus().getOrThrow()

    fun catalogLoadedState(): OrderFlowUiState =
        OrderFlowUiState(
            loading = false,
            catalog = catalog(),
            operationalStatus = operationalStatus(),
        )

    fun emptySearchState(): OrderFlowUiState =
        catalogLoadedState().copy(
            searchQuery = "zzzsinresultados",
        )

    fun emptyCartState(): OrderFlowUiState = catalogLoadedState()

    fun cartState(
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        destination: OrderDestination = OrderDestination.TAKE_AWAY,
        spaceId: Int = 12,
    ): OrderFlowUiState {
        val loadedCatalog = catalog()
        val firstProduct = loadedCatalog.products.first()
        val defaultOptionIds =
            firstProduct.optionGroups
                .firstOrNull()
                ?.options
                ?.firstOrNull()
                ?.id
                ?.let { setOf(it) }
                ?: emptySet()
        return catalogLoadedState().copy(
            cartLines =
                listOf(
                    CartLine(
                        product = firstProduct,
                        quantity = 1,
                        selectedOptionIds = defaultOptionIds,
                    ),
                ),
            checkoutPayment = paymentMethod,
            checkoutDestination = destination,
            selectedSpaceId = spaceId,
        )
    }

    fun sampleOrder(
        state: OrderState = OrderState.PENDING_PAYMENT,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        destination: OrderDestination = OrderDestination.TAKE_AWAY,
        spaceId: Int = 12,
    ): OrderDetail {
        val order = orderJson.parseOrderDetail(fixtureSource.read("fixtures/created_order.json"))
        val space =
            if (destination == OrderDestination.IN_SPACE) {
                OrderSpace(
                    id = spaceId,
                    name = "Mesa $spaceId",
                    type = "mesa",
                )
            } else {
                null
            }
        return order.copy(
            summary =
                order.summary.copy(
                    state = state,
                    paymentMethod = paymentMethod,
                    destination = destination,
                    space = space,
                ),
        )
    }

    fun trackingState(
        order: OrderDetail,
        selected: Boolean = true,
    ): OperationalUiState =
        OperationalUiState(
            role = OperationalRole.CLIENT,
            orders = listOf(order),
            selectedOrderId = if (selected) order.summary.id else null,
        )

    fun emptyTrackingState(): OperationalUiState =
        OperationalUiState(
            role = OperationalRole.CLIENT,
            orders = emptyList(),
        )

    fun walletState(): WalletUiState = WalletUiState()
}
