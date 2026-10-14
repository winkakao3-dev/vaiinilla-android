package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.core.qr.QrCodeEncoder
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import com.vaiinilla.app.ui.wallet.WalletRemoteUiState
import kotlinx.coroutines.delay

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
    /** Avisa cuando el QR personal abre o cierra, para quitar la barra de abajo mientras tanto. */
    onCodeOpenChange: (Boolean) -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val haptics = LocalHapticFeedback.current
    val reduceMotion = reducedMotion()
    val qrValue = userId?.trim()?.takeIf { it.isNotEmpty() }?.let(QrPayloadParser::encodeUser)
    val matrix = remember(qrValue) { qrValue?.let { runCatching { QrCodeEncoder.encode(it) }.getOrNull() } }
    // El QR personal está abierto sobre la casita: el frasco se vuelve el código.
    var codeOpen by rememberSaveable { mutableStateOf(false) }
    // El saldo de antes de una recarga, mientras el QR se deshace de vuelta en el frasco.
    var heldBalance by remember { mutableStateOf<String?>(null) }
    val balance = remoteState.data?.wallet?.visibleBalance
    val shownBalance = heldBalance ?: balance
    val latestOnCodeOpenChange by rememberUpdatedState(onCodeOpenChange)
    LaunchedEffect(codeOpen) { latestOnCodeOpenChange(codeOpen) }
    DisposableEffect(Unit) { onDispose { latestOnCodeOpenChange(false) } }
    // Mientras el código está abierto se pregunta por la recarga; al llegar, el código se cierra solo.
    val latestOnRetry by rememberUpdatedState(onRetry)
    LaunchedEffect(codeOpen) {
        while (codeOpen) {
            delay(CODE_POLL_MS)
            latestOnRetry()
        }
    }
    var lastBalance by remember { mutableStateOf(balance) }
    LaunchedEffect(balance) {
        val before = lastBalance?.walletAmount()
        val after = balance?.walletAmount()
        val previous = lastBalance
        if (balance != null) lastBalance = balance
        if (codeOpen && before != null && after != null && after > before) {
            // Caja registró la recarga: el código regresa al frasco y luego caen las monedas.
            heldBalance = previous
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            codeOpen = false
            delay(if (reduceMotion) 250 else 1700)
            heldBalance = null
        }
    }
    val sheetAway by animateFloatAsState(
        targetValue = if (codeOpen) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.9f, stiffness = 130f),
        label = "wallet_sheet_away",
    )

    // Vainiverso: la casita de Vaini con su frasco de tesoros arriba; la cartera en vidrio encima.
    val palette = scenePaletteForTheme()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val glassColors =
        colors.copy(
            paper = Color.Transparent,
            solidPaper = colors.onInk,
            paper2 = palette.glassCard,
            line = palette.glassLine,
            ink = palette.ink,
            muted = palette.muted,
        )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowPx = constraints.maxHeight * WALLET_SCENE_WINDOW
        val windowDp = with(density) { windowPx.toDp() }
        val sheetTop = {
            if (listState.firstVisibleItemIndex == 0) windowPx - listState.firstVisibleItemScrollOffset else 0f
        }
        CompositionLocalProvider(LocalScene provides palette) {
            WalletJarBackdrop(sheetTop = sheetTop, balance = shownBalance)
        }
        CompositionLocalProvider(LocalVaiinillaColors provides glassColors) {
            LazyColumn(
                state = listState,
                // La hoja baja mientras la cámara vuela al frasco, y regresa al cerrar el código.
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = size.height * 0.7f * sheetAway
                            alpha = 1f - sheetAway
                        },
                userScrollEnabled = !codeOpen,
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = VaiinillaBottomNavClearance + 48.dp,
                    ),
            ) {
                item(key = "home-window") { Spacer(Modifier.height(windowDp + 20.dp)) }
                item {
                    WalletBalanceHero(
                        remoteState = remoteState,
                        shownBalance = shownBalance,
                        onRetry = onRetry,
                        onRecharge = {
                            if (matrix != null && !codeOpen) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                codeOpen = true
                            }
                        },
                    )
                }

                val movements = remoteState.data?.movements
                if (movements != null) {
                    item {
                        Spacer(Modifier.height(30.dp))
                        Text(
                            text = "Movimientos recientes",
                            color = colors.ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (movements.isEmpty()) {
                        item {
                            Text(
                                text = "Aquí verás tus recargas y lo que pagues con saldo.",
                                color = colors.muted,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            TextButton(onClick = onMenu, contentPadding = PaddingValues(0.dp)) {
                                Text("Ver el menú", color = colors.ink, fontWeight = FontWeight.Black)
                            }
                        }
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

                item { Spacer(Modifier.height(4.dp)) }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = if (sheetTop() < size.height) 1f else 0f }
                .background(palette.stage.copy(alpha = 0.92f)),
        )
        if (matrix != null) {
            CompositionLocalProvider(LocalScene provides palette) {
                WalletCodeOverlay(
                    matrix = matrix,
                    shown = codeOpen,
                    fill = jarFillLevel(shownBalance),
                    label = shownBalance?.let { "$$it" },
                    onClose = { codeOpen = false },
                )
            }
        }
    }
}

private const val CODE_POLL_MS = 4_000L

private fun String.walletAmount(): Double? = trim().replace(",", "").toDoubleOrNull()

private const val WALLET_SCENE_WINDOW = 0.4f

@Composable
private fun WalletBalanceHero(
    remoteState: WalletRemoteUiState,
    shownBalance: String?,
    onRetry: () -> Unit,
    onRecharge: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        if (remoteState.data != null || remoteState.loading) {
            Text(
                text = "Saldo Vaiinilla",
                color = colors.muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        when {
            remoteState.data != null -> {
                AnimatedContent(
                    targetState = shownBalance ?: remoteState.data.wallet.visibleBalance,
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
                        text = moneyLabel(balance),
                        color = colors.ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 52.sp,
                        lineHeight = 58.sp,
                        letterSpacing = (-1.6).sp,
                    )
                }
                SeaGlassButton(
                    tone = SeaGlassTone.lima,
                    onClick = onRecharge,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(54.dp),
                ) {
                    Icon(Icons.Outlined.QrCode2, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        "Recargar en caja",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            remoteState.loading -> {
                Spacer(Modifier.height(8.dp))
                SkeletonBlock(
                    modifier = Modifier.size(width = 170.dp, height = 50.dp),
                    corner = 14.dp,
                )
                Spacer(Modifier.height(18.dp))
                SkeletonBlock(
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    corner = 27.dp,
                )
            }

            else -> {
                // El error del servidor habla de "contexto operativo"; aquí se dice qué pasó y qué hacer.
                Text(
                    text = "No pudimos cargar tu saldo",
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = "Revisa tu conexión o vuelve a entrar al lugar desde el menú.",
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                    Text("Reintentar", color = colors.ink, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/** El código personal en grande, para que caja lo escanee al recargar. */
@Composable
internal fun WalletQrDialog(
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
                    GlassCircleButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        iconSize = 18.dp,
                    )
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
                        .background(colors.paper2, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.ink,
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
                    text = "Saldo después: ${moneyLabel(balanceAfter)}",
                    color = colors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            Text(
                text = if (isDebit) "−${moneyLabel(amount.removePrefix("-"))}" else "+${moneyLabel(amount)}",
                color = colors.ink,
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
        WalletScreen(onMenu = {})
    }
}

@Preview(name = "Cartera · oscuro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        WalletScreen(onMenu = {})
    }
}
