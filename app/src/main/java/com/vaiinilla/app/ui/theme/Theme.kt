package com.vaiinilla.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private fun VaiinillaThemeMode.toMaterialColorScheme() = when (this) {
    VaiinillaThemeMode.Light -> lightColorScheme(
        primary = Lime,
        onPrimary = AccentInk,
        primaryContainer = LimeSoft,
        onPrimaryContainer = Ink,
        secondary = Coral,
        background = Cream,
        onBackground = Ink,
        surface = CreamDeep,
        onSurface = Ink,
        surfaceVariant = CreamDeep,
        onSurfaceVariant = MutedInk,
    )
    VaiinillaThemeMode.Dark -> darkColorScheme(
        primary = Lime,
        onPrimary = DarkAccentInk,
        primaryContainer = LimeSoft,
        onPrimaryContainer = DarkInk,
        secondary = Coral,
        background = DarkPaper,
        onBackground = DarkInk,
        surface = DarkPaper2,
        onSurface = DarkInk,
        surfaceVariant = DarkPaper2,
        onSurfaceVariant = DarkMuted,
    )
    VaiinillaThemeMode.Amoled -> darkColorScheme(
        primary = Lime,
        onPrimary = AmoledAccentInk,
        primaryContainer = LimeSoft,
        onPrimaryContainer = AmoledInk,
        secondary = Coral,
        background = AmoledPaper,
        onBackground = AmoledInk,
        surface = AmoledPaper2,
        onSurface = AmoledInk,
        surfaceVariant = AmoledPaper2,
        onSurfaceVariant = AmoledMuted,
    )
}

@Composable
fun VaiinillaTheme(
    themeMode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
    onThemeModeChange: ((VaiinillaThemeMode) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = themeMode.resolveColors()
    CompositionLocalProvider(
        LocalVaiinillaColors provides colors,
        LocalVaiinillaThemeMode provides themeMode,
        LocalVaiinillaThemeModeChanger provides onThemeModeChange,
    ) {
        MaterialTheme(
            colorScheme = themeMode.toMaterialColorScheme(),
            typography = VaiinillaTypography,
            shapes = Shapes(
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(34.dp),
            ),
            content = content,
        )
    }
}
