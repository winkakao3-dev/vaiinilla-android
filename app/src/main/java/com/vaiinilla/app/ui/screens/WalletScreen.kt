package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import com.vaiinilla.app.ui.wallet.WalletRemoteUiState

/**
 * Wallet surface backed by the Entrega 03 contract. The API exposes only the
 * visible balance; internal commission buckets stay server-side.
 */
@Composable
fun WalletScreen(
    remoteState: WalletRemoteUiState = WalletRemoteUiState(),
    onRetry: () -> Unit = {},
    onMenu: () -> Unit,
    onAssistant: () -> Unit,
    onOrders: () -> Unit,
    onCart: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding(),
        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = VaiinillaBottomNavClearance + 48.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Cartera", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.accent,
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = colors.accentInk,
                    )
                    when {
                        remoteState.loading -> {
                            CircularProgressIndicator(
                                color = colors.accentInk,
                                modifier = Modifier.padding(top = 18.dp),
                            )
                        }
                        remoteState.data != null -> {
                            Text(
                                "$${remoteState.data.wallet.visibleBalance}",
                                color = colors.accentInk,
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                modifier = Modifier.padding(top = 14.dp),
                            )
                            Text(
                                "Saldo disponible en este establecimiento",
                                color = colors.accentInk.copy(alpha = 0.72f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        else -> {
                            Text(
                                remoteState.error ?: "No se pudo consultar el saldo.",
                                color = colors.accentInk,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(top = 14.dp),
                            )
                            Button(
                                onClick = onRetry,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = colors.accentInk,
                                        contentColor = colors.accent,
                                    ),
                                modifier = Modifier.padding(top = 12.dp),
                            ) {
                                Text("Reintentar", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.paper2,
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Saldo y movimientos", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(
                        "Las recargas se hacen en Caja y el saldo se consume al confirmar el pedido.",
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Button(
                        onClick = onCart,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.ink,
                                contentColor = colors.paper,
                            ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(top = 14.dp),
                    ) {
                        Text("Ver menú y pagar con saldo", fontWeight = FontWeight.Black)
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        remoteState.data?.movements?.takeIf { it.isNotEmpty() }?.let { movements ->
            item {
                Text("Últimos movimientos", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
            items(movements.take(8), key = { it.id }) { movement ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.paper2,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                movementLabel(movement.type, movement.description),
                                color = colors.ink,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                            )
                            Text(
                                "Saldo después: $${movement.balanceAfter}",
                                color = colors.muted,
                                fontSize = 12.sp,
                            )
                        }
                        Text(
                            "$${movement.amount}",
                            color = if (movement.amount.startsWith("-")) colors.ink else colors.accent,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }

        item {
            Text("Atajos", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        item {
            ShortcutRow(
                icon = Icons.Outlined.MenuBook,
                title = "Abrir menú",
                subtitle = "Consulta el catálogo del establecimiento",
                onClick = onMenu,
            )
        }
        item {
            ShortcutRow(
                icon = Icons.Outlined.ReceiptLong,
                title = "Mis pedidos",
                subtitle = "Consulta el estado de tus pedidos reales",
                onClick = onOrders,
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

private fun movementLabel(
    type: String,
    description: String,
): String =
    when (type) {
        "recarga_efectivo" -> "Recarga en Caja"
        "compra" -> "Compra"
        "cashback" -> "Cashback"
        "cancelacion" -> "Cancelación"
        "ajuste" -> "Ajuste autorizado"
        else -> description
    }

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = colors.paper2,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = colors.ink)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(subtitle, color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = colors.muted)
        }
    }
}

@Preview(name = "Cartera · claro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletScreenLightPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        WalletScreen(onMenu = {}, onAssistant = {}, onOrders = {}, onCart = {})
    }
}

@Preview(name = "Cartera · oscuro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        WalletScreen(onMenu = {}, onAssistant = {}, onOrders = {}, onCart = {})
    }
}
