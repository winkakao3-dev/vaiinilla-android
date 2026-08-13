package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.vaiinilla.app.ui.components.WalletUnavailableScreen
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import com.vaiinilla.app.ui.wallet.WalletUiState

@Composable
fun WalletAddMoneyScreen(
    walletState: WalletUiState,
    initialMethod: String = "card",
    onBack: () -> Unit,
    onCreditBalance: (Int) -> Unit,
) {
    WalletUnavailableScreen(
        title = "Añadir dinero",
        onBack = onBack,
        description =
            "La recarga de Entrega 03 es efectivo en Caja, no tarjeta ni SPEI. Cuando el backend publique esas rutas, Caja acreditará el saldo de esta cafetería.",
    )
}

@Preview(name = "Añadir dinero · claro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletAddMoneyScreenLightPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        WalletAddMoneyScreen(walletState = WalletUiState(), onBack = {}, onCreditBalance = {})
    }
}

@Preview(name = "Añadir dinero · oscuro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletAddMoneyScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        WalletAddMoneyScreen(walletState = WalletUiState(), onBack = {}, onCreditBalance = {})
    }
}
