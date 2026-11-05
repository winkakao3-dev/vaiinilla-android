package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.operational.CloseCashUi
import com.vaiinilla.app.ui.screens.admin.AdminPrimaryButton
import com.vaiinilla.app.ui.screens.admin.moneyLabelCents
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.math.BigDecimal

/** Efectivo contado válido: número no negativo con hasta dos decimales, normalizado a "725.50". */
fun normalizeCountedCash(raw: String): String? {
    val text = raw.trim().removePrefix("$").replace(",", ".")
    if (!Regex("""\d+(\.\d{1,2})?""").matches(text)) return null
    val parts = text.split(".")
    val cents = (parts.getOrNull(1) ?: "").padEnd(2, '0')
    return "${parts[0].trimStart('0').ifEmpty { "0" }}.$cents"
}

/** El conteo del cierre de caja: el total escrito y, si se contó billete por billete, cada denominación. */
data class CashCountDraft(
    val amount: String = "",
    val counting: Boolean = false,
    val billCounts: Map<Int, Int> = emptyMap(),
    val coins: String = "",
) {
    companion object {
        /** Guarda el conteo en el estado de la ruta: sobrevive a ir a «Ver cuentas» y volver. */
        val Saver: Saver<CashCountDraft, Any> =
            listSaver(
                save = { draft ->
                    listOf(
                        draft.amount,
                        draft.counting,
                        draft.coins,
                        ArrayList(draft.billCounts.flatMap { (bill, count) -> listOf(bill, count) }),
                    )
                },
                restore = { saved ->
                    val bills = (saved[3] as List<*>).map { it as Int }
                    CashCountDraft(
                        amount = saved[0] as String,
                        counting = saved[1] as Boolean,
                        coins = saved[2] as String,
                        billCounts = bills.chunked(2).associate { (bill, count) -> bill to count },
                    )
                },
            )
    }
}

/**
 * Cierre de caja (C07): lo que debe haber, el efectivo contado y la diferencia en grande, y se confirma
 * deslizando porque no se deshace. Si el servidor rechaza el cierre porque hay cuentas a pagar al final sin
 * cobrar, la hoja lo explica y lleva directo a esas cuentas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashCloseSheet(
    state: CloseCashUi,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onOpenAccounts: (() -> Unit)?,
    ordersToExpire: Int = 0,
    amountToExpire: BigDecimal = BigDecimal.ZERO,
    expectedCash: String? = null,
    openingAmount: String? = null,
    countState: MutableState<CashCountDraft>? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            CashCloseContent(
                state,
                colors,
                onDismiss,
                onConfirm,
                onOpenAccounts,
                ordersToExpire,
                amountToExpire,
                expectedCash,
                openingAmount,
                countState,
            )
        }
    }
}

/** Contado menos esperado; null si falta alguno de los dos. */
internal fun cashCountDifference(
    counted: String?,
    expected: String?,
): BigDecimal? {
    val c = counted?.toBigDecimalOrNull() ?: return null
    val e = expected?.toBigDecimalOrNull() ?: return null
    return c - e
}

