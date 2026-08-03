package com.vaiinilla.app.data.auth.student

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** UX-only flags for student auth. Never used for authorization. */
@Singleton
class StudentAuthPreferences
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        var enrolledEstablishmentId: String?
            get() = prefs.getString(KEY_ENROLLED_ESTABLISHMENT_ID, null)
            set(value) {
                prefs
                    .edit()
                    .apply {
                        if (value.isNullOrBlank()) {
                            remove(KEY_ENROLLED_ESTABLISHMENT_ID)
                        } else {
                            putString(KEY_ENROLLED_ESTABLISHMENT_ID, value)
                        }
                    }.apply()
            }

        val enrollmentComplete: Boolean
            get() = !enrolledEstablishmentId.isNullOrBlank()

        fun markEnrolled(establishmentId: String) {
            require(establishmentId.isNotBlank()) { "establishmentId requerido para enrollment." }
            enrolledEstablishmentId = establishmentId
        }

        fun isEnrolledFor(establishmentId: String?): Boolean {
            val enrolled = enrolledEstablishmentId ?: return false
            if (establishmentId.isNullOrBlank()) return true
            return enrolled == establishmentId
        }

        fun clear() {
            prefs.edit().clear().apply()
        }

        private companion object {
            const val PREFS = "vaiinilla_student_auth_ux"
            const val KEY_ENROLLED_ESTABLISHMENT_ID = "enrolled_establishment_id"
        }
    }
