package com.vaiinilla.app.core.network

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

        else -> message?.trim().takeUnless { it.isNullOrEmpty() } ?: fallback
    }
}
