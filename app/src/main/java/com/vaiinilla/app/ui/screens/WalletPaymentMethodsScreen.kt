package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.vaiinilla.app.ui.components.WalletUnavailableScreen
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import com.vaiinilla.app.ui.wallet.WalletUiState

@Composable
fun WalletPaymentMethodsScreen(
    walletState: WalletUiState,
    onBack: () -> Unit,
    onAddCard: () -> Unit,
) {
    WalletUnavailableScreen(
        title = "Métodos de pago",
        onBack = onBack,
        description =
            "El backend vigente todavía no expone tarjetas, transferencias ni otros métodos digitales. El checkout real disponible es efectivo.",
    )
}

@Preview(name = "Métodos de pago · claro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletPaymentMethodsScreenLightPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        WalletPaymentMethodsScreen(walletState = WalletUiState(), onBack = {}, onAddCard = {})
    }
}

@Preview(name = "Métodos de pago · oscuro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletPaymentMethodsScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        WalletPaymentMethodsScreen(walletState = WalletUiState(), onBack = {}, onAddCard = {})
    }
}
