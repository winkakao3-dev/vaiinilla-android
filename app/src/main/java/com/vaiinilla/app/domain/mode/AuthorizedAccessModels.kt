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
)

data class AuthorizedModeContext(
    val role: OperationalRole,
    val establishmentId: String,
    val establishmentName: String,
    val membershipId: String,
    val accessToken: String,
    val expiresIn: Int = 900,
)
