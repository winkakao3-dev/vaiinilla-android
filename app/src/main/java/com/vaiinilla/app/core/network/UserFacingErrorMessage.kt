package com.vaiinilla.app.core.network

import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val DEFAULT_ERROR_MESSAGE = "No se pudo completar la operación. Intenta de nuevo."

/** Mensajes del servidor que nombran un detalle técnico (versión, token): se dicen por su código (STAFF-21). */
private val CODED_MESSAGES =
    mapOf(
        "ORDER_VERSION_CONFLICT" to "El pedido cambió mientras lo tenías abierto. Revísalo e inténtalo de nuevo.",
        "INVALID_PICKUP_TOKEN" to "Ese código de recogida no es válido. Pide al cliente que lo muestre de nuevo.",
        "UNAUTHENTICATED" to "Tu sesión caducó. Vuelve a iniciar sesión.",
        "ACCOUNT_REQUIRED" to "Para apartar necesitas una cuenta. Crea una o inicia sesión.",
        "INSUFFICIENT_WALLET_BALANCE" to "No te alcanza el saldo. Recarga tu cartera o paga en caja.",
    )

/**
 * Converts transport failures into copy that is safe and useful to show in the UI.
 * API/domain messages are preserved because they are already written for the user.
 */
fun Throwable?.toUserFacingMessage(fallback: String = DEFAULT_ERROR_MESSAGE): String {
    if (this == null) return fallback

    val causes = generateSequence(this) { it.cause }.toList()
    val combinedMessage = causes.joinToString(" ") { it.message.orEmpty() }.lowercase()

    val codedMessage = causes.filterIsInstance<ApiClientException>().firstNotNullOfOrNull { CODED_MESSAGES[it.code] }

    return when {
        causes.any { it is UnknownHostException } ||
            "unable to resolve host" in combinedMessage ||
            "localhost.invalid" in combinedMessage ->
            "No pudimos conectar con Vaiinilla. Revisa tu conexión e inténtalo de nuevo."

        causes.any { it is SocketTimeoutException } || "timeout" in combinedMessage ->
            "La conexión tardó demasiado. Inténtalo de nuevo."

        causes.any { it is ConnectException } ||
            "failed to connect" in combinedMessage ||
            "connection refused" in combinedMessage ->
            "No pudimos contactar al servidor. Inténtalo de nuevo en unos segundos."

        codedMessage != null -> codedMessage

        causes.any { it is ApiClientException && it.httpStatus in 500..599 } ->
            "Tuvimos un problema en el servidor. Intenta de nuevo en unos momentos."

        // Un cuerpo JSON que no cumple el contrato revienta con offsets y tokens
        // internos ("Unexpected JSON token at offset …"): nunca llegan al usuario.
        causes.any { it is SerializationException } ->
            "Recibimos una respuesta que esta versión no entiende. Actualiza la app e inténtalo de nuevo."

        else -> message?.trim().takeUnless { it.isNullOrEmpty() } ?: fallback
    }
}
