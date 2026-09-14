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
    private val spaceSlug = Regex("[A-Za-z0-9][A-Za-z0-9_-]*")
    private val reservedSpaceSlugs = setOf("e", "u", "invitaciones")
    private const val MAX_SPACE_SLUG_LENGTH = 100
    private const val MAX_SPACE_TOKEN_LENGTH = 256

    fun encodeUser(userId: String): String {
        val id = userId.trim()
        require(id.isNotEmpty()) { "El usuario no tiene identificador para el QR." }
        return "https://vaiinilla.app/u/$id"
    }

    fun encodeSpace(
        slug: String,
        token: String,
    ): String {
        val cleanSlug = slug.trim()
        val cleanToken = token.trim()
        require(cleanSlug.isNotEmpty()) { "El espacio no tiene slug de establecimiento para el QR." }
        require(cleanToken.isNotEmpty()) { "El espacio no tiene token para el QR." }
        return "https://vaiinilla.app/$cleanSlug/m/$cleanToken"
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
            val isHttpsHost =
                uri?.let { parsed ->
                    parsed.scheme.equals("https", ignoreCase = true) &&
                        parsed.host?.lowercase() in allowedHosts
                } == true
            val isHttpsAppHost =
                isHttpsHost && segments.size == 2 && segments.last().isNotBlank()
            if (isHttpsAppHost && segments.first() == "e") {
                return@runCatching QrPayload.Establishment(segments.last())
            }
            if (isHttpsAppHost && segments.first() == "u") {
                return@runCatching QrPayload.User(segments.last())
            }
            if (isHttpsHost && segments.size == 3 && segments[1] == "m") {
                val slug = segments[0]
                val token = segments[2]
                val validSlug =
                    slug.length in 1..MAX_SPACE_SLUG_LENGTH &&
                        spaceSlug.matches(slug) &&
                        slug !in reservedSpaceSlugs
                val validToken =
                    token.length in 1..MAX_SPACE_TOKEN_LENGTH && token.none(Char::isWhitespace)
                if (validSlug && validToken) {
                    return@runCatching QrPayload.SpaceToken(token)
                }
            }

            // Space QR values are opaque by contract and are sent only in the request body.
            QrPayload.SpaceToken(value)
        }
}
