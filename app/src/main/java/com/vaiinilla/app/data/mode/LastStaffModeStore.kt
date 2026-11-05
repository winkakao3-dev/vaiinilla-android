package com.vaiinilla.app.data.mode

import android.content.Context
import androidx.core.content.edit
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El último modo de personal (Caja, Cocina, Mesero, Administración) que se eligió en este teléfono. El contexto
 * operativo vive en memoria: si Android cierra la app en segundo plano, al volver se reactiva este modo en vez de
 * pedir otra vez elegirlo. Solo guarda a qué membresía volver, no el token.
 */
@Singleton
class LastStaffModeStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun save(
            uid: String,
            mode: AuthorizedMode,
        ) {
            prefs.edit {
                putString(KEY_UID, uid)
                putString(KEY_ROLE, mode.role.name)
                putString(KEY_ESTABLISHMENT_ID, mode.establishmentId)
                putString(KEY_MEMBERSHIP_ID, mode.membershipId)
            }
        }

        /** El modo guardado para [uid], si sigue entre [modes] (el acceso pudo revocarse mientras tanto). */
        fun find(
            uid: String,
            modes: List<AuthorizedMode>,
        ): AuthorizedMode? {
            if (prefs.getString(KEY_UID, null) != uid) return null
            return rememberedAmong(
                modes = modes,
                role = prefs.getString(KEY_ROLE, null),
                establishmentId = prefs.getString(KEY_ESTABLISHMENT_ID, null),
                membershipId = prefs.getString(KEY_MEMBERSHIP_ID, null),
            )
        }

        fun clear() {
            prefs.edit { clear() }
        }

        private companion object {
            const val PREFS = "vaiinilla_last_staff_mode"
            const val KEY_UID = "uid"
            const val KEY_ROLE = "role"
            const val KEY_ESTABLISHMENT_ID = "establishment_id"
            const val KEY_MEMBERSHIP_ID = "membership_id"
        }
    }

/** El modo de personal de [modes] que coincide con lo guardado; nunca el de cliente. */
internal fun rememberedAmong(
    modes: List<AuthorizedMode>,
    role: String?,
    establishmentId: String?,
    membershipId: String?,
): AuthorizedMode? =
    modes.firstOrNull {
        it.role.name == role &&
            it.role != OperationalRole.CLIENT &&
            it.establishmentId == establishmentId &&
            it.membershipId == membershipId &&
            it.establishmentActive
    }
