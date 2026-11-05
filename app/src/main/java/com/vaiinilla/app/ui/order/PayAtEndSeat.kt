package com.vaiinilla.app.ui.order

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.ui.sharedtable.SHARED_TABLE_ALIAS_MAX

/** El servidor solo carga a la cuenta de la mesa a quien está sentado en ella (mesa compartida). */
internal const val PAYMENT_AT_END_REQUIRES_SEAT = "PAYMENT_AT_END_REQUIRES_SEAT"
internal const val PARTICIPANT_NAME_TAKEN = "PARTICIPANT_NAME_TAKEN"

/**
 * Pagar al final desde el QR: escanear (o escribir el código) solo elige la mesa, no sienta al cliente en la
 * mesa compartida, y sin silla el servidor rechaza el pedido a la cuenta. Si responde eso, se sienta con su
 * nombre (si ese nombre ya está en la mesa, con el nombre completo) y se reintenta una vez con la misma llave
 * de idempotencia: el rechazo deshizo la transacción, así que la llave sigue libre.
 */
internal fun <T> createSeatingAtTableIfNeeded(
    create: () -> Result<T>,
    aliases: List<String>,
    join: ((String) -> Result<*>)?,
): Result<T> {
    val first = create()
    if (first.exceptionOrNull()?.serverCode() != PAYMENT_AT_END_REQUIRES_SEAT) return first
    if (join == null || aliases.isEmpty()) return first
    var joined: Result<*> = first
    for (alias in aliases.distinct()) {
        joined = join(alias)
        if (joined.isSuccess || joined.exceptionOrNull()?.serverCode() != PARTICIPANT_NAME_TAKEN) break
    }
    joined.exceptionOrNull()?.let { return Result.failure(it) }
    return create()
}

/** Los nombres con que se intenta sentar: el primer nombre, como al llamar al mesero, y luego el completo. */
internal fun payAtEndSeatAliases(displayName: String): List<String> {
    val full =
        displayName
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(SHARED_TABLE_ALIAS_MAX)
    return listOfNotNull(waiterCallAlias(displayName), full.takeIf { it.isNotEmpty() }).distinct()
}

private fun Throwable.serverCode(): String? =
    generateSequence(this) { it.cause }.firstNotNullOfOrNull { cause ->
        when (cause) {
            is OrderRepositoryException -> cause.code
            is ApiClientException -> cause.code
            else -> null
        }
    }
