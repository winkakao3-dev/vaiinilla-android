package com.vaiinilla.app.domain.mode

import com.vaiinilla.app.domain.model.OperationalRole
import java.time.Instant

data class AuthorizedInvitation(
    val id: String = "",
    val token: String,
    val establishmentId: String = "",
    val establishmentName: String = "",
    val invitedEmail: String = "",
    val role: OperationalRole? = null,
    val expiresAt: Instant? = null,
    val revoked: Boolean = false,
    /** True when the backend intentionally withholds preview metadata until acceptance. */
    val requiresRemoteAcceptance: Boolean = false,
)

data class AuthorizedMode(
    val role: OperationalRole,
    val establishmentId: String,
    val establishmentName: String,
    val membershipId: String,
    val clientIdentifier: String? = null,
)

/**
 * Restriction sent by the backend when an establishment is suspended or closing.
 * The server remains the authorization authority; this value is only the canonical
 * context metadata that the client can use to explain the current surface.
 */
enum class RestrictedMode(
    val wireValue: String,
    val label: String,
) {
    READ_ONLY("solo_lectura", "Solo lectura"),
    OPERATIONAL_CLOSE("cierre_operativo", "Cierre operativo"),
    ;

    companion object {
        fun fromWireValue(value: String?): RestrictedMode? {
            val normalized = value?.trim()?.lowercase().orEmpty()
            if (normalized.isEmpty()) return null
            return entries.firstOrNull { it.wireValue == normalized }
                ?: throw IllegalArgumentException("El servidor devolvió un modo restringido no soportado: $value")
        }
    }
}

data class AuthorizedModeContext(
    val role: OperationalRole,
    val establishmentId: String,
    val establishmentName: String,
    val membershipId: String,
    val accessToken: String,
    val expiresIn: Int = 900,
    val restrictedMode: RestrictedMode? = null,
)
