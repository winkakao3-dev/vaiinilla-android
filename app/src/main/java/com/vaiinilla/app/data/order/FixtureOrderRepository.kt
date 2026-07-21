package com.vaiinilla.app.data.order

import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.data.fixture.FixtureSource
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderItemOption
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import java.math.BigDecimal
import java.util.UUID

class FixtureOrderRepository(
    private val fixtureSource: FixtureSource,
    private val parser: ContractFixtureParser,
) : OrderRepository {
    private val resultsByKey = linkedMapOf<String, StoredRequest>()

    @Synchronized
    override fun createOrder(request: CreateOrderRequest, idempotencyKey: String): Result<OrderDetail> = runCatching {
        requireUuid(idempotencyKey)
        resultsByKey[idempotencyKey]?.let { stored ->
            if (stored.request != request) {
                throw OrderRepositoryException(
                    code = "IDEMPOTENCY_KEY_REUSED",
                    message = "La llave de idempotencia ya fue usada con otro request.",
                )
            }
            return@runCatching stored.order
        }

        ContractRules.validateCreateOrderRequest(request)
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
        val sequence = resultsByKey.size
        val order = OrderDetail(
            summary = OrderSummary(
                id = UUID.nameUUIDFromBytes("vaiinilla:$idempotencyKey".toByteArray()).toString(),
                folio = 3472 + sequence,
                operationalDate = consultedAt.substringBefore('T'),
                state = OrderState.PENDING_PAYMENT,
                paymentMethod = request.paymentMethod,
                destination = request.destination,
                space = null,
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
        )
        resultsByKey[idempotencyKey] = StoredRequest(request, order)
        order
    }

    private fun requireUuid(value: String) {
        try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            throw OrderRepositoryException("VALIDATION_ERROR", "Idempotency-Key debe ser UUID.")
        }
    }

    private data class StoredRequest(
        val request: CreateOrderRequest,
        val order: OrderDetail,
    )

    private companion object {
        const val CATALOG_PATH = "fixtures/catalog.json"
        const val OPERATIONAL_STATUS_PATH = "fixtures/operational_status.json"
    }
}
