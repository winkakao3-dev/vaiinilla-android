package com.vaiinilla.app.core.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.security.MessageDigest
import java.util.Base64

internal object JwtContextScope {
    private val json = Json

    fun stableKey(token: String): String? {
        val payload = token.split('.').getOrNull(1) ?: return null
        val claims =
            runCatching {
                val decoded = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
                json.parseToJsonElement(decoded).jsonObject
            }.getOrNull() ?: return null
        val uid =
            claims.textClaim("uid")?.takeIf { it.isNotBlank() }
                ?: claims.textClaim("sub")?.takeIf { it.isNotBlank() }
                ?: return null
        val role = claims.textClaim("rol")?.takeIf { it.isNotBlank() } ?: return null
        val establishmentId =
            claims.textClaim("establecimiento_id")?.takeIf { it.isNotBlank() } ?: return null
        val membershipId =
            claims.textClaim("membresia_id")?.takeIf { it.isNotBlank() } ?: return null
        return "$uid:$role:$establishmentId:$membershipId"
    }

    fun storageKey(token: String): String = stableKey(token) ?: "token:${sha256(token)}"

    fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun JsonObject.textClaim(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull
}
