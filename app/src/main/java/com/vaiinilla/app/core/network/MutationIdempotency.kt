package com.vaiinilla.app.core.network

import com.vaiinilla.app.core.security.JwtContextScope
import java.security.MessageDigest

/**
 * Claves de idempotencia deterministas para mutaciones operativas.
 *
 * Cuando una mutación pierde su respuesta (proceso muerto, timeout, red
 * caída), reintentarla con la MISMA clave hace que el servidor devuelva la
 * respuesta almacenada en lugar de ejecutarla dos veces o responder con un
 * conflicto de versión. Cada clave se deriva de la identidad lógica de la
 * operación — pedido + estado destino + versión esperada, o producto +
 * contenido de la imagen — así que una operación genuinamente distinta jamás
 * reutiliza una clave vieja.
 *
 * No usar para toggles sin versión (ej. disponibilidad de producto): dos
 * operaciones distintas con los mismos parámetros colapsarían en una sola
 * clave y el servidor deduplicaría la segunda aunque sea legítima. Ahí el
 * patrón correcto es re-leer el estado real tras un fallo.
 */
internal object MutationIdempotency {
    fun orderTransition(
        orderId: String,
        targetState: String,
        expectedVersion: Int,
        pickupToken: String?,
    ): String = derive("transition", orderId, targetState, expectedVersion.toString(), pickupToken)

    fun cashCollection(
        orderId: String,
        amountReceived: String,
        expectedVersion: Int,
    ): String = derive("collect-cash", orderId, amountReceived, expectedVersion.toString())

    fun productImage(
        productId: Int,
        imageBytes: ByteArray,
    ): String = derive("product-image", productId.toString(), sha256Bytes(imageBytes))

    fun accountDeletion(uid: String): String = derive("account-deletion", uid)

    private fun derive(
        operation: String,
        vararg parts: String?,
    ): String =
        "mk_" +
            JwtContextScope.sha256(
                (listOf(operation) + parts.map { it ?: "null" }).joinToString("|"),
            )

    private fun sha256Bytes(value: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value)
            .joinToString("") { "%02x".format(it) }
}
