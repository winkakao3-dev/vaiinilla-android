package com.vaiinilla.app.domain.counter

import com.vaiinilla.app.core.network.ApiClientException
import java.io.IOException
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/**
 * Una venta de Caja a un alumno que no se pudo mandar por falta de conexión. Se manda sola al volver;
 * su `id` es la llave de idempotencia, así que si el primer intento sí había entrado no se cobra dos veces.
 */
data class OfflineStudentPurchase(
    val id: String,
    val studentId: String,
    val studentName: String,
    val items: List<StudentPurchaseItem>,
    val total: BigDecimal,
    val createdAt: Instant,
    /** Por qué el servidor no la aceptó. Con él ya no se reintenta: Caja la cobra de otra forma. */
    val problem: String? = null,
)

/** Lo que Caja recuerda de un alumno para atenderlo sin conexión el mismo día. */
data class CachedStudentCard(
    val userId: String,
    val name: String,
    val studentId: String?,
    val group: String?,
    val balance: BigDecimal,
    val spentToday: BigDecimal,
    val dailyLimit: BigDecimal?,
    val allergens: List<String>,
    val requiresPin: Boolean,
) {
    val card: StudentCard
        get() =
            StudentCard(
                userId = userId,
                name = name,
                studentId = studentId,
                group = group,
                hasOwnAccount = false,
                balance = balance,
                spentToday = spentToday,
                dailyLimit = dailyLimit,
                allergens = allergens,
                todayOrders = emptyList(),
                requiresPin = requiresPin,
            )

    companion object {
        fun of(card: StudentCard): CachedStudentCard =
            CachedStudentCard(
                userId = card.userId,
                name = card.name,
                studentId = card.studentId,
                group = card.group,
                balance = card.balance,
                spentToday = card.spentToday,
                dailyLimit = card.dailyLimit,
                allergens = card.allergens,
                requiresPin = card.requiresPin,
            )
    }
}

/** Lo que Caja guarda en el teléfono: las fichas del día y las ventas por mandar. */
data class OfflineCounterSnapshot(
    val day: String,
    val cards: List<CachedStudentCard> = emptyList(),
    val purchases: List<OfflineStudentPurchase> = emptyList(),
) {
    /** Las fichas son de un día: otro día el saldo y lo gastado ya no valen. Las ventas se quedan hasta mandarse. */
    fun startingDay(today: String): OfflineCounterSnapshot =
        if (day == today) this else OfflineCounterSnapshot(day = today, cards = emptyList(), purchases = purchases)

    fun remember(card: StudentCard): OfflineCounterSnapshot =
        copy(cards = cards.filterNot { it.userId == card.userId } + CachedStudentCard.of(card))

    fun cardsMatching(text: String): List<StudentCard> {
        val key = text.searchKey()
        if (key.length < 2) return emptyList()
        return cards
            .filter { it.name.searchKey().contains(key) || it.studentId?.searchKey()?.contains(key) == true }
            .sortedBy { it.name }
            .map { it.card }
    }

    /** Lo ya vendido sin conexión a ese alumno y que todavía no se manda. */
    fun queued(studentId: String): BigDecimal =
        purchases
            .filter { it.studentId == studentId && it.problem == null }
            .fold(BigDecimal.ZERO) { sum, purchase -> sum + purchase.total }

    val pending: List<OfflineStudentPurchase> get() = purchases.filter { it.problem == null }
    val problems: List<OfflineStudentPurchase> get() = purchases.filter { it.problem != null }
}

object OfflineCounterRules {
    /** Sin conexión no se ve el saldo real: a cada alumno se le fía como mucho esto. */
    val MAXIMUM_PER_STUDENT: BigDecimal = BigDecimal("200.00")

    /** Una venta de otro turno ya no se cobra sola: el saldo y los controles del alumno pudieron cambiar. */
    val MAXIMUM_AGE: Duration = Duration.ofHours(12)

    /** Mientras haya pendientes, aunque la red parezca bien (el servidor puede estar caído). */
    const val RETRY_INTERVAL_MS = 30_000L

    const val TOO_OLD_PROBLEM = "Pasaron más de 12 horas sin conexión. Cóbralo de otra forma."

    /**
     * Lo que todavía se le puede cobrar sin conexión: lo menor entre lo que puede gastar hoy y el tope,
     * menos lo que ya se le vendió sin conexión.
     */
    fun allowance(
        card: StudentCard,
        queued: BigDecimal,
    ): BigDecimal = (card.spendableToday.min(MAXIMUM_PER_STUDENT) - queued).max(BigDecimal.ZERO)

    sealed interface Refusal {
        /** El PIN solo lo revisa el servidor. */
        data object NeedsPin : Refusal

        data class OverAllowance(
            val allowance: BigDecimal,
        ) : Refusal
    }

    fun refusal(
        total: BigDecimal,
        card: StudentCard,
        queued: BigDecimal,
    ): Refusal? {
        if (card.requiresPin) return Refusal.NeedsPin
        val allowance = allowance(card, queued)
        return if (total > allowance) Refusal.OverAllowance(allowance) else null
    }

    /**
     * Falta de red, servidor caído (5xx sin sobre) o saturado (429): la venta se puede guardar y reenviar
     * con la misma llave. Un rechazo con su código (saldo, bloqueo, alergia) no: eso ya es la respuesta.
     */
    fun isConnectionProblem(error: Throwable): Boolean {
        val causes = generateSequence(error) { it.cause }.take(8).toList()
        if (causes.any { it is CancellationException }) return false
        if (causes.any { it is IOException }) return true
        val api = causes.filterIsInstance<ApiClientException>().firstOrNull() ?: return false
        return api.httpStatus == 429 || (api.httpStatus in 500..599 && api.code.startsWith("HTTP_5"))
    }

    fun isTooOld(
        purchase: OfflineStudentPurchase,
        now: Instant,
    ): Boolean = Duration.between(purchase.createdAt, now) > MAXIMUM_AGE
}

/** De quién es la cola: un cajero (su membresía) en un negocio. */
data class CounterScope(
    val membershipId: String,
    val establishmentId: String,
)

/** Donde vive la cola en el teléfono; se guarda antes de cualquier reintento. */
interface OfflineCounterStore {
    fun load(
        scope: CounterScope,
        today: String,
    ): OfflineCounterSnapshot

    fun save(
        scope: CounterScope,
        snapshot: OfflineCounterSnapshot,
    )
}

/** Avisa cuando el teléfono vuelve a tener red, para mandar lo guardado sin esperar al siguiente intento. */
fun interface ConnectivityWatcher {
    /** Devuelve con qué dejar de escuchar. */
    fun onReconnect(callback: () -> Unit): AutoCloseable
}
