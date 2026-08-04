package com.vaiinilla.app.ui.discovery

import java.net.URI

sealed interface QrPayload {
    data class Establishment(
        val slug: String,
    ) : QrPayload

    data class SpaceToken(
        val token: String,
    ) : QrPayload
}

object QrPayloadParser {
    private val allowedHosts = setOf("vaiinilla.app", "www.vaiinilla.app")

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
            val isCanonicalEstablishmentQr =
                uri?.let { parsed ->
                    parsed.scheme.equals("https", ignoreCase = true) &&
                        parsed.host?.lowercase() in allowedHosts &&
                        segments.size == 2 &&
                        segments.first() == "e" &&
                        segments.last().isNotBlank()
                } == true
            if (isCanonicalEstablishmentQr) {
                return@runCatching QrPayload.Establishment(segments.last())
            }

            // Space QR values are opaque by contract and are sent only in the request body.
            QrPayload.SpaceToken(value)
        }
}
