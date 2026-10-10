package com.vaiinilla.app.data.guest

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pedidos de un invitado (compra sin cuenta, docs/compra-sin-cuenta.md del backend). Crear y pagar
 * usan el bearer de invitado; listar usa la llave en el body. Saldo y cashback no aplican a
 * invitados: el servidor lo decide, aquí no se calculan totales ni pagos.
 */
@Singleton
class GuestOrderGateway
    @Inject
    constructor(
        private val apiClient: VaiinillaApiClient,
        private val guestSession: GuestReservationSession,
        private val store: GuestSessionStore,
        private val orderContractJson: OrderContractJson,
        private val pickupTokenStore: PickupTokenStore,
        private val parser: ContractResponseParser,
    ) {
        fun operationalStatus(establishmentSlug: String): Result<OperationalStatus> =
            guestSession
                .withBearer(establishmentSlug) { bearer -> apiClient.getWithBearer(bearer, "estado-operativo") }
                .mapCatching(parser::parseOperationalStatus)
                .mapApiErrors()

        fun create(
            establishmentSlug: String,
            request: CreateOrderRequest,
            idempotencyKey: String,
        ): Result<CreatedOrder> =
            runCatching {
                ContractRules.validateRemoteOrderRequest(request)
                guestSession
                    .withBearer(establishmentSlug) { bearer ->
                        apiClient.postWithBearer(
                            bearer = bearer,
                            path = "pedidos",
                            body = orderContractJson.encodeCreateRequest(request),
                            headers = mapOf("Idempotency-Key" to idempotencyKey),
                        )
                    }.getOrThrow()
            }.mapCatching(orderContractJson::parseCreatedOrder)
                .mapCatching { created -> created.copy(order = pickupTokenStore.attach(created.order)) }
                .mapApiErrors()

        fun hasIdentity(): Boolean = guestSession.hasIdentity()

        /** Pedidos del invitado en [establishmentSlug], con seguimiento en vivo. Sin llave no hay lista. */
        fun list(establishmentSlug: String): Result<List<OrderDetail>> {
            val key = store.readGuestKey() ?: return Result.failure(GuestIdentityRequiredException())
            val body =
                buildJsonObject {
                    put("establecimiento_slug", establishmentSlug)
                    put("llave", key)
                }.toString()
            return apiClient
                .postPublic("publico/invitados/pedidos", body)
                .mapCatching { raw -> orderContractJson.parseOrderList(raw).map(pickupTokenStore::attach) }
                .mapApiErrors()
        }

        fun get(
            establishmentSlug: String,
            orderId: String,
        ): Result<OrderDetail> =
            list(establishmentSlug).mapCatching { orders ->
                orders.firstOrNull { it.summary.id == orderId }
                    ?: throw OrderRepositoryException(NOT_FOUND, "No encontramos ese pedido.")
            }

        fun retryStripePayment(
            establishmentSlug: String,
            orderId: String,
            idempotencyKey: String,
        ): Result<StripePaymentSession> =
            guestSession
                .withBearer(establishmentSlug) { bearer ->
                    apiClient.postWithBearer(
                        bearer = bearer,
                        path = "pedidos/$orderId/pago/stripe",
                        headers = mapOf("Idempotency-Key" to idempotencyKey),
                    )
                }.mapCatching(orderContractJson::parseStripeRetry)
                .mapApiErrors()

        /**
         * Reclamo "estos pedidos eran míos" tras crear cuenta o entrar. Solo un éxito olvida la llave:
         * un fallo de red o del servidor la conserva para reintentar en el siguiente acceso.
         */
        fun claim(firebaseIdToken: String): Result<Unit> {
            val key = store.readGuestKey() ?: return Result.success(Unit)
            val body = buildJsonObject { put("llave", key) }.toString()
            return apiClient
                .postWithBearer(
                    bearer = firebaseIdToken,
                    path = "invitados/reclamar",
                    body = body,
                    headers = mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
                ).map { guestSession.forgetIdentity() }
        }

        private fun <T> Result<T>.mapApiErrors(): Result<T> =
            fold(
                onSuccess = { Result.success(it) },
                onFailure = { error ->
                    Result.failure(
                        if (error is ApiClientException) {
                            OrderRepositoryException(error.code, error.message ?: error.code, error)
                        } else {
                            error
                        },
                    )
                },
            )

        private companion object {
            const val NOT_FOUND = "NOT_FOUND"
        }
    }
