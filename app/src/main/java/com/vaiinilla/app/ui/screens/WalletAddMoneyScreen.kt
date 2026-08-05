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
            "El backend vigente todavía no ofrece saldo, transferencias ni una pasarela de pago. El checkout disponible usa efectivo.",
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
