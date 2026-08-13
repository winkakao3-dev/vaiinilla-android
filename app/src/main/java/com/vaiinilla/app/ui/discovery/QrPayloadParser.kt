package com.vaiinilla.app.ui.discovery

import java.net.URI

sealed interface QrPayload {
    data class Establishment(
        val slug: String,
    ) : QrPayload

    data class User(
        val userId: String,
    ) : QrPayload

    data class SpaceToken(
        val token: String,
    ) : QrPayload
}

object QrPayloadParser {
    private val allowedHosts = setOf("vaiinilla.app", "www.vaiinilla.app")

    fun encodeUser(userId: String): String {
        val id = userId.trim()
        require(id.isNotEmpty()) { "El usuario no tiene identificador para el QR." }
        return "https://vaiinilla.app/u/$id"
    }

    fun parse(rawValue: String): Result<QrPayload> =
        runCatching {
            val value = rawValue.trim()
            require(value.isNotEmpty()) { "El QR está vacío." }

            val uri = runCatching { URI(value) }.getOrNull()
            val segments =
                uri
                    ?.path
                    ?.split('/')
                    ?.filter(String::isNotBlank)
                    .orEmpty()
            val isHttpsAppHost =
                uri?.let { parsed ->
                    parsed.scheme.equals("https", ignoreCase = true) &&
                        parsed.host?.lowercase() in allowedHosts &&
                        segments.size == 2 &&
                        segments.last().isNotBlank()
                } == true
            if (isHttpsAppHost && segments.first() == "e") {
                return@runCatching QrPayload.Establishment(segments.last())
            }
            if (isHttpsAppHost && segments.first() == "u") {
                return@runCatching QrPayload.User(segments.last())
            }

            // Space QR values are opaque by contract and are sent only in the request body.
            QrPayload.SpaceToken(value)
        }
}
