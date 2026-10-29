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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.core.qr.QrCodeEncoder
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.domain.model.WalletMovement
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.CremeDivider
import com.vaiinilla.app.ui.components.CremeHero
import com.vaiinilla.app.ui.components.CremePill
import com.vaiinilla.app.ui.components.CremePillRow
import com.vaiinilla.app.ui.components.CremeStat
import com.vaiinilla.app.ui.components.CremeStatRow
import com.vaiinilla.app.ui.components.CremeVenueChip
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.HeroGlassPill
import com.vaiinilla.app.ui.components.ProfileInitialsButton
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.cremeMoment
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import com.vaiinilla.app.ui.parentmode.LocalWalletFamilyUi
import com.vaiinilla.app.ui.parentmode.WalletSchoolEntry
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import com.vaiinilla.app.ui.wallet.WalletRemoteUiState
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Wallet surface backed by the current contract. The API exposes only the
 * visible balance; internal commission buckets stay server-side.
 *
 * Estilo CREME: la portada del lugar, el saldo, las pills (la primera es la acción principal) y la fila de datos.
 * El QR de recarga conserva su animación; solo cambia quién lo abre.
 */
@Composable
fun WalletScreen(
    remoteState: WalletRemoteUiState = WalletRemoteUiState(),
    userId: String? = null,
    /** El saldo es por negocio: el chip de la portada dice de cuál. */
    venueName: String? = null,
    /** Falso cuando el negocio apagó el saldo: se sigue viendo lo que hay, pero ya no se recarga. */
    canRecharge: Boolean = true,
    onRetry: () -> Unit = {},
    onMenu: () -> Unit,
    /** Avisa cuando el QR personal abre o cierra, para quitar la barra de abajo mientras tanto. */
    onCodeOpenChange: (Boolean) -> Unit = {},
    /** "Te cuida Ana": el alumno con tutor ve quién lo cuida (GET /tutelas/mis-controles). */
    caretakerLine: String? = null,
    /** Debajo de quién lo cuida: su límite por día, lo que le queda hoy y lo que no puede pedir (F11). */
    caretakerDetail: String? = null,
    /** Sus controles, para la fila de datos del alumno con tutor. */
    guardianControls: MyGuardianControls? = null,
    /** Mis hijos y lo de tutores y alumnos, solo en una escuela (Modo Padre). */
    familySection: (@Composable () -> Unit)? = null,
    /** La portada del lugar, la misma del menú. */
    venuePhotoUrl: String? = null,
    profileInitials: String = "?",
    onOpenAccount: () -> Unit = {},
    onChangeVenue: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = LocalHapticFeedback.current
    val reduceMotion = reducedMotion()
    val qrValue = userId?.trim()?.takeIf { it.isNotEmpty() }?.let(QrPayloadParser::encodeUser)
    val matrix = remember(qrValue) { qrValue?.let { runCatching { QrCodeEncoder.encode(it) }.getOrNull() } }
    // El QR personal está abierto sobre la cartera: la cartera baja y el frasco se vuelve el código.
    var codeOpen by rememberSaveable { mutableStateOf(false) }
    var visibleMovements by rememberSaveable { mutableIntStateOf(MOVEMENTS_PAGE) }
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
            // Caja registró la recarga: el código regresa al frasco y el saldo nuevo llega al cerrar.
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

    val palette = scenePaletteForTheme()
    val listState = rememberLazyListState()
    val family = LocalWalletFamilyUi.current
    val inSchool = family != null || familySection != null
    val isParent = family?.entry == WalletSchoolEntry.PARENT
    val isStudent = caretakerLine != null && family != null
    val movements = remoteState.data?.movements
    val ledger = remember(movements) { WalletLedgerStats.from(movements.orEmpty()) }
    val statItems =
        when {
            isStudent && guardianControls != null -> studentStats(guardianControls)
            isParent && family != null ->
                listOf(
                    CremeStat("Gastado este mes", ledger.spentThisMonth),
                    CremeStat("Hijos", family.childCount.toString()),
                    CremeStat("Saldo de tus hijos", family.childrenBalance?.let(::compactMoney)),
                )
            else ->
                listOf(
                    CremeStat("Gastado este mes", ledger.spentThisMonth),
                    CremeStat("Recargas", ledger.reloads),
                    CremeStat("Último movimiento", ledger.lastMovement),
                )
        }
    val pills =
        buildList {
            if (canRecharge) {
                add(
                    CremePill(
                        title = "Recargar en caja",
                        icon = Icons.Outlined.QrCode2,
                        testTag = "wallet-recharge",
                        enabled = matrix != null,
                        onClick = {
                            if (matrix != null && !codeOpen) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                codeOpen = true
                            }
                        },
                    ),
                )
            }
            if (isStudent && family != null) {
                add(CremePill("Otro tutor", Icons.Outlined.Tag, family.onLinkCode, "wallet-school-link-code"))
                add(
                    CremePill(
                        "Cómo funciona",
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        family.onStudentHelp,
                        "wallet-school-help",
                    ),
                )
            } else {
                add(CremePill("Cuenta", Icons.Outlined.AccountCircle, onOpenAccount, "wallet-account"))
            }
        }
    val heroGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Box(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        LazyColumn(
            state = listState,
            // La cartera baja mientras la cámara vuela al frasco, y regresa al cerrar el código.
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = size.height * 0.7f * sheetAway
                        alpha = 1f - sheetAway
                    },
            userScrollEnabled = !codeOpen,
            contentPadding = PaddingValues(bottom = VaiinillaBottomNavClearance + 48.dp),
        ) {
            item(key = "hero") {
                WalletHero(
                    remoteState = remoteState,
                    shownBalance = shownBalance,
                    venueName = venueName,
                    venuePhotoUrl = venuePhotoUrl,
                    height = if (inSchool) 320.dp else 400.dp,
                    chip =
                        when {
                            caretakerLine != null -> caretakerLine to Icons.Outlined.Group
                            isParent -> "Tu saldo · el de tus hijos va aparte" to Icons.Outlined.FamilyRestroom
                            !venueName.isNullOrBlank() -> "Vale para comer en $venueName" to null
                            else -> null
                        },
                    profileInitials = profileInitials,
                    onOpenAccount = onOpenAccount,
                    onChangeVenue = onChangeVenue,
                    onRetry = onRetry,
                )
            }
            item(key = "pills") {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    if (remoteState.data == null && (remoteState.loading || remoteState.error == null)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            SkeletonBlock(modifier = Modifier.size(width = 190.dp, height = 54.dp), corner = 27.dp)
                            SkeletonBlock(modifier = Modifier.size(width = 150.dp, height = 54.dp), corner = 27.dp)
                        }
                    } else {
                        CremePillRow(pills)
                    }
                    if (remoteState.data != null && !canRecharge) {
                        Text(
                            text = "Este lugar ya no recibe recargas de saldo.",
                            color = colors.muted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                        )
                    }
                    if (caretakerLine != null && caretakerDetail != null) {
                        Text(
                            text = caretakerDetail.replace('\n', ' '),
                            color = colors.muted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                        )
                    }
                    CremeDivider(Modifier.padding(top = 20.dp))
                    CremeStatRow(statItems, Modifier.padding(top = 18.dp))
                }
            }
            if (family != null) {
                item(key = "family") { Box(Modifier.padding(horizontal = 20.dp)) { family.top() } }
            } else if (familySection != null) {
                item(key = "family") { Box(Modifier.padding(horizontal = 20.dp)) { familySection() } }
            }

            if (movements != null) {
                item(key = "movements-title") {
                    Text(
                        text = "Movimientos recientes",
                        color = colors.ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 4.dp),
                    )
                }
                if (movements.isEmpty()) {
                    item(key = "movements-empty") {
                        Column(Modifier.padding(horizontal = 20.dp)) {
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
                }
                items(movements.take(visibleMovements), key = { it.id }) { movement ->
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        WalletMovementRow(
                            title = movementTitle(movement.type, movement.description, movement.orderFolio),
                            detail = movementDetail(movement.place, movement.productsSummary),
                            amount = movement.amount,
                            balanceAfter = movement.balanceAfter,
                        )
                    }
                }
                if (movements.size > visibleMovements) {
                    item(key = "more-movements") {
                        TextButton(
                            onClick = { visibleMovements += MOVEMENTS_PAGE },
                            modifier = Modifier.padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text("Ver más movimientos", color = colors.ink, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            if (family != null) {
                item(key = "school-ending") { Box(Modifier.padding(horizontal = 20.dp)) { family.ending() } }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = if (heroGone) 1f else 0f }
                .background(colors.paper.copy(alpha = 0.92f)),
        )
        if (matrix != null) {
            CompositionLocalProvider(LocalScene provides palette) {
                WalletCodeOverlay(
                    matrix = matrix,
                    shown = codeOpen,
                    fill = jarFillLevel(shownBalance),
                    label = shownBalance?.let { moneyLabel(it) },
                    onClose = { codeOpen = false },
                )
            }
        }
    }
}

private const val CODE_POLL_MS = 4_000L
private const val MOVEMENTS_PAGE = 12

private fun String.walletAmount(): Double? = trim().replace(",", "").toDoubleOrNull()

/** La fila de datos del alumno con tutor: su límite, lo que le queda hoy y cuánto tiene con candado. */
private fun studentStats(controls: MyGuardianControls): List<CremeStat> {
    val restricted = (controls.blockedProductIds + controls.allergenProductIds + controls.unreviewedProductIds).size
    return listOf(
        CremeStat("Límite al día", controls.dailyLimit?.let(::compactMoney)),
        CremeStat("Te quedan hoy", controls.availableToday?.let(::compactMoney), highlighted = true),
        CremeStat("Con candado", if (restricted == 1) "1 producto" else "$restricted productos"),
    )
}

/**
 * Las tres cifras de la fila de datos de Cartera, sacadas de los movimientos que ya baja la app. Sin movimientos,
 * ninguna: la fila muestra «—».
 */
internal class WalletLedgerStats(
    val spentThisMonth: String?,
    val reloads: String?,
    val lastMovement: String?,
) {
    companion object {
        fun from(
            movements: List<WalletMovement>,
            empty: Boolean = movements.isEmpty(),
            now: Instant = Instant.now(),
        ): WalletLedgerStats {
            if (empty) return WalletLedgerStats(null, null, null)
            val zone = BusinessTime.zone
            val today = BusinessTime.at(now, zone).toLocalDate()
            var spent = BigDecimal.ZERO
            for (movement in movements) {
                if (!movement.amount.startsWith("-")) continue
                val date = movement.createdAt.toInstantOrNull()?.let { BusinessTime.at(it, zone).toLocalDate() }
                val value =
                    movement.amount
                        .replace(",", "")
                        .trim()
                        .toBigDecimalOrNull()
                if (date != null && value != null && date.year == today.year && date.month == today.month) {
                    spent += value.negate()
                }
            }
            val reloads = movements.count { !it.amount.startsWith("-") }
            val last = cremeMoment(movements.first().createdAt, now)
            return WalletLedgerStats(compactMoney(spent), reloads.toString(), last)
        }

        private fun String.toInstantOrNull(): Instant? =
            runCatching { OffsetDateTime.parse(this).toInstant() }.getOrNull()
    }
}

@Composable
private fun WalletHero(
    remoteState: WalletRemoteUiState,
    shownBalance: String?,
    venueName: String?,
    venuePhotoUrl: String?,
    height: Dp,
    chip: Pair<String, ImageVector?>?,
    profileInitials: String,
    onOpenAccount: () -> Unit,
    onChangeVenue: (() -> Unit)?,
    onRetry: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    // Antes de la primera respuesta todavía no hay error: se espera con el esqueleto, no con "No pudimos cargar".
    val waiting = remoteState.data == null && remoteState.error == null
    CremeHero(
        height = height,
        photoUrl = venuePhotoUrl,
        contentDescription = venueName?.let { "Foto de $it" }.orEmpty(),
        leading = {
            if (onChangeVenue != null && !venueName.isNullOrBlank()) {
                HeroGlassPill(
                    label = venueName,
                    icon = Icons.Rounded.Storefront,
                    onClick = onChangeVenue,
                    modifier = Modifier.semantics { contentDescription = "$venueName. Cambiar lugar" },
                )
            }
        },
        trailing = { ProfileInitialsButton(profileInitials, onClick = onOpenAccount) },
    ) {
        if (remoteState.data != null || remoteState.loading || waiting) {
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
                            (slideInVertically { h -> h } + fadeIn()) togetherWith
                                (slideOutVertically { h -> -h } + fadeOut())
                        } else {
                            (slideInVertically { h -> -h } + fadeIn()) togetherWith
                                (slideOutVertically { h -> h } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "wallet_balance_ticker",
                ) { balance ->
                    Text(
                        text = moneyLabel(balance),
                        color = colors.ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 60.sp,
                        lineHeight = 66.sp,
                        letterSpacing = (-1.6).sp,
                        maxLines = 1,
                    )
                }
                if (remoteState.error != null) {
                    Text(
                        text = "Sin conexión: puede que este saldo no sea el más reciente.",
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                chip?.let { (label, icon) ->
                    CremeVenueChip(
                        label = label,
                        icon = icon,
                        initialOf = venueName.orEmpty().ifBlank { label },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            remoteState.loading || waiting -> {
                Spacer(Modifier.height(8.dp))
                SkeletonBlock(modifier = Modifier.size(width = 190.dp, height = 58.dp), corner = 14.dp)
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
    title: String,
    detail: String?,
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
                    text = title,
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                )
                detail?.let {
                    Text(
                        text = it,
                        color = colors.ink,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
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

/** «Compra #12»: el tipo del movimiento con el folio de su pedido, si viene de uno. */
internal fun movementTitle(
    type: String,
    description: String,
    orderFolio: Int?,
): String = movementLabel(type, description) + (orderFolio?.let { " #$it" } ?: "")

/** «Mesa 2 · 2× Molletes, 1× Café americano y 2 más»; null si el movimiento no viene de un pedido. */
internal fun movementDetail(
    place: String?,
    productsSummary: String?,
): String? = listOfNotNull(place, productsSummary).joinToString(" · ").ifBlank { null }

internal fun movementLabel(
    type: String,
    description: String,
): String =
    when (type) {
        "recarga_efectivo" -> "Recarga en Caja"
        "recarga_stripe" -> "Recarga con tarjeta"
        "compra" -> "Compra"
        "cashback" -> "Cashback"
        "cancelacion" -> "Devolución por cancelación"
        "devolucion_articulo" -> "Devolución de un artículo"
        "ajuste" -> description.ifBlank { "Ajuste autorizado" }
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
