package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.vaiinilla.app.ui.theme.AccentInk
import com.vaiinilla.app.ui.theme.AmberInkLight
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.CreamDeep
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.Line
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.MutedInk
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import com.vaiinilla.app.ui.theme.Yolk

/**
 * Adaptive operational color tokens supporting Light, Dark, and pure-pitch AMOLED.
 */
data class OperationalColors(
    val background: Color,
    val surfacePaper: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val cardInner: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentLime: Color,
    val accentInk: Color,
    val pillBackground: Color,
    val pillBorder: Color,
    val buttonSecondary: Color,
    val buttonSecondaryInk: Color,
    val highlightBorder: Color,
    val isDark: Boolean,
)

/** Texto de aviso ámbar: [Yolk] solo se lee sobre las tarjetas oscuras. */
internal val OperationalColors.amberInk: Color get() = if (isDark) Yolk else AmberInkLight

@Composable
fun rememberOperationalColors(): OperationalColors {
    val themeMode = LocalVaiinillaThemeMode.current
    val isSystemDark = isSystemInDarkTheme()
    val effectiveMode = themeMode.resolveEffectiveMode(isSystemDark)

    return when (effectiveMode) {
        VaiinillaThemeMode.Amoled ->
            OperationalColors(
                background = Color(0xFF000000),
                surfacePaper = Color(0xFF070806),
                cardBackground = Color(0xFF0D0E0C),
                cardBorder = Color(0xFF242620),
                cardInner = Color(0xFF141512),
                textPrimary = Color(0xFFFAF7F0),
                textSecondary = Color(0xFFA5A79E),
                textMuted = Color(0xFF85887E),
                accentLime = Color(0xFFB7DE63),
                accentInk = Color(0xFF000000),
                pillBackground = Color(0xFF181A16),
                pillBorder = Color(0xFF2C2F27),
                buttonSecondary = Color(0xFF1E201B),
                buttonSecondaryInk = Color(0xFFFAF7F0),
                highlightBorder = Color(0xFFB7DE63),
                isDark = true,
            )
        VaiinillaThemeMode.Dark ->
            OperationalColors(
                background = Color(0xFF1D1E1C),
                surfacePaper = Color(0xFF242622),
                cardBackground = Color(0xFF282A25),
                cardBorder = Color(0xFF3C3E37),
                cardInner = Color(0xFF32342E),
                textPrimary = Color(0xFFF7F3E7),
                textSecondary = Color(0xFFB2B4AA),
                textMuted = Color(0xFF9A9C92),
                accentLime = Color(0xFFB7DE63),
                accentInk = Color(0xFF171816),
                pillBackground = Color(0xFF2E312A),
                pillBorder = Color(0xFF45483E),
                buttonSecondary = Color(0xFF363931),
                buttonSecondaryInk = Color(0xFFF7F3E7),
                highlightBorder = Color(0xFFB7DE63),
                isDark = true,
            )
        else ->
            OperationalColors(
                background = Cream,
                surfacePaper = Cream,
                cardBackground = CreamDeep,
                cardBorder = Line,
                cardInner = Color(0xFFFDFBF4),
                textPrimary = Ink,
                textSecondary = MutedInk,
                textMuted = Color(0xFF65675F),
                accentLime = Lime,
                accentInk = AccentInk,
                pillBackground = Color(0xFFE8E3D2),
                pillBorder = Line,
                buttonSecondary = Color(0xFFE4DFCE),
                buttonSecondaryInk = Ink,
                highlightBorder = Color(0xFF96C83F),
                isDark = false,
            )
    }
}
