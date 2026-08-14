package com.vaiinilla.app.ui.theme

import android.content.Context

object ThemePreferences {
    private const val PREFS_NAME = "vaiinilla_theme_prefs"
    private const val KEY_THEME = "vaiinilla_theme"

    fun load(context: Context): VaiinillaThemeMode {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return VaiinillaThemeMode.fromStorageKey(prefs.getString(KEY_THEME, VaiinillaThemeMode.System.storageKey))
    }

    fun save(
        context: Context,
        mode: VaiinillaThemeMode,
    ) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode.storageKey)
            .apply()
    }
}
