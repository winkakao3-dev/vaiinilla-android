package com.vaiinilla.app.domain.outbox

import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedMode
import kotlin.coroutines.cancellation.CancellationException

/** Cómo terminó un recorrido de la bandeja de salida: lo que falta se vuelve a intentar con espera. */
enum class DrainOutcome {
    /** No queda nada que mandar o lo que queda ya lo apartó el personal como problema. */
    DONE,

    /** Falta de red, servidor caído o sesión por renovar: queda algo por mandar y se reintenta. */
    RETRY_LATER,
    ;

    infix fun and(other: DrainOutcome): DrainOutcome =
        if (this == RETRY_LATER ||
            other == RETRY_LATER
        ) {
            RETRY_LATER
        } else {
            DONE
        }
}

/**
 * Con quién se manda lo guardado cuando no hay pantalla abierta: la cuenta, sus modos de personal y un token por
 * modo. El token se pide una sola vez por modo en cada recorrido aunque varias colas lo necesiten.
 */
class OutboxContext(
    val session: StudentAuthSession,
    val modes: List<AuthorizedMode>,
    private val activate: suspend (AuthorizedMode) -> String?,
) {
    private val tokens = mutableMapOf<String, String?>()

    fun modeOfMembership(membershipId: String): AuthorizedMode? =
        modes.firstOrNull { it.membershipId.equals(membershipId, ignoreCase = true) }

    suspend fun tokenFor(mode: AuthorizedMode): String? =
        if (tokens.containsKey(mode.membershipId)) {
            tokens[mode.membershipId]
        } else {
            activate(mode).also { tokens[mode.membershipId] = it }
        }
}

/** Una de las colas guardadas en el teléfono (pedidos del personal, ventas de cafetería…). */
interface OutboxSource {
    val name: String

    /** Hay algo guardado que todavía se puede mandar; evita pedir sesión y token de balde. */
    fun hasWork(): Boolean

    suspend fun drain(context: OutboxContext): DrainOutcome
}

/** Pide un recorrido de la bandeja aunque la app se cierre; lo cumple WorkManager. */
interface OutboxScheduler {
    /** Pronto: apenas haya red. Varias peticiones seguidas se juntan en un solo recorrido. */
    fun requestDrain()

    /** Otro intento después de [delayMillis] porque quedó algo por mandar. */
    fun scheduleRetry(
        attempt: Int,
        delayMillis: Long,
    )

    object None : OutboxScheduler {
        override fun requestDrain() = Unit

        override fun scheduleRetry(
            attempt: Int,
            delayMillis: Long,
        ) = Unit
    }
}

/** Espera entre intentos: 30 s, 1 min, 2 min, 4 min y de ahí 5 min; una comanda no espera horas por un tropiezo. */
object OutboxBackoff {
    const val FIRST_DELAY_MS = 30_000L
    const val MAXIMUM_DELAY_MS = 300_000L

    fun delayMillis(attempt: Int): Long {
        val doublings = attempt.coerceIn(0, 10)
        return (FIRST_DELAY_MS shl doublings).coerceAtMost(MAXIMUM_DELAY_MS)
    }
}

/**
 * El único recorrido de salida: pasa por cada cola, una tras otra, y junta el resultado. Una cola que truena no
 * detiene a las demás; se reintenta sola.
 */
class OutboxDispatcher(
    private val sources: List<OutboxSource>,
    private val onSourceFailure: (name: String, error: Throwable) -> Unit = { _, _ -> },
) {
    fun hasWork(): Boolean = sources.any { it.hasWork() }

    suspend fun drainAll(context: OutboxContext): DrainOutcome {
        var outcome = DrainOutcome.DONE
        for (source in sources) {
            val result =
                try {
                    source.drain(context)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    onSourceFailure(source.name, error)
                    DrainOutcome.RETRY_LATER
                }
            outcome = outcome and result
        }
        return outcome
    }
}
