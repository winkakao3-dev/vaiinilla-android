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
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableCallTaker
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.operational.WaiterUiState
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

    fun waiterBoardState(): WaiterUiState {
        val now = java.time.Instant.now()

        fun iso(minusSeconds: Long) = now.minusSeconds(minusSeconds).toString()

        fun space(
            id: Int,
            name: String = "Mesa $id",
        ) = TableSpace(id = id, name = name, type = "mesa")

        fun order(
            id: String,
            folio: Int,
            state: OrderState,
        ) = BoardOrder(
            id = id,
            folio = folio,
            state = state,
            version = 1,
            clientName = "Ana",
            itemsSummary = "2× Taco de cochinita, 1× Agua de jamaica",
            updatedAt = iso(300),
        )

        fun call(
            id: String,
            spaceId: Int,
            status: CallStatus,
            reason: CallReason = CallReason.UTENSILIOS,
            takenBy: TableCallTaker? = null,
        ) = TableCall(
            id = id,
            space = space(spaceId),
            orderId = null,
            reason = reason,
            status = status,
            clientName = "Ana",
            takenBy = takenBy,
            createdAt = iso(222),
            takenAt = if (status == CallStatus.EN_CAMINO) iso(60) else null,
            closedAt = null,
            version = if (status == CallStatus.EN_CAMINO) 2 else 1,
        )
        return WaiterUiState(
            tables =
                listOf(
                    BoardTable(space = space(4), call = call("call-4", 4, CallStatus.PENDIENTE), orders = emptyList()),
                    BoardTable(
                        space = space(7),
                        call = call("call-7", 7, CallStatus.EN_CAMINO, takenBy = TableCallTaker("u-2", "Luis")),
                        orders = emptyList(),
                    ),
                    BoardTable(
                        space = space(12),
                        call = null,
                        orders = listOf(order("order-12", 312, OrderState.READY)),
                    ),
                    BoardTable(
                        space = space(3, "Barra 3"),
                        call = null,
                        orders =
                            listOf(
                                order("order-3", 308, OrderState.PREPARING),
                                order("order-3b", 309, OrderState.PAID),
                            ),
                    ),
                    BoardTable(space = space(9), call = null, orders = emptyList()),
                ),
            callsEnabled = true,
        )
    }
}
