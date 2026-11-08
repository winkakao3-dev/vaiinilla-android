package com.vaiinilla.app.data.outbox

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.core.network.idempotencyHeaders
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.offline.OfflineStaffOrderStore
import com.vaiinilla.app.domain.outbox.DrainOutcome
import com.vaiinilla.app.domain.outbox.OutboxContext
import com.vaiinilla.app.domain.outbox.OutboxSource
import com.vaiinilla.app.ui.operational.OfflineStaffOrderQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Los pedidos y el efectivo cobrado que Mesero o Caja guardaron sin internet. Se mandan con el token de la
 * membresía que los tomó aunque ese modo ya no esté abierto o la app esté cerrada, con la misma llave de
 * idempotencia de cada pedido.
 */
@Singleton
class StaffOrderOutboxSource
    @Inject
    constructor(
        private val queue: OfflineStaffOrderQueue,
        private val store: OfflineStaffOrderStore,
        private val apiClient: HttpVaiinillaApiClient,
        private val contractJson: OrderContractJson,
    ) : OutboxSource {
        override val name: String = "pedidos del personal"

        override fun hasWork(): Boolean = store.scopesWithOrders().isNotEmpty()

        override suspend fun drain(context: OutboxContext): DrainOutcome {
            var outcome = DrainOutcome.DONE
            for (saved in store.scopesWithOrders()) {
                val mode = context.modeOfMembership(saved.membershipId)?.takeIf { it.role in STAFF_ROLES } ?: continue
                val token = context.tokenFor(mode)
                if (token == null) {
                    outcome = outcome and DrainOutcome.RETRY_LATER
                    continue
                }
                val result =
                    queue.rescue(saved) { body, key ->
                        apiClient
                            .postWithBearer(
                                bearer = token,
                                path = "pedidos",
                                body = body,
                                headers = idempotencyHeaders(key),
                            ).map { raw ->
                                // Con 2xx ya llegó; se lee para saber si el efectivo cobrado quedó en la caja.
                                runCatching { contractJson.parseCreatedOrder(raw) }.getOrNull()
                            }
                    }
                outcome = outcome and result
            }
            return outcome
        }

        private companion object {
            val STAFF_ROLES = setOf(OperationalRole.WAITER, OperationalRole.CASHIER, OperationalRole.ADMIN)
        }
    }
