package com.vaiinilla.app.ui.screenshot

import com.vaiinilla.app.TestFixtureSource
import com.vaiinilla.app.data.catalog.FixtureCatalogRepository
import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.order.OrderFlowUiState

object ScreenshotFixtures {
    private val repository = FixtureCatalogRepository(TestFixtureSource(), ContractFixtureParser())

    fun catalog(): Catalog = repository.getCatalog().getOrThrow()

    fun operationalStatus(): OperationalStatus = repository.getOperationalStatus().getOrThrow()

    fun catalogLoadedState(): OrderFlowUiState = OrderFlowUiState(
        loading = false,
        catalog = catalog(),
        operationalStatus = operationalStatus(),
        testOnlyMode = true,
    )

    fun cartState(paymentMethod: PaymentMethod = PaymentMethod.CASH): OrderFlowUiState {
        val loadedCatalog = catalog()
        val firstProduct = loadedCatalog.products.first()
        val defaultOptionIds = firstProduct.optionGroups
            .firstOrNull()
            ?.options
            ?.firstOrNull()
            ?.id
            ?.let { setOf(it) }
            ?: emptySet()
        return catalogLoadedState().copy(
            cartLines = listOf(
                CartLine(
                    product = firstProduct,
                    quantity = 1,
                    selectedOptionIds = defaultOptionIds,
                ),
            ),
            checkoutPayment = paymentMethod,
        )
    }
}
