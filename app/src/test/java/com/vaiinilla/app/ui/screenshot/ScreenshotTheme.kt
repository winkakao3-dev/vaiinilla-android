package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun ScreenshotTheme(content: @Composable () -> Unit) {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light, content = content)
}
