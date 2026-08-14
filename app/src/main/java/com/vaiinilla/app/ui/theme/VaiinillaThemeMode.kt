package com.vaiinilla.app.ui.theme

enum class VaiinillaThemeMode(
    val storageKey: String,
    val label: String,
) {
    System("system", "Sistema"),
    Light("light", "Claro"),
    Dark("dark", "Oscuro"),
    Amoled("amoled", "Amoled"),
    ;

    fun next(): VaiinillaThemeMode =
        when (this) {
            System -> Light
            Light -> Dark
            Dark -> Amoled
            Amoled -> System
        }

    fun resolveEffectiveMode(isSystemDark: Boolean): VaiinillaThemeMode =
        when (this) {
            System -> if (isSystemDark) Dark else Light
            Light -> Light
            Dark -> Dark
            Amoled -> Amoled
        }

    fun resolveColors(isSystemDark: Boolean = false): VaiinillaColors =
        when (resolveEffectiveMode(isSystemDark)) {
            Light, System ->
                VaiinillaColors(
                    paper = Cream,
                    paper2 = CreamDeep,
                    ink = Ink,
                    ink2 = Ink2,
                    muted = MutedInk,
                    line = Line,
                    accent = Lime,
                    accent2 = LimeSoft,
                    accentInk = AccentInk,
                    coral = Coral,
                    yolk = Yolk,
                    navGlass = NavGlassLight,
                    navBorder = NavBorderLight,
                    navInsetHighlight = NavInsetHighlight,
                    navShadow = NavShadow,
                    navPill = NavPillLight,
                    navTextIdle = NavTextIdleLight,
                    navTextActive = NavTextActiveLight,
                    isDark = false,
                )
            Dark ->
                VaiinillaColors(
                    paper = DarkPaper,
                    paper2 = DarkPaper2,
                    ink = DarkInk,
                    ink2 = DarkInk2,
                    muted = DarkMuted,
                    line = DarkLine,
                    accent = Lime,
                    accent2 = LimeSoft,
                    accentInk = DarkAccentInk,
                    coral = Coral,
                    yolk = Yolk,
                    navGlass = NavGlass,
                    navBorder = NavBorder,
                    navInsetHighlight = NavInsetHighlight,
                    navShadow = NavShadow,
                    navPill = NavPill,
                    navTextIdle = NavTextIdle,
                    navTextActive = NavTextActive,
                    isDark = true,
                )
            Amoled ->
                VaiinillaColors(
                    paper = AmoledPaper,
                    paper2 = AmoledPaper2,
                    ink = AmoledInk,
                    ink2 = AmoledInk2,
                    muted = AmoledMuted,
                    line = AmoledLine,
                    accent = Lime,
                    accent2 = LimeSoft,
                    accentInk = AmoledAccentInk,
                    coral = Coral,
                    yolk = Yolk,
                    navGlass = NavGlassAmoled,
                    navBorder = NavBorder,
                    navInsetHighlight = NavInsetHighlight,
                    navShadow = NavShadow,
                    navPill = NavPillAmoled,
                    navTextIdle = NavTextIdle,
                    navTextActive = NavTextActive,
                    isDark = true,
                )
        }

    companion object {
        fun fromStorageKey(key: String?): VaiinillaThemeMode = entries.firstOrNull { it.storageKey == key } ?: System
    }
}
