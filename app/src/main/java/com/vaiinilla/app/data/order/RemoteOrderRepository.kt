package com.vaiinilla.app.data.order

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.domain.model.CashCollectionResult
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.repository.OrderRepositoryException

class RemoteOrderRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: OrderContractJson,
    private val pickupTokenStore: PickupTokenStore,
) : OrderRepository {
    override fun createOrder(
        request: CreateOrderRequest,
        idempotencyKey: String,
    ): Result<CreatedOrder> =
        apiClient
            .post(
                path = "pedidos",
                body = contractJson.encodeCreateRequest(request),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { contractJson.parseCreatedOrder(it) }
            .mapCatching { created ->
                created.copy(order = pickupTokenStore.attach(created.order))
            }.mapApiErrors()

    override fun getOrder(orderId: String): Result<OrderDetail> =
        apiClient
            .get("pedidos/$orderId")
            .mapCatching { contractJson.parseOrderDetail(it) }
            .mapCatching { pickupTokenStore.attach(it) }
            .mapApiErrors()

    override fun listOrders(
        role: OperationalRole,
        updatedSince: String?,
    ): Result<List<OrderDetail>> {
        val query =
            buildMap {
                updatedSince?.let { put("actualizado_desde", it) }
            }
        return fetchAllOrderPages(query).mapApiErrors()
    }

    /**
     * El servidor entrega los pedidos más viejos primero y en páginas: quedarse con la primera ocultaba los
     * recientes a quien tuviera más de 50. La primera petición no lleva más parámetros para que siga siendo la
     * que guarda la caché de [cachedClientOrders]; las siguientes siguen el cursor (PEDIDO-01).
     */
    private fun fetchAllOrderPages(query: Map<String, String>): Result<List<OrderDetail>> =
        runCatching {
            val orders = mutableListOf<OrderDetail>()
            var cursor: String? = null
            repeat(MAX_ORDER_PAGES) {
                val pageQuery = cursor?.let { query + ("cursor" to it) } ?: query
                val page =
                    apiClient
                        .get("pedidos", pageQuery)
                        .mapCatching(contractJson::parseOrderListPage)
                        .getOrThrow()
                orders += page.orders.map(pickupTokenStore::attach)
                cursor = page.nextCursor
                if (cursor == null) return@runCatching orders.toList()
            }
            orders.toList()
        }

    override fun cachedClientOrders(): List<OrderDetail>? =
        apiClient
            .readCachedGet("pedidos")
            ?.let { raw -> runCatching { parseOrderList(raw) }.getOrNull() }

    private fun parseOrderList(raw: String): List<OrderDetail> =
        contractJson.parseOrderList(raw).map(pickupTokenStore::attach)

    override fun recoverPickupToken(order: OrderDetail): Result<OrderDetail> =
        apiClient
            .get("pedidos/${order.summary.id}/qr")
            .mapCatching(contractJson::parsePickupToken)
            .mapCatching { token -> pickupTokenStore.attach(order.copy(pickupToken = token)) }
            .mapApiErrors()

    override fun retryStripePayment(
        orderId: String,
        idempotencyKey: String,
    ): Result<StripePaymentSession> =
        apiClient
            .postWithoutBody(
                path = "pedidos/$orderId/pago/stripe",
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(contractJson::parseStripeRetry)
            .mapApiErrors()

    override fun collectCash(
        orderId: String,
        amountReceived: String,
        expectedVersion: Int,
        idempotencyKey: String,
        tip: String?,
    ): Result<CashCollectionResult> {
        val cachedToken = pickupTokenStore.read(orderId)
        return apiClient
            .post(
                path = "pedidos/$orderId/cobros-efectivo",
                body =
                    contractJson.encodeCashCollection(
                        amountReceived = amountReceived,
                        expectedVersion = expectedVersion,
                        tip = tip,
                    ),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { contractJson.parseCashCollection(it) }
            .mapCatching { result ->
                result.copy(
                    order =
                        pickupTokenStore.attach(
                            result.order.copy(
                                pickupToken = result.order.pickupToken ?: cachedToken,
                            ),
                        ),
                )
            }.mapApiErrors()
    }

    override fun transition(
        orderId: String,
        targetState: OrderState,
        expectedVersion: Int,
        idempotencyKey: String,
        pickupToken: String?,
    ): Result<OrderDetail> {
        val token = pickupToken ?: pickupTokenStore.read(orderId)
        return apiClient
            .post(
                path = "pedidos/$orderId/transiciones",
                body = contractJson.encodeTransition(targetState, expectedVersion, token),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { contractJson.parseOrderDetail(it) }
            .mapCatching { pickupTokenStore.attach(it.copy(pickupToken = it.pickupToken ?: token)) }
            .mapApiErrors()
    }

    override fun announceArrival(orderId: String): Result<OrderDetail> =
        apiClient
            .postWithoutBody(path = "pedidos/$orderId/llegada")
            .mapCatching { contractJson.parseOrderDetail(it) }
            .mapCatching { pickupTokenStore.attach(it) }
            .mapApiErrors()

    override fun cancelOrder(
        orderId: String,
        expectedVersion: Int,
        reason: String,
        idempotencyKey: String,
    ): Result<OrderDetail> =
        apiClient
            .post(
                path = "pedidos/$orderId/cancelaciones",
                body = contractJson.encodeCancelOrder(expectedVersion, reason),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { contractJson.parseCancelledOrder(it) }
            .mapCatching { pickupTokenStore.attach(it) }
            .mapApiErrors()

    override fun rejectItem(
        orderId: String,
        itemId: Int,
        expectedVersion: Int,
        reason: String,
        idempotencyKey: String,
    ): Result<OrderDetail> =
        apiClient
            .post(
                path = "pedidos/$orderId/articulos/$itemId/rechazos",
                // Mismo cuerpo que cancelar (motivo + versión) y la respuesta también trae `data.pedido`.
                body = contractJson.encodeCancelOrder(expectedVersion, reason),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { contractJson.parseCancelledOrder(it) }
            .mapCatching { pickupTokenStore.attach(it) }
            .mapApiErrors()

    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Result.failure(
                    when (error) {
                        is ApiClientException ->
                            OrderRepositoryException(error.code, error.message ?: error.code, error)
                        is OrderRepositoryException -> error
                        else -> error
                    },
                )
            },
        )
}

/** Tope de páginas por consulta (50 pedidos cada una): evita un ciclo largo si el cursor no avanza. */
private const val MAX_ORDER_PAGES = 20
