package com.vaiinilla.app.data.order

import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.data.fixture.FixtureSource
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.DemoCheckoutFixtures
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderItemOption
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import java.math.BigDecimal
import java.util.UUID

class FixtureOrderRepository(
    private val fixtureSource: FixtureSource,
    private val parser: ContractFixtureParser,
) : OrderRepository {
    private val ordersById = linkedMapOf<String, OrderDetail>()
    private val createRequestsByKey = linkedMapOf<String, StoredCreateRequest>()
    private val mutationResultsByKey = linkedMapOf<String, OrderDetail>()

    @Synchronized
    override fun createOrder(request: CreateOrderRequest, idempotencyKey: String): Result<OrderDetail> = runCatching {
        createOrderInternal(
            request = request,
            idempotencyKey = idempotencyKey,
            validate = ContractRules::validateCreateOrderRequest,
            initialState = OrderState.PENDING_PAYMENT,
            space = null,
        )
    }

    @Synchronized
    override fun createStudentCheckout(
        request: CreateOrderRequest,
        idempotencyKey: String,
    ): Result<OrderDetail> = runCatching {
        val initialState = if (request.paymentMethod.isInstantDemoPayment) {
            OrderState.PAID
        } else {
            OrderState.PENDING_PAYMENT
        }
        val space = if (request.destination == OrderDestination.IN_SPACE) {
            OrderSpace(
                id = DemoCheckoutFixtures.SPACE_ID,
                name = DemoCheckoutFixtures.SPACE_NAME,
                type = DemoCheckoutFixtures.SPACE_TYPE,
            )
        } else {
            null
        }
        createOrderInternal(
            request = request,
            idempotencyKey = idempotencyKey,
            validate = ContractRules::validateStudentCheckoutRequest,
            initialState = initialState,
            space = space,
        )
    }

    private fun createOrderInternal(
        request: CreateOrderRequest,
        idempotencyKey: String,
        validate: (CreateOrderRequest) -> Unit,
        initialState: OrderState,
        space: OrderSpace?,
    ): OrderDetail {
        requireUuid(idempotencyKey)
        createRequestsByKey[idempotencyKey]?.let { stored ->
            if (stored.request != request) {
                throw OrderRepositoryException(
                    code = "IDEMPOTENCY_KEY_REUSED",
                    message = "La llave de idempotencia ya fue usada con otro request.",
                )
            }
            return stored.order
        }

        validate(request)
        val status = parser.parseOperationalStatus(fixtureSource.read(OPERATIONAL_STATUS_PATH))
        ContractRules.validateOperationalStatus(status)
        if (!status.acceptingOrders || !status.cashSessionOpen) {
            throw OrderRepositoryException(
                code = "ESTABLISHMENT_NOT_RECEIVING",
                message = "El establecimiento no está recibiendo pedidos.",
            )
        }

        val catalog = parser.parseCatalog(fixtureSource.read(CATALOG_PATH))
        ContractRules.validateCatalog(catalog)
        val orderItems = request.items.mapIndexed { index, requestedItem ->
            val product = catalog.products.firstOrNull { it.id == requestedItem.productId }
                ?: throw OrderRepositoryException("PRODUCT_UNAVAILABLE", "El producto no está disponible.")
            if (!product.available) {
                throw OrderRepositoryException("PRODUCT_UNAVAILABLE", "El producto no está disponible.")
            }

            val selectedIds = requestedItem.optionIds.toSet()
            try {
                ContractRules.validateSelections(product, selectedIds)
            } catch (error: IllegalArgumentException) {
                throw OrderRepositoryException(
                    code = "INVALID_PRODUCT_OPTION",
                    message = error.message ?: "Las opciones no son válidas.",
                )
            }

            val selectedOptions = product.optionGroups
                .flatMap { it.options }
                .filter { it.id in selectedIds }
            val unitPrice = Money.productUnitPreview(product, selectedIds)
            val itemSubtotal = Money.format(Money.parse(unitPrice) * requestedItem.quantity.toBigDecimal())

            OrderItem(
                id = 501 + index,
                productId = product.id,
                productName = product.name,
                preparationStation = product.preparationStation,
                quantity = requestedItem.quantity,
                unitDigitalPrice = unitPrice,
                subtotal = itemSubtotal,
                options = selectedOptions.map { option ->
                    OrderItemOption(
                        optionId = option.id,
                        name = option.name,
                        extraPrice = option.extraPrice,
                    )
                },
            )
        }

        val total = Money.format(
            orderItems.fold(BigDecimal.ZERO) { accumulated, item ->
                accumulated + Money.parse(item.subtotal)
            },
        )
        val consultedAt = status.consultedAt
        val sequence = ordersById.size
        val order = OrderDetail(
            summary = OrderSummary(
                id = UUID.nameUUIDFromBytes("vaiinilla:$idempotencyKey".toByteArray()).toString(),
                folio = 3472 + sequence,
                operationalDate = consultedAt.substringBefore('T'),
                state = initialState,
                paymentMethod = request.paymentMethod,
                destination = request.destination,
                space = space,
                subtotal = total,
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = total,
                version = 1,
                createdAt = consultedAt,
                updatedAt = consultedAt,
            ),
            user = null,
            kitchenNotes = request.kitchenNotes,
            items = orderItems,
            pickupToken = "v1.fixture-${idempotencyKey.replace("-", "").take(32)}",
        )
        persist(order)
        createRequestsByKey[idempotencyKey] = StoredCreateRequest(request, order)
        return order
    }

    @Synchronized
    override fun getOrder(orderId: String): Result<OrderDetail> = runCatching {
        ordersById[orderId]
            ?: throw OrderRepositoryException("ORDER_NOT_FOUND", "El pedido no existe.")
    }

    @Synchronized
    override fun listOrders(role: OperationalRole, updatedSince: String?): Result<List<OrderDetail>> = runCatching {
        ordersById.values
            .filter { order -> matchesRole(role, order) }
            .filter { order ->
                updatedSince == null || order.summary.updatedAt > updatedSince
            }
            .sortedByDescending { it.summary.updatedAt }
    }

    @Synchronized
    override fun collectCash(
        orderId: String,
        amountReceived: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): Result<OrderDetail> = runMutation(idempotencyKey) {
        requireUuid(idempotencyKey)
        if (!ContractRules.isValidMoney(amountReceived)) {
            throw OrderRepositoryException("VALIDATION_ERROR", "El monto recibido no es válido.")
        }

        val order = requireOrder(orderId)
        if (order.summary.version != expectedVersion) {
            throw OrderRepositoryException("VERSION_CONFLICT", "El pedido cambió en otro dispositivo.")
        }
        if (order.summary.state != OrderState.PENDING_PAYMENT) {
            throw OrderRepositoryException("INVALID_ORDER_STATE", "El pedido no está por cobrar.")
        }
        if (Money.parse(amountReceived) < Money.parse(order.summary.total)) {
            throw OrderRepositoryException("INSUFFICIENT_CASH", "El monto recibido es menor al total.")
        }

        val timestamp = bumpTimestamp(order.summary.updatedAt)
        val paid = order.withState(OrderState.PAID, order.summary.version + 1, timestamp)
        val next = if (paid.requiresKitchen()) {
            paid
        } else {
            paid.withState(OrderState.READY, paid.summary.version + 1, timestamp)
        }
        persist(next)
        next
    }

    @Synchronized
    override fun transition(
        orderId: String,
        targetState: OrderState,
        expectedVersion: Int,
        idempotencyKey: String,
        pickupToken: String?,
    ): Result<OrderDetail> = runMutation(idempotencyKey) {
        requireUuid(idempotencyKey)
        val order = requireOrder(orderId)
        if (order.summary.version != expectedVersion) {
            throw OrderRepositoryException("VERSION_CONFLICT", "El pedido cambió en otro dispositivo.")
        }

        val current = order.summary.state
        val allowed = when (targetState) {
            OrderState.PREPARING -> current == OrderState.PAID && order.requiresKitchen()
            OrderState.READY -> current == OrderState.PREPARING
            OrderState.DELIVERED -> current == OrderState.READY
            else -> false
        }
        if (!allowed) {
            throw OrderRepositoryException("INVALID_TRANSITION", "La transición solicitada no es válida.")
        }
        if (targetState == OrderState.DELIVERED) {
            val token = pickupToken ?: order.pickupToken
            if (token.isNullOrBlank()) {
                throw OrderRepositoryException("INVALID_PICKUP_TOKEN", "Entregar exige qr_token.")
            }
        }

        val timestamp = bumpTimestamp(order.summary.updatedAt)
        val next = order.withState(targetState, order.summary.version + 1, timestamp)
        persist(next)
        next
    }

    private fun runMutation(
        idempotencyKey: String,
        block: () -> OrderDetail,
    ): Result<OrderDetail> = runCatching {
        mutationResultsByKey[idempotencyKey]?.let { return@runCatching it }
        val result = block()
        mutationResultsByKey[idempotencyKey] = result
        result
    }

    private fun persist(order: OrderDetail) {
        ordersById[order.summary.id] = order
    }

    private fun requireOrder(orderId: String): OrderDetail =
        ordersById[orderId]
            ?: throw OrderRepositoryException("ORDER_NOT_FOUND", "El pedido no existe.")

    private fun OrderDetail.withState(
        state: OrderState,
        version: Int,
        updatedAt: String,
    ): OrderDetail = copy(
        summary = summary.copy(
            state = state,
            version = version,
            updatedAt = updatedAt,
        ),
    )

    private fun OrderDetail.requiresKitchen(): Boolean =
        items.any { it.preparationStation == PreparationStation.KITCHEN }

    private fun matchesRole(role: OperationalRole, order: OrderDetail): Boolean {
        val state = order.summary.state
        return when (role) {
            OperationalRole.CLIENT -> true
            OperationalRole.CASHIER -> {
                state == OrderState.PENDING_PAYMENT ||
                    (state == OrderState.READY && order.summary.destination == OrderDestination.TAKE_AWAY)
            }
            OperationalRole.KITCHEN -> {
                order.requiresKitchen() &&
                    state in setOf(OrderState.PAID, OrderState.PREPARING, OrderState.READY)
            }
            OperationalRole.WAITER -> {
                state == OrderState.READY && order.summary.destination == OrderDestination.IN_SPACE
            }
        }
    }

    private fun bumpTimestamp(previous: String): String {
        val base = previous.substringBeforeLast('.').substringBefore('Z')
        val millis = base.substringAfterLast('.', "000").toIntOrNull() ?: 0
        val nextMillis = (millis + 1).coerceAtMost(999)
        return "${base.substringBeforeLast('.')}.$nextMillis" + "Z"
    }

    private fun requireUuid(value: String) {
        try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            throw OrderRepositoryException("VALIDATION_ERROR", "Idempotency-Key debe ser UUID.")
        }
    }

    private data class StoredCreateRequest(
        val request: CreateOrderRequest,
        val order: OrderDetail,
    )

    private companion object {
        const val CATALOG_PATH = "fixtures/catalog.json"
        const val OPERATIONAL_STATUS_PATH = "fixtures/operational_status.json"
    }
}
