package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.discovery.QrPayloadParser
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
    userId: String? = null,
    onRetry: () -> Unit = {},
    onMenu: () -> Unit,
    onAssistant: () -> Unit,
    onOrders: () -> Unit,
    onCart: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    var qrDialogOpen by remember { mutableStateOf(false) }
    val qrValue = userId?.trim()?.takeIf { it.isNotEmpty() }?.let(QrPayloadParser::encodeUser)

    if (qrDialogOpen) {
        WalletQrDialog(
            qrValue = qrValue,
            onDismiss = { qrDialogOpen = false },
        )
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding(),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WalletAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ShoppingCart,
                    title = "Pagar",
                    subtitle = "Usar saldo",
                    onClick = onCart,
                )
                WalletAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Pedidos",
                    subtitle = "Ver actividad",
                    onClick = onOrders,
                )
                WalletAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.QrCode2,
                    title = "Recargar",
                    subtitle = "Mostrar QR",
                    onClick = { qrDialogOpen = true },
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
            remoteState.data != null -> {
                AnimatedContent(
                    targetState = remoteState.data.wallet.visibleBalance,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "wallet_balance_ticker",
                ) { balance ->
                    Text(
                        text = "$$balance",
                        color = colors.ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 46.sp,
                        lineHeight = 50.sp,
                        letterSpacing = (-1.4).sp,
                    )
                }
                Text(
                    text = "Saldo Vaiinilla",
                    color = colors.muted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            remoteState.loading -> {
                CircularProgressIndicator(
                    color = colors.accent,
                    modifier = Modifier.size(32.dp),
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
        modifier = modifier.height(168.dp),
        onClick = onClick,
        color = colors.paper2,
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(colors.accent, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accentInk,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WalletQrDialog(
    qrValue: String?,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            color = colors.paper,
            shape = RoundedCornerShape(30.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Recargar saldo",
                        color = colors.ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    Surface(
                        onClick = onDismiss,
                        color = colors.paper2,
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Cerrar",
                            tint = colors.ink,
                            modifier = Modifier.padding(9.dp).size(20.dp),
                        )
                    }
                }

                Text(
                    text = "Muestra este QR en Caja para encontrar tu cuenta y recargar Saldo Vaiinilla.",
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp, bottom = 20.dp),
                )

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (qrValue != null) {
                            VaiinillaQrCode(
                                value = qrValue,
                                qrSize = 232.dp,
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(232.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Tu código todavía no está disponible.",
                                    color = colors.muted,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp),
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Caja escanea este código. No necesitas escribir tu identificador.",
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
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
