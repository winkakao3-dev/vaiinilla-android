package com.vaiinilla.app.data.mode

import android.content.Context
import androidx.core.content.edit
import com.vaiinilla.app.core.network.ApiJson
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.OperationalRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * O04: los últimos modos de la cuenta y el contexto del último modo de personal que se activó, para abrir Mesero o
 * Caja si la app arranca sin internet (la cola sin conexión guarda los pedidos). No guarda el token: ese vive en el
 * almacén seguro y, al volver la red, el modo se activa de nuevo con el servidor.
 */
@Singleton
class StaffOfflineContextStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun saveModes(
            uid: String,
            modes: List<AuthorizedMode>,
        ) {
            val stored = read()?.takeIf { it.uid == uid }
            write(StoredDto(uid = uid, modes = modes.map(ModeDto::of), context = stored?.context))
        }

        fun saveContext(
            uid: String,
            context: AuthorizedModeContext,
        ) {
            val stored = read()?.takeIf { it.uid == uid }
            write(StoredDto(uid = uid, modes = stored?.modes.orEmpty(), context = ContextDto.of(context)))
        }

        /** Lo guardado para [uid]; null si es de otra cuenta o no se entiende. */
        fun load(uid: String): StaffOfflineSnapshot? {
            val stored = read()?.takeIf { it.uid == uid } ?: return null
            return runCatching {
                StaffOfflineSnapshot(
                    modes = stored.modes.mapNotNull { it.toDomain() },
                    context = stored.context?.toDomain(),
                )
            }.getOrNull()
        }

        fun clear() {
            prefs.edit { clear() }
        }

        private fun read(): StoredDto? =
            prefs.getString(KEY, null)?.let { raw ->
                runCatching { json.decodeFromString(StoredDto.serializer(), raw) }.getOrNull()
            }

        private fun write(stored: StoredDto) {
            prefs.edit { putString(KEY, json.encodeToString(StoredDto.serializer(), stored)) }
        }

        @Serializable
        private data class StoredDto(
            val uid: String,
            val modes: List<ModeDto> = emptyList(),
            val context: ContextDto? = null,
        )

        @Serializable
        private data class ModeDto(
            val role: String,
            val establishmentId: String,
            val establishmentName: String,
            val membershipId: String,
            val clientIdentifier: String? = null,
            val establishmentSlug: String = "",
            val isSchool: Boolean = false,
            val establishmentActive: Boolean = true,
        ) {
            fun toDomain(): AuthorizedMode? =
                OperationalRole.entries.firstOrNull { it.name == role }?.let {
                    AuthorizedMode(
                        role = it,
                        establishmentId = establishmentId,
                        establishmentName = establishmentName,
                        membershipId = membershipId,
                        clientIdentifier = clientIdentifier,
                        establishmentSlug = establishmentSlug,
                        isSchool = isSchool,
                        establishmentActive = establishmentActive,
                    )
                }

            companion object {
                fun of(mode: AuthorizedMode) =
                    ModeDto(
                        role = mode.role.name,
                        establishmentId = mode.establishmentId,
                        establishmentName = mode.establishmentName,
                        membershipId = mode.membershipId,
                        clientIdentifier = mode.clientIdentifier,
                        establishmentSlug = mode.establishmentSlug,
                        isSchool = mode.isSchool,
                        establishmentActive = mode.establishmentActive,
                    )
            }
        }

        @Serializable
        private data class ContextDto(
            val role: String,
            val establishmentId: String,
            val establishmentName: String,
            val membershipId: String,
            val restrictedMode: String? = null,
        ) {
            fun toDomain(): StaffOfflineContext? {
                val operationalRole = OperationalRole.entries.firstOrNull { it.name == role } ?: return null
                return StaffOfflineContext(
                    role = operationalRole,
                    establishmentId = establishmentId,
                    establishmentName = establishmentName,
                    membershipId = membershipId,
                    restrictedMode = runCatching { RestrictedMode.fromWireValue(restrictedMode) }.getOrNull(),
                )
            }

            companion object {
                fun of(context: AuthorizedModeContext) =
                    ContextDto(
                        role = context.role.name,
                        establishmentId = context.establishmentId,
                        establishmentName = context.establishmentName,
                        membershipId = context.membershipId,
                        restrictedMode = context.restrictedMode?.wireValue,
                    )
            }
        }

        private companion object {
            const val PREFS = "vaiinilla_staff_offline_context"
            const val KEY = "snapshot"
            val json = ApiJson.plain
        }
    }

/** Lo que se guardó del contexto de un modo de personal, sin el token. */
data class StaffOfflineContext(
    val role: OperationalRole,
    val establishmentId: String,
    val establishmentName: String,
    val membershipId: String,
    val restrictedMode: RestrictedMode? = null,
) {
    fun matches(mode: AuthorizedMode): Boolean =
        mode.role == role && mode.establishmentId == establishmentId && mode.membershipId == membershipId

    fun toModeContext(accessToken: String): AuthorizedModeContext =
        AuthorizedModeContext(
            role = role,
            establishmentId = establishmentId,
            establishmentName = establishmentName,
            membershipId = membershipId,
            accessToken = accessToken,
            // Vencido a propósito: al volver la red se pide uno nuevo antes de lo demás.
            expiresIn = 0,
            restrictedMode = restrictedMode,
        )
}

data class StaffOfflineSnapshot(
    val modes: List<AuthorizedMode>,
    val context: StaffOfflineContext?,
)

/** Solo Mesero y Caja trabajan sin internet: sus pedidos se guardan en el teléfono. */
internal fun canWorkOffline(role: OperationalRole): Boolean =
    role == OperationalRole.WAITER || role == OperationalRole.CASHIER
