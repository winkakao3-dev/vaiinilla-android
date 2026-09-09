package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CreateOrderRequest
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
            request.items.forEach { item ->
                append('|')
                append(item.productId)
                append(':')
                append(item.quantity)
                append(':')
                append(item.optionIds.sorted().joinToString(","))
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
