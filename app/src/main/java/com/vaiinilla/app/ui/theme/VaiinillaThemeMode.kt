package com.vaiinilla.app.ui.theme

enum class VaiinillaThemeMode(
    val storageKey: String,
) {
    Light("light"),
    Dark("dark"),
    Amoled("amoled"),
    ;

    fun next(): VaiinillaThemeMode =
        when (this) {
            Light -> Dark
            Dark -> Amoled
            Amoled -> Light
        }

    fun resolveColors(): VaiinillaColors =
        when (this) {
            Light ->
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
                    navGlass = NavGlass,
                    navBorder = NavBorder,
                    navInsetHighlight = NavInsetHighlight,
                    navShadow = NavShadow,
                    navPill = NavPill,
                    navTextIdle = NavTextIdle,
                    navTextActive = NavTextActive,
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
                    navGlass = NavGlass,
                    navBorder = NavBorder,
                    navInsetHighlight = NavInsetHighlight,
                    navShadow = NavShadow,
                    navPill = NavPill,
                    navTextIdle = NavTextIdle,
                    navTextActive = NavTextActive,
                )
        }

    companion object {
        fun fromStorageKey(key: String?): VaiinillaThemeMode = entries.firstOrNull { it.storageKey == key } ?: Light
    }
}
