package com.vaiinilla.app.data.mode

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.data.auth.SesionesContextoDataDto
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRepository
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRequest
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Only the routes that the backend contract exposes for a real Firebase identity. */
interface AuthorizedAccessApi {
    suspend fun listAccess(firebaseIdToken: String): Result<String>

    suspend fun acceptInvitation(
        firebaseIdToken: String,
        token: String,
        idempotencyKey: String,
    ): Result<String>

    suspend fun activateContext(
        firebaseIdToken: String,
        membershipId: String,
        idempotencyKey: String,
    ): Result<String>
}

@Singleton
class RemoteAuthorizedAccessApi
    @Inject
    constructor(
        private val apiClient: HttpVaiinillaApiClient,
    ) : AuthorizedAccessApi {
        override suspend fun listAccess(firebaseIdToken: String): Result<String> =
            withContext(Dispatchers.IO) {
                apiClient.getWithBearer(
                    bearer = firebaseIdToken,
                    path = "sesiones/accesos",
                )
            }

        override suspend fun acceptInvitation(
            firebaseIdToken: String,
            token: String,
            idempotencyKey: String,
        ): Result<String> =
            withContext(Dispatchers.IO) {
                apiClient.postWithBearer(
                    bearer = firebaseIdToken,
                    path = "invitaciones/aceptar",
                    body = json.encodeToString(InvitationAcceptanceRequest(token)),
                    headers = mapOf("Idempotency-Key" to idempotencyKey),
                )
            }

        override suspend fun activateContext(
            firebaseIdToken: String,
            membershipId: String,
            idempotencyKey: String,
        ): Result<String> =
            withContext(Dispatchers.IO) {
                apiClient.postWithBearer(
                    bearer = firebaseIdToken,
                    path = "sesiones/contexto",
                    body = json.encodeToString(ContextRequest(membershipId)),
                    headers = mapOf("Idempotency-Key" to idempotencyKey),
                )
            }

        private companion object {
            val json =
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = false
                    explicitNulls = false
                }
        }
    }

