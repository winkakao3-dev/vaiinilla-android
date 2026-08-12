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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Assistant
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

/**
 * Keeps the wallet surface in the student shell without inventing balance,
 * cards, transfers, or activity that the current backend contract does not
 * provide. Ordering remains available through the real cash checkout.
 */
@Composable
fun WalletScreen(
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
                    Text(
                        "Cartera pendiente de conexión",
                        color = colors.accentInk,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        "El backend actual todavía no expone saldo, tarjetas ni transferencias. No mostraremos datos inventados.",
                        color = colors.accentInk.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
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
                    Text("Checkout disponible", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(
                        "Puedes consultar el catálogo y confirmar pedidos con efectivo cuando la caja del establecimiento esté abierta.",
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
                        Text("Ver mi pedido", fontWeight = FontWeight.Black)
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp),
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
        item {
            ShortcutRow(
                icon = Icons.Outlined.Assistant,
                title = "Asistente",
                subtitle = "Revisa opciones del producto y del pedido",
                onClick = onAssistant,
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
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
