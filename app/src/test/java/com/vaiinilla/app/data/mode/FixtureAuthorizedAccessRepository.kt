package com.vaiinilla.app.data.mode

import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixtureAuthorizedAccessRepository
    @Inject
    constructor() : AuthorizedAccessRepository {
        private val acceptedByUser = ConcurrentHashMap<String, MutableSet<AuthorizedMode>>()
        private val revokedByUser = ConcurrentHashMap<String, MutableSet<String>>()

        /** token → uid del único aceptante (idempotente para ese uid). */
        private val acceptedTokenOwners = ConcurrentHashMap<String, String>()

        override suspend fun invitation(token: String): Result<AuthorizedInvitation> =
            runCatching {
                fixtureInvitations[token.trim()]
                    ?: throw IllegalArgumentException("No encontramos esta invitación.")
            }

        override suspend fun acceptInvitation(
            token: String,
            session: StudentAuthSession,
        ): Result<AuthorizedMode> =
            runCatching {
                require(session.emailVerified) {
                    "Verifica tu correo antes de aceptar la invitación."
                }
                val normalizedToken = token.trim()
                val invitation = invitation(normalizedToken).getOrThrow()
                require(!invitation.revoked) { "Esta invitación fue revocada." }
                require(Instant.now().isBefore(invitation.expiresAt)) {
                    "Esta invitación ya expiró."
                }
                require(session.email.equals(invitation.invitedEmail, ignoreCase = true)) {
                    "La invitación no corresponde a este correo."
                }
                val previousOwner = acceptedTokenOwners[normalizedToken]
                require(previousOwner == null || previousOwner == session.uid) {
                    "Esta invitación ya fue utilizada."
                }
                val mode = invitation.toMode()
                val accepted = acceptedByUser.getOrPut(session.uid) { linkedSetOf() }
                accepted.removeIf { it.role == mode.role && it.establishmentId == mode.establishmentId }
                accepted.add(mode)
                acceptedTokenOwners[normalizedToken] = session.uid
                mode
            }

        override suspend fun authorizedModes(session: StudentAuthSession): Result<List<AuthorizedMode>> =
            runCatching {
                acceptedByUser[session.uid]
                    .orEmpty()
                    .filterNot { mode -> revokedByUser[session.uid].orEmpty().contains(modeKey(mode)) }
                    .sortedBy { it.role.ordinal }
            }

        override suspend fun activateMode(
            mode: AuthorizedMode,
            session: StudentAuthSession,
        ): Result<AuthorizedModeContext> =
            runCatching {
                require(session.emailVerified) {
                    "Verifica tu correo antes de cambiar de modo."
                }
                val available = authorizedModes(session).getOrThrow()
                require(available.any { it == mode }) {
                    "Este modo ya no está autorizado."
                }
                AuthorizedModeContext(
                    role = mode.role,
                    establishmentId = mode.establishmentId,
                    establishmentName = mode.establishmentName,
                    membershipId = mode.membershipId,
                    accessToken = "test-vaiinilla-${session.uid}-${mode.role.wireValue}",
                )
            }

        /**
         * Simula una revocación externa (admin/backend). No es una acción del invitado.
         */
        override suspend fun revokeMode(
            mode: AuthorizedMode,
            session: StudentAuthSession,
        ): Result<Unit> =
            runCatching {
                revokedByUser.getOrPut(session.uid) { linkedSetOf() }.add(modeKey(mode))
            }

        fun reset() {
            acceptedByUser.clear()
            revokedByUser.clear()
            acceptedTokenOwners.clear()
        }

        fun seedGalleryModes(session: StudentAuthSession) {
            val modes =
                fixtureInvitations.values
                    .filter { it.invitedEmail.equals(session.email, ignoreCase = true) }
                    .filter { it.role != OperationalRole.CLIENT && !it.revoked }
                    .filter { Instant.now().isBefore(it.expiresAt) }
                    .map { it.toMode() }
                    .toMutableSet()
            acceptedByUser[session.uid] = modes
            revokedByUser.remove(session.uid)
        }

        private fun modeKey(mode: AuthorizedMode): String = "${mode.establishmentId}:${mode.role.wireValue}"

        private fun AuthorizedInvitation.toMode(): AuthorizedMode =
            AuthorizedMode(
                role = requireNotNull(role) { "La invitación no tiene un rol autorizado." },
                establishmentId = establishmentId,
                establishmentName = establishmentName,
                membershipId = "test-membership-$id",
            )

        private companion object {
            val fixtureInvitations =
                listOf(
                    AuthorizedInvitation(
                        id = "invite-cashier",
                        token = "vai27-valid-cashier",
                        establishmentId = "8246ff44-aad0-4e49-9268-b71c997893fe",
                        establishmentName = "Cafetería Centro",
                        invitedEmail = "ana@vaiinilla.test",
                        role = OperationalRole.CASHIER,
                        expiresAt = Instant.parse("2099-01-01T00:00:00Z"),
                    ),
                    AuthorizedInvitation(
                        id = "invite-kitchen",
                        token = "vai27-valid-kitchen",
                        establishmentId = "8246ff44-aad0-4e49-9268-b71c997893fe",
                        establishmentName = "Cafetería Centro",
                        invitedEmail = "ana@vaiinilla.test",
                        role = OperationalRole.KITCHEN,
                        expiresAt = Instant.parse("2099-01-01T00:00:00Z"),
                    ),
                    AuthorizedInvitation(
                        id = "invite-waiter",
                        token = "vai27-valid-waiter",
                        establishmentId = "8246ff44-aad0-4e49-9268-b71c997893fe",
                        establishmentName = "Cafetería Centro",
                        invitedEmail = "ana@vaiinilla.test",
                        role = OperationalRole.WAITER,
                        expiresAt = Instant.parse("2099-01-01T00:00:00Z"),
                    ),
                    AuthorizedInvitation(
                        id = "invite-expired",
                        token = "vai27-expired",
                        establishmentId = "8246ff44-aad0-4e49-9268-b71c997893fe",
                        establishmentName = "Cafetería Centro",
                        invitedEmail = "ana@vaiinilla.test",
                        role = OperationalRole.CASHIER,
                        expiresAt = Instant.parse("2020-01-01T00:00:00Z"),
                    ),
                    AuthorizedInvitation(
                        id = "invite-revoked",
                        token = "vai27-revoked",
                        establishmentId = "8246ff44-aad0-4e49-9268-b71c997893fe",
                        establishmentName = "Cafetería Centro",
                        invitedEmail = "ana@vaiinilla.test",
                        role = OperationalRole.WAITER,
                        expiresAt = Instant.parse("2099-01-01T00:00:00Z"),
                        revoked = true,
                    ),
                ).associateBy { it.token }
        }
    }
