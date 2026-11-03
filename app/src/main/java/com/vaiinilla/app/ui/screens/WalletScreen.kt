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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.core.qr.QrCodeEncoder
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.domain.model.WalletMovement
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProfileInitialsButton
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.cremeMoment
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import com.vaiinilla.app.ui.parentmode.LocalWalletFamilyUi
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import com.vaiinilla.app.ui.wallet.WalletRemoteUiState
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime

/**
 * La cartera como el dibujo de Doop (E02; el alumno con tutor, E14): el título, el saldo de este lugar con lo del mes,
 * Recargar y Mi código, Mis hijos y los últimos movimientos. «Ver todos» abre Movimientos (E04) y «Recargar», la hoja
 * de Caja (E03). El API solo expone el saldo visible; las bolsas internas de comisión se quedan en el servidor.
 */
@Composable
fun WalletScreen(
    remoteState: WalletRemoteUiState = WalletRemoteUiState(),
    userId: String? = null,
    /** El saldo es por negocio: «Saldo en …» dice de cuál. */
    venueName: String? = null,
    /** Falso cuando el negocio apagó el saldo: se sigue viendo lo que hay, pero ya no se recarga. */
    canRecharge: Boolean = true,
    onRetry: () -> Unit = {},
    onMenu: () -> Unit,
    /** Avisa cuando algo tapa la cartera (el código, la recarga o Movimientos), para quitar la barra de abajo. */
    onCodeOpenChange: (Boolean) -> Unit = {},
    /** "Te cuida Ana": el alumno con tutor ve quién lo cuida (GET /tutelas/mis-controles). */
    caretakerLine: String? = null,
    /** Debajo de quién lo cuida: su límite por día, lo que le queda hoy y lo que no puede pedir (F11). */
    caretakerDetail: String? = null,
    /** Sus controles: con ellos la cartera del alumno es el anillo de lo que le queda hoy (E14). */
    guardianControls: MyGuardianControls? = null,
    /** Mis hijos y lo de tutores y alumnos, solo en una escuela (Modo Padre). */
    familySection: (@Composable () -> Unit)? = null,
    profileInitials: String = "?",
    /** Sale bajo el código de la recarga, para que Caja vea que es la persona. */
    profileName: String? = null,
    onOpenAccount: () -> Unit = {},
    onChangeVenue: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = LocalHapticFeedback.current
    val reduceMotion = reducedMotion()
    val qrValue = userId?.trim()?.takeIf { it.isNotEmpty() }?.let(QrPayloadParser::encodeUser)
    val matrix = remember(qrValue) { qrValue?.let { runCatching { QrCodeEncoder.encode(it) }.getOrNull() } }
    // «Mi código»: el QR personal sobre la cartera; la cartera baja y el frasco se vuelve el código.
    var codeOpen by rememberSaveable { mutableStateOf(false) }
    var reloadOpen by rememberSaveable { mutableStateOf(false) }
    var movementsOpen by rememberSaveable { mutableStateOf(false) }
    // El saldo de antes de una recarga, mientras el QR se deshace de vuelta en el frasco.
    var heldBalance by remember { mutableStateOf<String?>(null) }
    val balance = remoteState.data?.wallet?.visibleBalance
    val shownBalance = heldBalance ?: balance
    val covered = codeOpen || reloadOpen || movementsOpen
    val latestOnCodeOpenChange by rememberUpdatedState(onCodeOpenChange)
    LaunchedEffect(covered) { latestOnCodeOpenChange(covered) }
    DisposableEffect(Unit) { onDispose { latestOnCodeOpenChange(false) } }
    // Mientras el código está a la vista se pregunta por la recarga; al llegar, se cierra solo.
    val waitingReload = codeOpen || reloadOpen
    val latestOnRetry by rememberUpdatedState(onRetry)
    LaunchedEffect(waitingReload) {
        while (waitingReload) {
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
        if (waitingReload && before != null && after != null && after > before) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            reloadOpen = false
            if (codeOpen) {
                // Caja registró la recarga: el código regresa al frasco y el saldo nuevo llega al cerrar.
                heldBalance = previous
                codeOpen = false
                delay(if (reduceMotion) 250 else 1700)
                heldBalance = null
            }
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
    val isStudent = caretakerLine != null && family != null
    val movements = remoteState.data?.movements
    val ledger = remember(movements) { WalletLedgerStats.from(movements.orEmpty()) }
    val scrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val openCode = {
        if (matrix != null && !codeOpen) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            codeOpen = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        LazyColumn(
            state = listState,
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
            item(key = "title") {
                WalletTitleRow(profileInitials = profileInitials, onOpenAccount = onOpenAccount)
            }
            if (isStudent && guardianControls != null && family != null) {
                item(key = "allowance") {
                    StudentAllowance(controls = guardianControls, balance = shownBalance)
                }
                item(key = "locked") { StudentLockedCard(guardianControls) }
                item(key = "student-actions") {
                    StudentActions(
                        codeReady = matrix != null,
                        onCode = openCode,
                        onLinkCode = family.onLinkCode,
                        onHelp = family.onStudentHelp,
                    )
                }
            } else {
                item(key = "balance") {
                    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                        WalletBalance(
                            remoteState = remoteState,
                            shownBalance = shownBalance,
                            venueName = venueName,
                            onChangeVenue = onChangeVenue,
                            monthLine = walletMonthLine(ledger, strong = colors.ink2),
                            onRetry = onRetry,
                        )
                        WalletActions(
                            loading = remoteState.data == null && (remoteState.loading || remoteState.error == null),
                            canRecharge = canRecharge,
                            codeReady = matrix != null,
                            onReload = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                reloadOpen = true
                            },
                            onCode = openCode,
                        )
                        if (remoteState.data != null && !canRecharge) {
                            WalletNote("Este lugar ya no recibe recargas de saldo.")
                        }
                        caretakerLine?.let { WalletNote(listOfNotNull(it, caretakerDetail?.replace('\n', ' ')).joinToString(". ")) }
                    }
                }
            }
            if (family != null) {
                item(key = "family") { Box(Modifier.padding(horizontal = 16.dp)) { family.top() } }
            } else if (familySection != null) {
                item(key = "family") { Box(Modifier.padding(horizontal = 16.dp)) { familySection() } }
            }
            if (movements != null) {
                item(key = "movements") {
                    WalletRecentMovements(
                        movements = movements,
                        onSeeAll = { movementsOpen = true },
                        onMenu = onMenu,
                    )
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
                .graphicsLayer { alpha = if (scrolled) 1f else 0f }
                .background(colors.paper.copy(alpha = 0.94f)),
        )
        WalletMovementsPage(
            visible = movementsOpen,
            movements = movements.orEmpty(),
            onBack = { movementsOpen = false },
        )
        WalletReloadCodeSheet(
            visible = reloadOpen,
            qrValue = qrValue,
            holderName = profileName,
            venueName = venueName,
            onClose = { reloadOpen = false },
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

private fun String.walletAmount(): Double? = trim().replace(",", "").toDoubleOrNull()

@Composable
private fun WalletTitleRow(
    profileInitials: String,
    onOpenAccount: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 16.dp, top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Cartera",
            color = colors.ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.2).sp,
        )
        ProfileInitialsButton(profileInitials, onClick = onOpenAccount, flat = true)
    }
}

@Composable
private fun WalletBalance(
    remoteState: WalletRemoteUiState,
    shownBalance: String?,
    venueName: String?,
    onChangeVenue: (() -> Unit)?,
    monthLine: AnnotatedString?,
    onRetry: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    // Antes de la primera respuesta todavía no hay error: se espera con el esqueleto, no con "No pudimos cargar".
    val waiting = remoteState.data == null && remoteState.error == null
    if (remoteState.data != null || remoteState.loading || waiting) {
        BalanceLabel(venueName = venueName, onChangeVenue = onChangeVenue)
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
                    lineHeight = 64.sp,
                    letterSpacing = (-2.8).sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (remoteState.error != null) {
                WalletNote("Sin conexión: puede que este saldo no sea el más reciente.", top = 4)
            }
            monthLine?.let {
                Text(
                    text = it,
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
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
            WalletNote("Revisa tu conexión o vuelve a entrar al lugar desde el menú.", top = 4)
            TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                Text("Reintentar", color = colors.ink, fontWeight = FontWeight.Black)
            }
        }
    }
}

/** «Saldo en Pastelería Vaini»; si se puede cambiar de lugar, tocarlo lleva a elegir otro. */
@Composable
private fun BalanceLabel(
    venueName: String?,
    onChangeVenue: (() -> Unit)?,
) {
    val colors = LocalVaiinillaColors.current
    val label = venueName?.takeIf { it.isNotBlank() }?.let { "Saldo en $it" } ?: "Tu saldo"
    val changeable = onChangeVenue != null && !venueName.isNullOrBlank()
    Row(
        modifier =
            if (changeable && onChangeVenue != null) {
                Modifier
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onChangeVenue)
                    .semantics { contentDescription = "$label. Cambiar lugar" }
            } else {
                Modifier
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (changeable) {
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.padding(start = 2.dp).size(16.dp),
            )
        }
    }
}

/** «Este mes gastaste $312 y te regresaron $40» (Doop E02); `null` sin movimientos. */
internal fun walletMonthLine(
    ledger: WalletLedgerStats,
    strong: Color = Color.Unspecified,
): AnnotatedString? {
    if (ledger.spentThisMonth == null) return null
    val bold = SpanStyle(color = strong, fontWeight = FontWeight.ExtraBold)
    return buildAnnotatedString {
        if (ledger.spentAnyThisMonth) {
            append("Este mes gastaste ")
            withStyle(bold) { append(ledger.spentThisMonth) }
        } else {
            append("Este mes no has gastado")
        }
        ledger.returnedThisMonth?.let { returned ->
            append(" y te regresaron ")
            withStyle(bold) { append(returned) }
        }
    }
}

@Composable
private fun WalletActions(
    loading: Boolean,
    canRecharge: Boolean,
    codeReady: Boolean,
    onReload: () -> Unit,
    onCode: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (loading) {
            SkeletonBlock(modifier = Modifier.weight(1f).height(50.dp), corner = 25.dp)
            SkeletonBlock(modifier = Modifier.weight(1f).height(50.dp), corner = 25.dp)
            return@Row
        }
        if (canRecharge) {
            WalletButton(
                label = "Recargar",
                primary = true,
                enabled = codeReady,
                onClick = onReload,
                modifier = Modifier.weight(1f).testTag("wallet-recharge"),
            )
        }
        WalletButton(
            label = "Mi código",
            primary = false,
            enabled = codeReady,
            onClick = onCode,
            modifier = Modifier.weight(1f).testTag("wallet-code"),
        )
    }
}

