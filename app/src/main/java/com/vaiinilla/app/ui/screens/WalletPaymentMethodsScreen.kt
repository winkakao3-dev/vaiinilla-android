package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.WalletPrimaryButton
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSectionHead
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.wallet.SavedCard
import com.vaiinilla.app.ui.wallet.WalletUiState

@Composable
fun WalletPaymentMethodsScreen(
    walletState: WalletUiState,
    onBack: () -> Unit,
    onAddCard: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current

    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
        ) {
            WalletSubflowTopBar(title = "Métodos de pago", onBack = onBack)

            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
            ) {
                Text(
                    "La tarjeta puede pagar un pedido directamente o añadir dinero. La transferencia sólo recarga el saldo.",
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                WalletSectionHead(
                    title = "Tarjetas",
                    action = "Agregar",
                    onAction = onAddCard,
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.paper2,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        walletState.cards.forEachIndexed { index, card ->
                            if (index > 0) Spacer(Modifier.height(7.dp))
                            SavedCardRow(card = card, selected = index == 0)
                        }
                    }
                }

                WalletPrimaryButton(
                    text = "Agregar método de pago",
                    onClick = onAddCard,
                    modifier = Modifier.padding(top = 12.dp),
                )

                WalletSectionHead(title = "Transferencia")

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.paper2,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(42.dp)
                                        .background(colors.accent, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("SPEI", color = colors.accentInk, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    "Transferencia bancaria",
                                    color = colors.ink,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    "CLABE 646180157034852019 · Ref UTCH241087",
                                    color = colors.muted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                    }
                }

                Text(
                    "La tarjeta puede pagar un pedido directamente o añadir dinero. La transferencia sólo recarga el saldo.",
                    color = colors.muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun SavedCardRow(
    card: SavedCard,
    selected: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .background(colors.ink, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(card.brand.take(4), color = colors.paper, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("•••• ${card.lastFour}", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(
                    "${card.holder} · vence ${card.expiry}",
                    color = colors.muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (selected) {
                Box(
                    modifier =
                        Modifier
                            .size(22.dp)
                            .background(colors.accent, RoundedCornerShape(99.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = colors.accentInk, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }
    }
}
