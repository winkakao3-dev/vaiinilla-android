package com.vaiinilla.app.domain.offline

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.counter.OfflineCounterRules
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import java.time.Duration
import java.time.Instant

/**
 * Un pedido que Mesero o Caja tomó sin internet. Se guarda en el teléfono antes de cualquier reintento y se manda
 * solo cuando vuelve la conexión, con el `id` que el teléfono le puso (el servidor lo respeta, migración 161) y la
 * misma [idempotencyKey] en cada intento: si el primero sí había llegado, no se registra dos veces.
 */
data class OfflineStaffOrder(
    /** Id del pedido, generado aquí. Las operaciones que se guarden después sobre él lo usan. */
    val id: String,
    val idempotencyKey: String,
    /** Folio provisional que ve el personal mientras no hay red, p. ej. «M7-014». */
    val localFolio: String,
    /** Cuerpo de `POST /pedidos` ya con `sin_conexion`; se manda tal cual. */
    val body: String,
    /** Para la lista: mesa o «Mostrador», y qué lleva. */
    val placeLabel: String,
    val summary: String,
    val capturedAt: Instant,
    /** Por qué el servidor no lo aceptó. Con él ya no se reintenta: lo resuelve el personal. */
    val problem: String? = null,
)

/** Lo que el teléfono guarda de un miembro del personal: sus pedidos por mandar y el consecutivo del día. */
data class OfflineStaffOrdersSnapshot(
    val day: String,
    /** Consecutivo del folio local en [day]; empieza en 1 cada día. */
    val nextSequence: Int = 1,
    val orders: List<OfflineStaffOrder> = emptyList(),
) {
    /** Otro día el consecutivo vuelve a 1; los pedidos se quedan hasta mandarse. */
    fun startingDay(today: String): OfflineStaffOrdersSnapshot =
        if (day == today) this else copy(day = today, nextSequence = 1)

    val pending: List<OfflineStaffOrder> get() = orders.filter { it.problem == null }
    val problems: List<OfflineStaffOrder> get() = orders.filter { it.problem != null }
}

object OfflineStaffOrderRules {
    /** El servidor deja de aceptarlos a las 12 h (`OFFLINE_ORDER_TOO_OLD`); aquí se avisa antes de intentarlo. */
    val MAXIMUM_AGE: Duration = Duration.ofHours(12)

    /** Mientras haya pendientes, aunque la red parezca bien (el servidor puede estar caído). */
    const val RETRY_INTERVAL_MS = 30_000L

    const val TOO_OLD_PROBLEM = "Pasaron más de 12 horas sin conexión. Regístralo a mano."

    /** Falta de red o servidor caído: el pedido se guarda y se reenvía con la misma llave. */
    fun isConnectionProblem(error: Throwable): Boolean = OfflineCounterRules.isConnectionProblem(error)

    /**
     * El servidor ya tenía un pedido con esa llave y otro cuerpo: es el intento con red que sí había llegado.
     * («Sigue en proceso» también: ese intento está terminando de registrarse.)
     */
    fun alreadyRegistered(error: Throwable): Boolean =
        generateSequence(error) { it.cause }.take(8).any { cause ->
            (cause is OrderRepositoryException && cause.code == IDEMPOTENCY_KEY_REUSED) ||
                (cause is ApiClientException && cause.code == IDEMPOTENCY_KEY_REUSED)
        }

    private const val IDEMPOTENCY_KEY_REUSED = "IDEMPOTENCY_KEY_REUSED"

    fun isTooOld(
        order: OfflineStaffOrder,
        now: Instant,
    ): Boolean = Duration.between(order.capturedAt, now) > MAXIMUM_AGE

    /**
     * «M7-014»: inicial del rol, marca de dos dígitos de este teléfono y consecutivo del día. Dos meseros no
     * comparten marca casi nunca, y si pasa el folio real del servidor los distingue al llegar.
     */
    fun localFolio(
        roleInitial: Char,
        deviceTag: Int,
        sequence: Int,
    ): String = "${roleInitial.uppercaseChar()}${deviceTag.mod(100)}-${sequence.toString().padStart(3, '0')}"
}

/** De quién es la cola: una membresía del personal en un negocio. */
data class OfflineStaffScope(
    val membershipId: String,
)

/** Donde vive la cola en el teléfono; se guarda (con commit) antes de cualquier envío. */
interface OfflineStaffOrderStore {
    fun load(
        scope: OfflineStaffScope,
        today: String,
    ): OfflineStaffOrdersSnapshot

    fun save(
        scope: OfflineStaffScope,
        snapshot: OfflineStaffOrdersSnapshot,
    )

    /** Marca de dos dígitos de este teléfono para el folio local; la misma mientras no se reinstale la app. */
    fun deviceTag(): Int
}