/** Los botones redondos de la cartera: el claro es la acción principal (Doop E02 y E14). */
@Composable
internal fun WalletButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Int = 50,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            modifier
                .height(height.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .physicalPress(enabled = enabled, onClick = onClick)
                .clip(CircleShape)
                .background(if (primary) colors.ink else colors.paper2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) colors.onInk else colors.ink,
            fontSize = 15.5.sp,
            fontWeight = if (primary) FontWeight.Black else FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun WalletNote(
    text: String,
    top: Int = 12,
) {
    val colors = LocalVaiinillaColors.current
    Text(
        text = text,
        color = colors.muted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(top = top.dp),
    )
}

/**
 * Las cifras de la cartera, sacadas de los movimientos que ya baja la app: lo gastado y lo que regresó este mes (E02) y
 * lo que entró (E04). Sin movimientos, ninguna.
 */
internal class WalletLedgerStats(
    val spentThisMonth: String?,
    val reloads: String?,
    val lastMovement: String?,
    /** Cashback y devoluciones de este mes (Doop E02); `null` si no hubo. */
    val returnedThisMonth: String? = null,
    /** Todo lo que entró este mes: recargas, cashback y devoluciones (Doop E04). */
    val incomeThisMonth: String? = null,
    val spentAnyThisMonth: Boolean = false,
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
            var returned = BigDecimal.ZERO
            var income = BigDecimal.ZERO
            for (movement in movements) {
                val date = movement.createdAt.toInstantOrNull()?.let { BusinessTime.at(it, zone).toLocalDate() }
                val value =
                    movement.amount
                        .replace(",", "")
                        .trim()
                        .toBigDecimalOrNull()
                if (date == null || value == null || date.year != today.year || date.month != today.month) continue
                if (movement.amount.startsWith("-")) {
                    spent += value.negate()
                } else {
                    income += value
                    if (movement.type == "cashback" || MovementFilter.REFUNDS.matches(movement.type)) returned += value
                }
            }
            val reloads = movements.count { !it.amount.startsWith("-") }
            val last = cremeMoment(movements.first().createdAt, now)
            return WalletLedgerStats(
                spentThisMonth = compactMoney(spent),
                reloads = reloads.toString(),
                lastMovement = last,
                returnedThisMonth = returned.takeIf { it.signum() > 0 }?.let(::compactMoney),
                incomeThisMonth = compactMoney(income),
                spentAnyThisMonth = spent.signum() > 0,
            )
        }

        private fun String.toInstantOrNull(): Instant? =
            runCatching { OffsetDateTime.parse(this).toInstant() }.getOrNull()
    }
}

/** El código personal en grande, para que caja lo escanee al recargar. */
@Composable
internal fun WalletQrDialog(
    qrValue: String?,
    onDismiss: () -> Unit,
    title: String = "Recargar saldo",
    message: String = "Muestra este QR en Caja para encontrar tu cuenta y recargar Saldo Vaiinilla.",
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
                        text = title,
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
                    text = message,
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