package com.vaiinilla.app.core.network

import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val DEFAULT_ERROR_MESSAGE = "No se pudo completar la operación. Intenta de nuevo."

/**
 * Converts transport failures into copy that is safe and useful to show in the UI.
 * API/domain messages are preserved because they are already written for the user.
 */
fun Throwable?.toUserFacingMessage(fallback: String = DEFAULT_ERROR_MESSAGE): String {
    if (this == null) return fallback

    val causes = generateSequence(this) { it.cause }.toList()
    val combinedMessage = causes.joinToString(" ") { it.message.orEmpty() }.lowercase()

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

        causes.any { it is ApiClientException && it.httpStatus in 500..599 } ->
            "Tuvimos un problema en el servidor. Intenta de nuevo en unos momentos."

        // Un cuerpo JSON que no cumple el contrato revienta con offsets y tokens
        // internos ("Unexpected JSON token at offset …"): nunca llegan al usuario.
        causes.any { it is SerializationException } ->
            "Recibimos una respuesta que esta versión no entiende. Actualiza la app e inténtalo de nuevo."

        else -> message?.trim().takeUnless { it.isNullOrEmpty() } ?: fallback
    }
}
