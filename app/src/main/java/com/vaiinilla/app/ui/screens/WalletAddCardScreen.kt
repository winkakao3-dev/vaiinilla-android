package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.vaiinilla.app.ui.components.WalletUnavailableScreen
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import com.vaiinilla.app.ui.wallet.WalletUiState

@Composable
fun WalletAddCardScreen(
    walletState: WalletUiState,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    WalletUnavailableScreen(
        title = "Agregar tarjeta",
        onBack = onBack,
        description =
            "El contrato backend vigente todavía no integra una pasarela de tarjetas. No se solicitarán ni almacenarán datos bancarios aquí.",
    )
}

@Preview(name = "Agregar tarjeta · claro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletAddCardScreenLightPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        WalletAddCardScreen(walletState = WalletUiState(), onBack = {}, onSaved = {})
    }
}

@Preview(name = "Agregar tarjeta · oscuro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletAddCardScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        WalletAddCardScreen(walletState = WalletUiState(), onBack = {}, onSaved = {})
    }
}
