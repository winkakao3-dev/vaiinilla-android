package com.vaiinilla.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorScheme = lightColorScheme(
    primary = Lime,
    onPrimary = Ink,
    primaryContainer = LimeSoft,
    onPrimaryContainer = Ink,
    secondary = Coral,
    background = Cream,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = CreamDeep,
    onSurfaceVariant = MutedInk,
)

@Composable
fun VaiinillaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
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