@Singleton
class RemoteAuthorizedAccessRepository
    @Inject
    constructor(
        private val api: AuthorizedAccessApi,
        private val authRepository: StudentAuthRepository,
        private val enrollmentRepository: StudentEnrollmentRepository,
    ) : AuthorizedAccessRepository {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
                explicitNulls = false
            }

        /**
         * The backend deliberately has no token-preview endpoint. Showing a fake role or
         * establishment here would turn local UI data into an authorization decision, so the
         * real flow displays only that the server will validate the invitation on acceptance.
         */
        override suspend fun invitation(token: String): Result<AuthorizedInvitation> =
            runCatching {
                val normalized = token.trim()
                require(normalized.isNotEmpty()) { "No hay una invitación seleccionada." }
                AuthorizedInvitation(
                    id = "remote-pending",
                    token = normalized,
                    requiresRemoteAcceptance = true,
                )
            }

        override suspend fun acceptInvitation(
            token: String,
            session: StudentAuthSession,
        ): Result<AuthorizedMode> =
            runCatching {
                require(session.emailVerified) {
                    "Verifica tu correo antes de aceptar la invitación."
                }
                val firebaseIdToken = authRepository.getIdToken(forceRefresh = true).getOrThrow()
                enrollmentRepository
                    .enroll(
                        StudentEnrollmentRequest(
                            nombre =
                                session.displayName.trim().ifBlank {
                                    session.email
                                        .substringBefore('@')
                                        .trim()
                                        .ifBlank { "Usuario" }
                                },
                            terminosVersion = "2026-07",
                            privacidadVersion = "2026-07",
                        ),
                        firebaseIdToken = firebaseIdToken,
                    ).getOrThrow()
                val data =
                    json
                        .decodeFromString<Envelope<InvitationAcceptanceDto>>(
                            api
                                .acceptInvitation(
                                    firebaseIdToken = firebaseIdToken,
                                    token = token.trim(),
                                    idempotencyKey = stableIdempotencyKey("invitation", token),
                                ).getOrThrow(),
                        ).data
                require(data.membresia.activo) {
                    "El servidor no activó la membresía de la invitación."
                }
                require(data.membresia.id.isNotBlank() && data.membresia.establishmentId.isNotBlank()) {
                    "La respuesta de la invitación no contiene una membresía válida."
                }
                val role = roleFromWire(data.membresia.rol)
                AuthorizedMode(
                    role = role,
                    establishmentId = data.membresia.establishmentId,
                    establishmentName = "Establecimiento",
                    membershipId = data.membresia.id,
                )
            }

        override suspend fun authorizedModes(session: StudentAuthSession): Result<List<AuthorizedMode>> =
            runCatching {
                require(session.emailVerified) {
                    "Verifica tu correo antes de consultar tus accesos."
                }
                val firebaseIdToken = authRepository.getIdToken(forceRefresh = false).getOrThrow()
                json
                    .decodeFromString<Envelope<List<SessionAccessDto>>>(
                        api.listAccess(firebaseIdToken).getOrThrow(),
                    ).data
                    .mapNotNull { access -> access.toDomainOrNull() }
                    .sortedWith(compareBy({ it.role.ordinal }, { it.establishmentName }))
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
                require(available.any { it.sameAccessAs(mode) }) {
                    "Este modo ya no está autorizado."
                }
                val firebaseIdToken = authRepository.getIdToken(forceRefresh = false).getOrThrow()
                val data =
                    json
                        .decodeFromString<Envelope<SesionesContextoDataDto>>(
                            api
                                .activateContext(
                                    firebaseIdToken = firebaseIdToken,
                                    membershipId = mode.membershipId,
                                    idempotencyKey = UUID.randomUUID().toString(),
                                ).getOrThrow(),
                        ).data
                val context =
                    data.contexto
                        ?: throw IllegalStateException("El servidor no devolvió el contexto activo.")
                require(data.tokenType.equals("Bearer", ignoreCase = true)) {
                    "El servidor devolvió un tipo de token no soportado."
                }
                require(data.accessToken.isNotBlank() && data.expiresIn > 0) {
                    "El servidor devolvió una sesión operativa inválida."
                }
                require(context.membresiaId == mode.membershipId) {
                    "El contexto recibido no corresponde al acceso seleccionado."
                }
                require(context.establecimientoId == mode.establishmentId) {
                    "El contexto recibido pertenece a otro establecimiento."
                }
                val role = roleFromWire(context.rol)
                require(role == mode.role) {
                    "El servidor devolvió un rol distinto al seleccionado."
                }
                AuthorizedModeContext(
                    role = role,
                    establishmentId = context.establecimientoId,
                    establishmentName = mode.establishmentName,
                    membershipId = context.membresiaId,
                    accessToken = data.accessToken,
                    expiresIn = data.expiresIn,
                    restrictedMode = RestrictedMode.fromWireValue(context.modoRestringido),
                )
            }

        override suspend fun revokeMode(
            mode: AuthorizedMode,
            session: StudentAuthSession,
        ): Result<Unit> =
            Result.failure(
                UnsupportedOperationException(
                    "La revocación se realiza desde administración; actualiza tus accesos para detectarla.",
                ),
            )

        private fun SessionAccessDto.toDomainOrNull(): AuthorizedMode? {
            val role = roleFromWireOrNull(rol) ?: return null
            if (membresiaId.isBlank() || establecimiento.id.isBlank()) return null
            return AuthorizedMode(
                role = role,
                establishmentId = establecimiento.id,
                establishmentName = establecimiento.nombre,
                membershipId = membresiaId,
                clientIdentifier = clientIdentifier,
            )
        }

        private fun roleFromWire(value: String): OperationalRole =
            roleFromWireOrNull(value)
                ?: throw IllegalArgumentException("El servidor devolvió un modo no soportado: $value")

        private fun roleFromWireOrNull(value: String): OperationalRole? =
            when (value.trim().lowercase()) {
                OperationalRole.CLIENT.wireValue -> OperationalRole.CLIENT
                OperationalRole.CASHIER.wireValue -> OperationalRole.CASHIER
                OperationalRole.KITCHEN.wireValue -> OperationalRole.KITCHEN
                OperationalRole.WAITER.wireValue -> OperationalRole.WAITER
                // Administración remains intentionally outside the Android client.
                "admin" -> null
                else -> null
            }

        private fun AuthorizedMode.sameAccessAs(other: AuthorizedMode): Boolean =
            role == other.role &&
                establishmentId == other.establishmentId &&
                membershipId == other.membershipId

        private fun stableIdempotencyKey(
            operation: String,
            value: String,
        ): String =
            UUID
                .nameUUIDFromBytes(
                    "vaiinilla:staff:$operation:${value.trim()}".toByteArray(StandardCharsets.UTF_8),
                ).toString()
    }

@Serializable
private data class Envelope<T>(
    val data: T,
    val meta: JsonObject? = null,
    val error: JsonElement? = null,
)

@Serializable
private data class InvitationAcceptanceRequest(
    val token: String,
)

@Serializable
private data class ContextRequest(
    @SerialName("membresia_id") val membershipId: String,
)

@Serializable
private data class InvitationAcceptanceDto(
    @SerialName("invitacion_id") val invitationId: String,
    val membresia: MembershipDto,
    @SerialName("aceptada_en") val acceptedAt: String,
)

@Serializable
private data class MembershipDto(
    val id: String,
    @SerialName("establecimiento_id") val establishmentId: String,
    val rol: String,
    val activo: Boolean,
)

@Serializable
private data class SessionAccessDto(
    @SerialName("membresia_id") val membresiaId: String,
    val establecimiento: EstablishmentAccessDto,
    val rol: String,
    @SerialName("identificador_cliente") val clientIdentifier: String? = null,
    @SerialName("estado_establecimiento") val establishmentStatus: String,
    @SerialName("cierre_operativo_disponible") val operationalCloseAvailable: Boolean,
)

@Serializable
private data class EstablishmentAccessDto(
    val id: String,
    val nombre: String,
    val slug: String,
)