@Composable
fun CashCloseContent(
    state: CloseCashUi,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onOpenAccounts: (() -> Unit)?,
    ordersToExpire: Int = 0,
    amountToExpire: BigDecimal = BigDecimal.ZERO,
    expectedCash: String? = null,
    openingAmount: String? = null,
    /** Dónde vive el conteo; si es null, la hoja lo guarda ella misma. */
    countState: MutableState<CashCountDraft>? = null,
) {
    val ownCount = rememberSaveable(stateSaver = CashCountDraft.Saver) { mutableStateOf(CashCountDraft()) }
    val count = countState ?: ownCount
    val draft = count.value
    val amount = draft.amount
    val counting = draft.counting
    val billCounts = draft.billCounts
    val coins = draft.coins
    val normalized = normalizeCountedCash(amount)
    val haptics = rememberVaiinillaHaptics()

    /** Con el contador, el total se recalcula de los billetes y monedas. */
    fun updateCounter(next: CashCountDraft) {
        count.value = billCountTotal(next.billCounts, next.coins)?.let { next.copy(amount = it) } ?: next
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .physicalPress(enabled = !state.closing, scale = PhysicalPressScale.Small) { onDismiss() }
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Regresar",
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            "Cerrar caja",
            fontWeight = FontWeight.Black,
            fontSize = 30.sp,
            letterSpacing = (-1).sp,
            color = colors.textPrimary,
        )
        AnimatedContent(
            targetState = state.blockedByAccounts,
            transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
            label = "cash-close-step",
        ) { blocked ->
            if (blocked) {
                CashWarningCard(
                    title = "Hay una cuenta sin cobrar",
                    body =
                        "Una mesa o ${Venue.court} pidió pagar al final y todavía no se cobra. " +
                            "Cóbrala antes de cerrar para no perder ese dinero.",
                    action =
                        onOpenAccounts?.let { open ->
                            "Ver cuentas por cobrar" to {
                                haptics.impact()
                                onDismiss()
                                open()
                            }
                        },
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    expiringOrdersNotice(ordersToExpire, amountToExpire)?.let {
                        CashWarningCard(title = it, body = "Cóbralos antes de cerrar para no perder ese dinero.")
                    }
                    if (expectedCash != null) {
                        val cashIn =
                            openingAmount?.toBigDecimalOrNull()?.let { opening ->
                                expectedCash.toBigDecimalOrNull()?.minus(opening)?.toPlainString()
                            }
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(colors.cardBackground)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            openingAmount?.let { CloseRow("Fondo al abrir", moneyLabelCents(it), colors) }
                            cashIn?.let { CloseRow("Cobros en efectivo", "+${moneyLabelCents(it)}", colors) }
                            CloseRow("Debe haber", moneyLabelCents(expectedCash), colors, strong = true)
                        }
                    }
                    val invalid = amount.isNotBlank() && normalized == null
                    CashAmountField(
                        value = amount,
                        onValueChange = { count.value = count.value.copy(amount = it) },
                        label = "EFECTIVO CONTADO",
                        colors = colors,
                        enabled = !state.closing,
                        isError = invalid,
                        supporting = if (invalid) "Solo números, con hasta dos decimales" else null,
                    )
                    Text(
                        if (counting) "Escribir el total" else "Contar billete por billete",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = colors.accentLime,
                        modifier =
                            Modifier
                                .physicalPress(enabled = !state.closing, scale = PhysicalPressScale.Small) {
                                    haptics.selection()
                                    val next = count.value.copy(counting = !count.value.counting)
                                    if (next.counting && (next.billCounts.isNotEmpty() || next.coins.isNotBlank())) {
                                        updateCounter(next)
                                    } else {
                                        count.value = next
                                    }
                                }.heightIn(min = 40.dp)
                                .wrapContentHeight(),
                    )
                    AnimatedVisibility(
                        visible = counting,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        CashBillCounter(
                            counts = billCounts,
                            coins = coins,
                            colors = colors,
                            enabled = !state.closing,
                            onCountChange = { bill, bills ->
                                updateCounter(count.value.copy(billCounts = count.value.billCounts + (bill to bills)))
                            },
                            onCoinsChange = { updateCounter(count.value.copy(coins = it)) },
                        )
                    }
                    cashCountDifference(normalized, expectedCash)?.let { diff ->
                        CashDifferenceCard(diff, colors)
                    }
                    state.errorMessage?.let {
                        Text(it, fontSize = 13.sp, color = colors.textSecondary)
                    }
                    AdminPrimaryButton(
                        title =
                            when {
                                state.closing -> "Cerrando…"
                                normalized == null -> "Escribe el efectivo contado"
                                else -> "Cerrar caja con ${moneyLabel(normalized)}"
                            },
                        colors = colors,
                        busy = state.closing,
                        enabled = normalized != null,
                    ) { normalized?.let(onConfirm) }
                }
            }
        }
    }
}

@Composable
private fun CloseRow(
    label: String,
    value: String,
    colors: OperationalColors,
    strong: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontWeight = if (strong) FontWeight.Black else FontWeight.SemiBold,
            fontSize = 14.sp,
            color = colors.textPrimary,
        )
        Text(value, fontWeight = FontWeight.Black, fontSize = if (strong) 18.sp else 14.sp, color = colors.textPrimary)
    }
}

/** El aviso ámbar de C07: lo que se pierde si se cierra así. */
@Composable
private fun CashWarningCard(
    title: String,
    body: String,
    action: Pair<String, () -> Unit>? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CashWarnBg)
                .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = CashWarnInk,
            modifier = Modifier.size(22.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFFFAF7F0))
            Text(body, fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFFD9D2BF))
            action?.let { (label, onClick) ->
                Text(
                    label,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = CashWarnInk,
                    modifier =
                        Modifier
                            .padding(top = 4.dp)
                            .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                            .heightIn(min = 40.dp)
                            .wrapContentHeight(),
                )
            }
        }
    }
}

private val CashWarnBg = Color(0xFF3A3220)
private val CashWarnInk = Color(0xFFF2C14E)

/** «Cuadra», «Faltan $20» o «Sobran $20», en grande, mientras el cajero escribe. */
@Composable
private fun CashDifferenceCard(
    diff: BigDecimal,
    colors: OperationalColors,
) {
    val coral = LocalVaiinillaColors.current.coral
    val (label, tint) =
        when (diff.signum()) {
            0 -> "Cuadra" to colors.accentLime
            -1 -> "Faltan" to coral
            else -> "Sobran" to CashWarnInk
        }
    val bg = animateSelectionColor(tint.copy(alpha = 0.16f), "cash-diff-bg")
    val ink = animateSelectionColor(tint, "cash-diff-ink")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ink)
        if (diff.signum() != 0) {
            Text(
                moneyLabel(Money.format(diff.abs())),
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = ink,
            )
        }
    }
}

