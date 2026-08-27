package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
 * Wallet surface backed by the current contract. The API exposes only the
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
    ) {
        item {
            Text(
                text = "Cartera",
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 23.sp,
            )
        }

        item {
            Spacer(Modifier.height(26.dp))
            WalletBalanceHero(
                remoteState = remoteState,
                onRetry = onRetry,
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WalletAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ShoppingCart,
                    title = "Pagar",
                    subtitle = "Usar mi saldo",
                    onClick = onCart,
                )
                WalletAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Pedidos",
                    subtitle = "Ver actividad",
                    onClick = onOrders,
                )
            }
        }

        remoteState.data?.movements?.takeIf { it.isNotEmpty() }?.let { movements ->
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Movimientos",
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                )
                Spacer(Modifier.height(8.dp))
            }

            items(movements.take(12), key = { it.id }) { movement ->
                WalletMovementRow(
                    type = movement.type,
                    description = movement.description,
                    amount = movement.amount,
                    balanceAfter = movement.balanceAfter,
                )
            }
        }

        item {
            Spacer(Modifier.height(28.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                onClick = onMenu,
                color = colors.paper2,
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(44.dp)
                                .background(colors.paper, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp),
                    ) {
                        Text(
                            text = "Abrir menú",
                            color = colors.ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = "Usa tu saldo en tu siguiente pedido",
                            color = colors.muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = colors.muted,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun WalletBalanceHero(
    remoteState: WalletRemoteUiState,
    onRetry: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .background(colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.height(14.dp))

        when {
            remoteState.loading -> {
                CircularProgressIndicator(
                    color = colors.accent,
                    modifier = Modifier.size(32.dp),
                )
            }

            remoteState.data != null -> {
                Text(
                    text = "$${remoteState.data.wallet.visibleBalance}",
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 46.sp,
                    lineHeight = 50.sp,
                    letterSpacing = (-1.4).sp,
                )
                Text(
                    text = "Saldo Vaiinilla",
                    color = colors.muted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            else -> {
                Text(
                    text = remoteState.error ?: "No se pudo consultar el saldo.",
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Button(
                    onClick = onRetry,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.ink,
                            contentColor = colors.paper,
                        ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Reintentar", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun WalletAction(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current

    Surface(
        modifier = modifier,
        onClick = onClick,
        color = colors.paper2,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .background(colors.accent, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accentInk,
                    modifier = Modifier.size(21.dp),
                )
            }
            Text(
                text = title,
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = subtitle,
                color = colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun WalletMovementRow(
    type: String,
    description: String,
    amount: String,
    balanceAfter: String,
) {
    val colors = LocalVaiinillaColors.current
    val isDebit = amount.startsWith("-")
    val icon = if (isDebit) Icons.Outlined.ShoppingCart else Icons.Outlined.AccountBalanceWallet

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .background(
                            if (isDebit) colors.paper2 else colors.accent2,
                            RoundedCornerShape(15.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDebit) colors.ink else colors.accentInk,
                    modifier = Modifier.size(21.dp),
                )
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 13.dp),
            ) {
                Text(
                    text = movementLabel(type, description),
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                )
                Text(
                    text = "Saldo después: \$$balanceAfter",
                    color = colors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            Text(
                text = "\$$amount",
                color = if (isDebit) colors.ink else colors.accent,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
            )
        }

        HorizontalDivider(
            color = colors.line,
            thickness = 1.dp,
            modifier = Modifier.padding(start = 57.dp),
        )
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
