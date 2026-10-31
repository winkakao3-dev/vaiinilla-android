package com.vaiinilla.app.core.notifications

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Busca el pedido de un aviso sin cambiar el servidor: el push del personal no dice de qué negocio es, así que se
 * prueba con los modos de la cuenta, primero los del rol al que va el aviso y luego Administración, que ve todos
 * los pedidos de su negocio. Cada intento pide un token de contexto aparte y no toca la sesión abierta.
 */
@Singleton
class NoticeOrderLookup
    @Inject
    constructor(
        private val accessRepository: AuthorizedAccessRepository,
        private val authRepository: StudentAuthRepository,
        private val apiClient: HttpVaiinillaApiClient,
        private val contractJson: OrderContractJson,
    ) {
        suspend fun lookUp(
            orderId: String,
            roles: List<OperationalRole>,
            establishmentId: String?,
            modes: List<AuthorizedMode>,
        ): NoticeLookup {
            val session =
                authRepository.peekSession()
                    ?: return unavailable("No hay sesión para buscar el pedido. Vuelve a entrar.")
            val candidates = candidates(roles, establishmentId, modes).take(MAX_ATTEMPTS)
            if (candidates.isEmpty()) return unavailable("Esta cuenta no tiene ningún modo con el que leer ese pedido.")
            var lastFailure: Throwable? = null
            for (mode in candidates) {
                try {
                    val context = accessRepository.activateMode(mode, session).getOrThrow()
                    val raw =
                        withContext(Dispatchers.IO) {
                            apiClient
                                .getWithBearer(bearer = context.accessToken, path = "pedidos/$orderId")
                                .getOrThrow()
                        }
                    val order = contractJson.parseOrderDetail(raw)
                    return NoticeLookup.Found(summary(order, mode), System.currentTimeMillis())
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Throwable) {
                    lastFailure = error
                }
            }
            return unavailable(message(lastFailure, candidates.size))
        }

        companion object {
            private const val MAX_ATTEMPTS = 5

            /** El mismo orden que iOS: el rol del aviso, luego Administración y, si va a una persona, Cliente. */
            fun candidates(
                roles: List<OperationalRole>,
                establishmentId: String?,
                modes: List<AuthorizedMode>,
            ): List<AuthorizedMode> {
                val scoped = establishmentId?.let { id -> modes.filter { it.establishmentId == id } } ?: modes
                val byRole = scoped.filter { it.role in roles }
                val admin = scoped.filter { it.role == OperationalRole.ADMIN && OperationalRole.ADMIN !in roles }
                val customer = if (roles.isEmpty()) scoped.filter { it.role == OperationalRole.CLIENT } else emptyList()
                return (byRole + admin + customer).distinctBy { it.membershipId }
            }

            fun summary(
                order: OrderDetail,
                mode: AuthorizedMode,
            ): NoticeOrderSummary =
                NoticeOrderSummary(
                    establishmentName = mode.establishmentName,
                    readAs = "${mode.role.label} en ${mode.establishmentName}",
                    folio = order.summary.folio,
                    status = order.summary.state.label,
                    customerName = order.user?.name ?: order.customerName,
                    total = "$" + order.summary.total.removeSuffix(".00"),
                    paymentMethod = order.summary.paymentMethod.label,
                    destination = order.summary.destination.label,
                    space = order.summary.space?.name,
                    items =
                        order.items.map { item ->
                            val removed = if (item.rejection == null) "" else " (quitado)"
                            "${item.quantity} × ${item.productName}$removed"
                        },
                    createdAt = order.summary.createdAt,
                )

            private fun unavailable(reason: String) = NoticeLookup.Unavailable(reason, System.currentTimeMillis())

            private fun message(
                error: Throwable?,
                tried: Int,
            ): String {
                if (error is IOException) return "No hay conexión para buscar el pedido."
                if (error is ApiClientException && error.httpStatus == 429) {
                    return "El servidor pidió esperar. Vuelve a intentarlo en un momento."
                }
                val modes = if (tried == 1) "el modo que tiene" else "los $tried modos que tiene"
                return "Esta cuenta no pudo leer el pedido con $modes. Es de un negocio donde ya no tiene ese modo, " +
                    "o ya no existe."
            }
        }
    }
