package com.vaiinilla.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class VaiinillaColors(
    val paper: Color,
    val paper2: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val accent2: Color,
    val accentInk: Color,
    val coral: Color,
    val yolk: Color,
    val navGlass: Color,
    val navBorder: Color,
    val navInsetHighlight: Color,
    val navShadow: Color,
    val navPill: Color,
    val navTextIdle: Color,
    val navTextActive: Color,
)

val LocalVaiinillaColors = staticCompositionLocalOf {
    VaiinillaThemeMode.Light.resolveColors()
}

val LocalVaiinillaThemeMode = staticCompositionLocalOf { VaiinillaThemeMode.Light }

val LocalVaiinillaThemeModeChanger = compositionLocalOf<((VaiinillaThemeMode) -> Unit)?> { null }

@Composable
fun PreviewVaiinillaColors(
    mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalVaiinillaColors provides mode.resolveColors()) {
        content()
    }
}
