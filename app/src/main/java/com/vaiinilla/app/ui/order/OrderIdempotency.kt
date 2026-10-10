package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderOrigin
import java.security.MessageDigest

/**
 * Deterministic fingerprint of an order's content, used to key idempotency
 * persistence: two submissions with the exact same cart/checkout content
 * reuse the same pending key (safe to retry after a lost response), while
 * any change to the cart invalidates it (a genuinely different order must
 * never replay a stale key).
 */
internal fun createOrderFingerprint(request: CreateOrderRequest): String {
    val canonical =
        buildString {
            append(request.paymentMethod.wireValue)
            append('|')
            append(request.destination.wireValue)
            append('|')
            append(request.spaceId ?: "null")
            append('|')
            append(request.kitchenNotes)
            // Solo cuando es true, para no cambiar la huella de los pedidos de siempre.
            if (request.payAtEnd) append("|pago_al_final")
            // Solo en pedidos de mostrador: el nombre cambia el pedido, así que no puede reusar la llave.
            if (request.origin == OrderOrigin.MOSTRADOR) {
                val name = request.customerName.orEmpty()
                append("|origen:mostrador|nombre:")
                append(name.length)
                append(':')
                append(name)
            }
            request.items.forEach { item ->
                append('|')
                append(item.productId)
                append(':')
                append(item.quantity)
                append(':')
                append(item.optionIds.sorted().joinToString(","))
                // Solo con nota, para no cambiar la huella de las líneas de siempre. El largo evita
                // que una nota con "|" o ":" imite otra línea.
                if (item.notes.isNotEmpty()) {
                    append(":n")
                    append(item.notes.length)
                    append(':')
                    append(item.notes)
                }
            }
        }
    return MessageDigest
        .getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

/**
 * Resolves which idempotency key a mutation (order creation, Stripe retry) should
 * use: an in-memory key already generated this session, then a key persisted
 * from a previous attempt that never got a response, and only failing both of
 * those does it generate (and let the caller persist) a brand-new one. This is
 * what prevents a lost response from turning into a duplicate order/charge.
 */
internal fun resolveIdempotencyKey(
    inMemoryKey: String?,
    persistedKey: String?,
    generate: () -> String,
): String = inMemoryKey ?: persistedKey ?: generate()

/** Llave de un pedido de mostrador junto con la huella del contenido que la generó. */
internal data class CounterOrderKey(
    val fingerprint: String,
    val key: String,
)

/**
 * Un reintento del mismo pedido (respuesta perdida) reusa la llave; si el pedido cambió, genera una nueva,
 * porque reusar la anterior con otro contenido devuelve 409.
 */
internal fun counterOrderKeyFor(
    previous: CounterOrderKey?,
    fingerprint: String,
    generate: () -> String,
): CounterOrderKey = if (previous?.fingerprint == fingerprint) previous else CounterOrderKey(fingerprint, generate())
