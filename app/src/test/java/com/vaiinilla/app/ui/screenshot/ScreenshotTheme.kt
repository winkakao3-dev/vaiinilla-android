package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vaiinilla.app.core.contract.UnreadableOrders
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun ScreenshotTheme(
    mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
    content: @Composable () -> Unit,
) {
    // Las pruebas de contrato que leen pedidos desconocidos dejan cuentas en este registro global; sin limpiarlo,
    // la franja «Actualiza la app» aparecería en capturas que no tienen nada que ver.
    remember { UnreadableOrders.clear().let { true } }
    VaiinillaTheme(themeMode = mode, content = content)
}
