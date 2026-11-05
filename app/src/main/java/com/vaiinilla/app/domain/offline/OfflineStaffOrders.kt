package com.vaiinilla.app.domain.offline

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.MissingAccessTokenException
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
    /** Por qué el servidor no lo aceptó. Ya no se reintenta solo: el personal toca Reintentar o lo resuelve. */
    val problem: String? = null,
    /** Caja ya recibió el efectivo (`cobrar_ahora`): hasta registrarse, ese dinero no está en ningún corte. */
    val collectedCash: Boolean = false,
    /**
     * El pedido sí quedó con este folio, pero el cobro en efectivo no se registró («cobrado sin registrar»).
     * Reenviarlo no lo arregla: Caja lo cobra en la lista y lo marca como resuelto.
     */
    val registeredFolio: Int? = null,
) {
    /** Cobrado sin internet, el pedido llegó pero el cobro no se aplicó. */
    val collectedButNotRegistered: Boolean get() = collectedCash && registeredFolio != null
}

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

/** Lo que se le dice a Caja cuando el pedido llegó pero el efectivo que ya cobró no se registró. */
fun cashNotRegisteredProblem(
    folio: Int,
    serverMessage: String?,
): String {
    val reason = serverMessage?.trim()?.takeIf { it.isNotEmpty() }?.let { " ($it)" }.orEmpty()
    return "Cobrado sin registrar: quedó como #$folio pero el efectivo no entró a la caja$reason. " +
        "Cóbralo en la lista de Caja para que entre al corte."
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
     * Al tomar el pedido: sin red o servidor caído sí se guarda; un 429 no (el servidor está arriba y solo pide
     * esperar), así un pedido con internet no se salta las reglas del menú por ir marcado «sin conexión».
     */
    fun shouldKeepOffline(error: Throwable): Boolean =
        isConnectionProblem(error) && apiError(error)?.httpStatus != HTTP_TOO_MANY_REQUESTS

    /**
     * Al mandar lo guardado: solo un rechazo con su motivo (4xx: producto, mesa, validación) lo aparta para que lo
     * vea el personal. Todo lo demás (red, cualquier 5xx aunque traiga sobre, sesión vencida, 429, una respuesta
     * que no se entendió) se vuelve a intentar con la misma llave; antes un 500 lo dejaba apartado para siempre.
     */
    fun shouldRetryLater(error: Throwable): Boolean {
        if (isConnectionProblem(error)) return true
        val causes = generateSequence(error) { it.cause }.take(8).toList()
        if (causes.any { it is MissingAccessTokenException }) return true
        val api = apiError(error)
        if (api == null) {
            // Un rechazo armado por el repositorio trae su código; cualquier otra cosa se reintenta.
            val refused = causes.filterIsInstance<OrderRepositoryException>().firstOrNull() ?: return true
            return refused.code in RETRYABLE_CODES
        }
        return api.httpStatus !in 400..499 || api.httpStatus in RETRYABLE_4XX || api.code in RETRYABLE_CODES
    }

    /** Tras tantos errores del servidor seguidos con el mismo pedido, se aparta para no atorar a los de atrás. */
    const val MAXIMUM_SERVER_FAILURES = 3

    /**
     * El servidor contestó un 5xx con su sobre: está arriba, pero algo falla con este pedido. Un 502/503/504 sin
     * sobre (deploy, proxy) es «servidor caído» como falta de red: no cuenta, o un minuto caído apartaría todo.
     */
    fun isServerError(error: Throwable): Boolean =
        (apiError(error)?.httpStatus ?: 0) >= 500 && !isConnectionProblem(error)

    /** El servidor aceptó el pedido (2xx) pero su respuesta no se pudo leer: ya está registrado. */
    fun acceptedUnreadable(error: Throwable): Boolean =
        generateSequence(error) { it.cause }.take(8).any { it is OfflineOrderAcceptedException }

    const val SERVER_FAILURE_PROBLEM =
        "El servidor falló varias veces con este pedido. Toca Reintentar o regístralo a mano."

    private fun apiError(error: Throwable): ApiClientException? =
        generateSequence(error) { it.cause }.take(8).filterIsInstance<ApiClientException>().firstOrNull()

    private const val HTTP_TOO_MANY_REQUESTS = 429

    /** 401 sesión vencida, 408 se cortó, 429 saturado. */
    private val RETRYABLE_4XX = setOf(401, 408, 429)

    private val RETRYABLE_CODES = setOf("UNAUTHORIZED", "TOKEN_EXPIRED", "INTERNAL_ERROR", "SERVICE_UNAVAILABLE")

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
    ): String = "${roleInitial.uppercaseChar()}${deviceTag.mod(1000)}-${sequence.toString().padStart(3, '0')}"
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

    /** Marca de este teléfono (0 a 999) para el folio local; la misma mientras no se reinstale la app. */
    fun deviceTag(): Int

    /** Las membresías que tienen algo guardado en este teléfono, para mandarlo aunque ya no esté abierto su modo. */
    fun scopesWithOrders(): List<OfflineStaffScope> = emptyList()
}

/** `POST /pedidos` respondió 2xx pero el cuerpo no se entendió: el pedido sí quedó; no se vuelve a mandar. */
class OfflineOrderAcceptedException(
    cause: Throwable,
) : IllegalStateException("El pedido se registró, pero la respuesta no se pudo leer.", cause)
