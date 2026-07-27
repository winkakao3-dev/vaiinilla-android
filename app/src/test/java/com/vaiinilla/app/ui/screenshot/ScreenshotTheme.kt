package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun ScreenshotTheme(
    mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
    content: @Composable () -> Unit,
) {
    VaiinillaTheme(themeMode = mode, content = content)
}