/** El monto en grande dentro de un recuadro lima, como en C06 y C07; el teclado es numérico. */
@Composable
internal fun CashAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    colors: OperationalColors,
    enabled: Boolean,
    isError: Boolean,
    supporting: String?,
    centered: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val coral = LocalVaiinillaColors.current.coral
    val border = animateSelectionColor(if (isError) coral else colors.accentLime, "cash-field-border")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            color = colors.textSecondary,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        BasicTextField(
            value = value,
            // Sin recortar: lo inválido se ve y se explica abajo, no se corrige solo.
            onValueChange = onValueChange,
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle =
                TextStyle(
                    color = colors.textPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                ),
            cursorBrush = SolidColor(colors.accentLime),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cardBackground)
                    .border(2.dp, border, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .semantics { contentDescription = label.lowercase().replaceFirstChar { it.uppercase() } },
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$",
                        color = if (value.isEmpty()) colors.textMuted else colors.textPrimary,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Box(modifier = Modifier.weight(1f, fill = !centered)) {
                        if (value.isEmpty()) {
                            Text("0.00", color = colors.textMuted, fontSize = 40.sp, fontWeight = FontWeight.Black)
                        }
                        inner()
                    }
                }
            },
        )
        supporting?.let { Text(it, fontSize = 12.sp, color = coral) }
    }
}

/** Lo que se pierde al cerrar: el servidor expira los pedidos en efectivo que siguen por cobrar. */
internal fun expiringOrdersNotice(
    count: Int,
    amount: BigDecimal,
): String? =
    when (count) {
        0 -> null
        1 -> "Hay 1 pedido por cobrar (${moneyLabel(Money.format(amount))}) que expirará al cerrar."
        else -> "Hay $count pedidos por cobrar (${moneyLabel(Money.format(amount))}) que expirarán al cerrar."
    }

/** Abrir la caja (C06): el cajero cuenta el efectivo del cajón; si cuadra con el fondo de siempre, solo confirma. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashOpenSheet(
    opening: Boolean,
    errorMessage: String?,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    suggestedAmount: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        CashOpenContent(opening, errorMessage, colors, onConfirm, suggestedAmount)
    }
}

@Composable
fun CashOpenContent(
    opening: Boolean,
    errorMessage: String?,
    colors: OperationalColors,
    onConfirm: (String) -> Unit,
    suggestedAmount: String? = null,
) {
    // El fondo del servidor ya escrito: si cuadra con el cajón se confirma en un toque (Kao, 25 oct).
    var amount by remember(suggestedAmount) { mutableStateOf(suggestedAmount.orEmpty()) }
    val normalized = normalizeCountedCash(amount)
    val suggested = suggestedAmount?.let(::normalizeCountedCash)
    val matchesSuggested = suggested != null && normalized == suggested
    val focus = remember { FocusRequester() }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.cardBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            "Abrir caja",
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            letterSpacing = (-1).sp,
            color = colors.textPrimary,
        )
        Text(
            "Cuenta el efectivo que hay en el cajón antes de abrir. Así al cerrar sabes si cuadra.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = colors.textSecondary,
        )
        val invalid = amount.isNotBlank() && normalized == null
        CashAmountField(
            value = amount,
            onValueChange = { amount = it },
            label = "EFECTIVO EN EL CAJÓN",
            colors = colors,
            enabled = !opening,
            isError = invalid,
            supporting = if (invalid) "Solo números, con hasta dos decimales" else null,
            centered = true,
            focusRequester = focus,
        )
        if (matchesSuggested) {
            Text(
                "Es el fondo de siempre. Si cuadra, solo confirma.",
                fontSize = 13.sp,
                color = colors.textSecondary,
            )
        }
        errorMessage?.let {
            Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        }
        ActionPill(
            label =
                when {
                    opening -> "Abriendo…"
                    normalized == null -> "Escribe el efectivo del cajón"
                    matchesSuggested -> "Sí, hay ${moneyLabel(normalized)} · Abrir caja"
                    else -> "Abrir caja con ${moneyLabel(normalized)}"
                },
            enabled = normalized != null,
            loading = opening,
            colors = colors,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { normalized?.let(onConfirm) }
        if (matchesSuggested && !opening) {
            ActionPill(
                label = "Conté otra cantidad",
                enabled = true,
                loading = false,
                primary = false,
                colors = colors,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                amount = ""
                focus.requestFocus()
            }
        }
    }
}
