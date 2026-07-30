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

        var enrollmentComplete: Boolean
            get() = prefs.getBoolean(KEY_ENROLLMENT_COMPLETE, false)
            set(value) {
                prefs.edit().putBoolean(KEY_ENROLLMENT_COMPLETE, value).apply()
            }

        fun clear() {
            prefs.edit().clear().apply()
        }

        private companion object {
            const val PREFS = "vaiinilla_student_auth_ux"
            const val KEY_ENROLLMENT_COMPLETE = "enrollment_complete"
        }
    }
