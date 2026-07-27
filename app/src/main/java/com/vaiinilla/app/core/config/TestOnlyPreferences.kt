package com.vaiinilla.app.core.config

import android.content.Context

object TestOnlyPreferences {
    private const val PREFS_NAME = "vaiinilla_runtime"
    private const val KEY_TEST_ONLY = "solo_pruebas"

    fun isEnabled(context: Context): Boolean =
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TEST_ONLY, false)

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TEST_ONLY, enabled)
            .apply()
    }
}
