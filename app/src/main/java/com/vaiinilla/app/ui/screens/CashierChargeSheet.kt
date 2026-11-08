package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.venue.LocalVenueContext
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.TipChoice
import com.vaiinilla.app.domain.model.TipPercents
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.domain.model.amountFor
import com.vaiinilla.app.domain.model.asTipWire
import com.vaiinilla.app.domain.model.cashAmountDue
import com.vaiinilla.app.domain.model.needsCashCollection
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.counter.TicketAllergyBand
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * C02: la hoja de un pedido de Caja. Si falta dinero, cobra en efectivo (con el cambio y la propina a la vista);
 * si ya está pagado y listo, lo entrega. Lo escrito vive en [draft], fuera de la hoja: el sondeo trae una copia
 * nueva del pedido cada pocos segundos y el cobro no se reinicia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierChargeSheet(
    order: OrderDetail,
    draft: CashierCashDraft?,
    onDraftChange: (CashierCashDraft) -> Unit,
    acting: Boolean,
    errorMessage: String?,
    cashSessionOpen: Boolean?,
    readOnly: Boolean,
    deliveryRequiresQr: Boolean,
    colors: OperationalColors,
    onCollect: (orderId: String, amount: String, version: Int, tip: String?) -> Unit,
    onDeliver: (orderId: String, version: Int) -> Unit,
    onScanDeliver: (orderId: String, version: Int) -> Unit,
    onRemoveItem: (OrderItem) -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
) {
    val venue = LocalVenueContext.current
    val haptics = rememberVaiinillaHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val summary = order.summary
    // Cocina puede arrancar el pedido antes de cobrarlo: el saldo sigue por cobrar en preparando y en listo, y no
    // se entrega mientras falte.
    val needsCash = summary.needsCashCollection
    val amountDue = summary.cashAmountDue
    val orderTotal = runCatching { BigDecimal(amountDue) }.getOrDefault(BigDecimal.ZERO)
    // Si lo que se cobra cambió (se quitó un artículo), lo escrito ya no aplica y se vuelve a sugerir; la propina
    // elegida se conserva.
    val cashDraft =
        draft?.takeIf { it.orderTotal == amountDue }
            ?: (draft?.tip ?: TipChoice.None).let { tip ->
                CashierCashDraft(
                    orderTotal = amountDue,
                    received = Money.format(orderTotal + tip.amountFor(orderTotal)),
                    tip = tip,
                )
            }
    val cashReceivedInput = cashDraft.received
    // Una renta de cancha no lleva propina (W21, Consejera; QAIOS-13 en iOS).
    val isRental = order.reservation != null
    val tipChoice = if (isRental) TipChoice.None else cashDraft.tip
    val cashRegisterOpen = cashSessionOpen == true
    val enabled = !acting && !readOnly
    // El progreso es del cobro: otra acción en curso solo deshabilita el botón.
    var collecting by remember { mutableStateOf(false) }
    // Quitar y rechazar quedan detrás de «Más opciones», lejos de Cobrar.
    var showMore by remember { mutableStateOf(false) }
    val cashFieldFocus = remember { FocusRequester() }
    LaunchedEffect(acting, errorMessage) {
        if (!acting) collecting = false
    }

    ModalBottomSheet(
        onDismissRequest = { if (!collecting) onDismiss() },
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "PEDIDO #${summary.folio} · ${cashierOrderOrigin(venue, order).uppercase()}${arrivalSuffix(order)}",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                )
                ChargeBadge(paid = !needsCash, colors = colors)
            }
            Text(
                if (needsCash) "Cobrar en efectivo" else "Entregar pedido",
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            // Sin cobro de por medio, la entrega depende de cómo va en cocina.
            if (!needsCash) CashierOrderStatus(summary = summary, modifier = Modifier.padding(top = 4.dp))
            TicketAllergyBand(order.user?.allergies.orEmpty())

            // Lo que Cocina o Caja quitaron ya no cuenta ni en el total ni en el ticket. El último artículo no se
            // quita: se rechaza el pedido entero.
            val canRemoveItems = !readOnly && order.activeItems.size > 1 && !acting && showMore
            Column(modifier = Modifier.padding(top = 12.dp)) {
                order.activeItems.forEach { item ->
                    ChargeLine(
                        quantity = item.quantity,
                        name = item.productName,
                        detail = item.options.joinToString(" · ") { it.name }.ifEmpty { null },
                        price = moneyLabel(item.subtotal),
                        colors = colors,
                        onRemove = if (canRemoveItems) ({ onRemoveItem(item) }) else null,
                    )
                }
            }
            ChargeDashedLine(colors, Modifier.padding(top = 6.dp, bottom = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (needsCash && amountDue != summary.total) "Pendiente" else "Total",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    moneyLabel(if (needsCash) amountDue else summary.total),
                    color = colors.textPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                )
            }

            if (needsCash) {
                val tipAmount = tipChoice.amountFor(orderTotal)
                val tipProblem = tipProblem(tipChoice, orderTotal)
                val required = orderTotal + tipAmount
                val receivedAmount = MoneyInput.parse(cashReceivedInput)
                val changeDue = receivedAmount?.minus(required)
                ChargeLabel("¿CON CUÁNTO PAGA?", colors, Modifier.padding(top = 16.dp))
                CashReceivedField(
                    value = cashReceivedInput,
                    placeholder = Money.format(required),
                    enabled = enabled,
                    colors = colors,
                    focusRequester = cashFieldFocus,
                    onValueChange = { raw -> onDraftChange(cashDraft.copy(received = MoneyInput.sanitize(raw))) },
                )
                CashBillChips(
                    required = required,
                    received = receivedAmount,
                    enabled = enabled,
                    colors = colors,
                    onPick = { value ->
                        haptics.selection()
                        onDraftChange(cashDraft.copy(received = value))
                    },
                    onOther = {
                        haptics.selection()
                        onDraftChange(cashDraft.copy(received = ""))
                        cashFieldFocus.requestFocus()
                    },
                )
                // El cambio se ve antes de confirmar, mientras el cajero cuenta los billetes.
                cashChangeHint(changeDue)?.let { hint ->
                    CashChangeCard(
                        hint = hint,
                        change = changeDue,
                        colors = colors,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (!isRental) {
                    ChargeTipRow(
                        choice = tipChoice,
                        base = orderTotal,
                        problem = tipProblem,
                        enabled = !acting,
                        colors = colors,
                        onChange = { next ->
                            // El efectivo sugerido sigue a la propina; lo tecleado no se pisa.
                            val suggestion = Money.format(required)
                            val withTip = Money.format(orderTotal + next.amountFor(orderTotal))
                            val keep = cashReceivedInput != suggestion
                            onDraftChange(
                                cashDraft.copy(received = if (keep) cashReceivedInput else withTip, tip = next),
                            )
                        },
                    )
                }
                if (!cashRegisterOpen) {
                    Text(
                        if (cashSessionOpen ==
                            false
                        ) {
                            "Abre la caja para cobrar."
                        } else {
                            "Revisando si la caja está abierta…"
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Button(
                    onClick = {
                        haptics.impact()
                        collecting = true
                        onCollect(summary.id, cashReceivedInput, summary.version, tipAmount.asTipWire())
                    },
                    enabled =
                        cashRegisterOpen &&
                            tipProblem == null &&
                            receivedAmount != null &&
                            receivedAmount >= required &&
                            enabled,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.textPrimary,
                            contentColor = colors.background,
                            disabledContainerColor = colors.cardInner,
                            disabledContentColor = colors.textMuted,
                        ),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(58.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                ) {
                    if (collecting && acting) {
                        CircularProgressIndicator(
                            color = colors.background,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (collecting && acting) "Cobrando…" else "Cobrar ${moneyLabel(Money.format(required))}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            } else {
                // La entrega exige el QR del cliente, salvo que el backend lo dispense para este pedido
                // (`entrega_sin_qr`, una cuenta abierta), que el negocio no lo pida, o que el pedido lo hiciera esta
                // misma caja y su token ya esté en el dispositivo.
                val withoutQr = cashierDeliversWithoutQr(order, deliveryRequiresQr)
                Button(
                    onClick = {
                        haptics.impact()
                        if (withoutQr) {
                            onDeliver(
                                summary.id,
                                summary.version,
                            )
                        } else {
                            onScanDeliver(summary.id, summary.version)
                        }
                    },
                    enabled = summary.state == OrderState.READY && enabled,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.textPrimary,
                            contentColor = colors.background,
                            disabledContainerColor = colors.cardInner,
                            disabledContentColor = colors.textMuted,
                        ),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp).heightIn(min = 58.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                ) {
                    Icon(
                        if (withoutQr) Icons.Rounded.Check else Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    // L01: con letra grande se cortaba; se encoge y, si aún no cabe, pasa a dos renglones.
                    Text(
                        if (withoutQr) "Entregar" else "Escanear QR para entregar",
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 17.sp, stepSize = 0.5.sp),
                    )
                }
            }
            if (!readOnly) {
                CashierMoreOptions(
                    expanded = showMore,
                    enabled = !acting,
                    colors = colors,
                    onToggle = {
                        haptics.selection()
                        showMore = !showMore
                    },
                    onReject = onReject,
                )
            }
        }
    }
}

/** «FALTA PAGAR» en coral o «PAGADO» en lima, arriba a la derecha de la hoja. */
@Composable
private fun ChargeBadge(
    paid: Boolean,
    colors: OperationalColors,
) {
    val coral = if (colors.isDark) LocalVaiinillaColors.current.coral else CashShortInk
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(if (paid) colors.accentLime else coral.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            if (paid) "PAGADO" else "FALTA PAGAR",
            color = if (paid) colors.accentInk else coral,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun ChargeLabel(
    text: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        color = colors.textSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.4.sp,
        modifier = modifier,
    )
}

/** La línea punteada del total, como la de un ticket. */
@Composable
private fun ChargeDashedLine(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    val line = colors.cardBorder
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(1.dp)
                .drawBehind {
                    drawLine(
                        color = line,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = size.height,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                    )
                },
    )
}

/** «1 × Pastel de fresas · mediano  $450»; con «Más opciones» abierto, cada renglón se puede quitar. */
@Composable
private fun ChargeLine(
    quantity: Int,
    name: String,
    detail: String?,
    price: String,
    colors: OperationalColors,
    onRemove: (() -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "$quantity × $name" + detail?.let { " · $it" }.orEmpty(),
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(end = 10.dp),
            )
            Text(price, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        if (onRemove != null) {
            Text(
                "Quitar",
                color = Color(0xFFE5645A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "Quitar $name de la comanda", onClick = onRemove)
                        .padding(vertical = 6.dp),
            )
        }
    }
}

/** El efectivo que entrega el cliente, en grande y con borde lima: es lo único que el cajero escribe. */
@Composable
private fun CashReceivedField(
    value: String,
    placeholder: String,
    enabled: Boolean,
    colors: OperationalColors,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TextStyle(color = colors.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black),
        cursorBrush = SolidColor(colors.textPrimary),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(64.dp)
                .focusRequester(focusRequester)
                .semantics { contentDescription = "Efectivo recibido" }
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .border(2.dp, colors.accentLime, RoundedCornerShape(20.dp)),
        decorationBox = { inner ->
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$", color = colors.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = colors.textMuted, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                    inner()
                }
                Text(
                    "MXN",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                )
            }
        },
    )
}

/** Los billetes que tiene sentido ofrecer: los de $100, $200, $500 y $1,000 que alcancen, hasta tres. */
internal fun cashBillOptions(required: BigDecimal): List<BigDecimal> =
    listOf(100, 200, 500, 1000)
        .map(::BigDecimal)
        .filter { it > required }
        .take(3)

@Composable
private fun CashBillChips(
    required: BigDecimal,
    received: BigDecimal?,
    enabled: Boolean,
    colors: OperationalColors,
    onPick: (String) -> Unit,
    onOther: () -> Unit,
) {
    val bills = cashBillOptions(required)
    val exact = received != null && received.compareTo(required) == 0
    val other = received != null && !exact && bills.none { it.compareTo(received) == 0 }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChargeChip("Exacto", exact, enabled, colors, Modifier.weight(1f)) { onPick(Money.format(required)) }
        bills.forEach { bill ->
            ChargeChip(
                "$${bill.toPlainString()}",
                received != null && received.compareTo(bill) == 0,
                enabled,
                colors,
                Modifier.weight(1f),
            ) { onPick(Money.format(bill)) }
        }
        ChargeChip("Otro", other, enabled, colors, Modifier.weight(1f), onClick = onOther)
    }
}

/** Un billete: el elegido en tinta. La letra se achica antes de cortar «$1000» en un teléfono angosto. */
@Composable
private fun ChargeChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = animateSelectionColor(if (selected) colors.textPrimary else colors.cardBackground, "charge-chip-bg")
    val fg = animateSelectionColor(if (selected) colors.background else colors.textPrimary, "charge-chip-ink")
    Box(
        modifier =
            modifier
                // Es una opción de un grupo: el lector de pantalla dice cuál está elegida.
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                }.physicalPress(scale = PhysicalPressScale.Small, enabled = enabled, onClick = onClick)
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = fg,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 15.sp, stepSize = 0.5.sp),
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

/** Una propina: píldora de 38 con el toque de 44; la elegida se marca con contorno, no compite con el billete. */
@Composable
private fun ChargeTipChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = animateSelectionColor(if (selected) colors.cardInner else colors.cardBackground, "charge-tip-bg")
    val outline = animateSelectionColor(if (selected) colors.textPrimary else Color.Transparent, "charge-tip-outline")
    Box(
        modifier =
            modifier
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                }.physicalPress(scale = PhysicalPressScale.Small, enabled = enabled, onClick = onClick)
                .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .border(1.5.dp, outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = colors.textPrimary,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 13.sp, stepSize = 0.5.sp),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** «Su cambio» en lima con los billetes que hay que dar; si no alcanza, lo dice en rojo. */
@Composable
private fun CashChangeCard(
    hint: String,
    change: BigDecimal?,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    if (change == null || change.signum() < 0) {
        Text(
            hint,
            color = if (colors.isDark) LocalVaiinillaColors.current.coral else CashShortInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        return
    }
    val pieces = cashChangePieces(change)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.accentLime)
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = listOfNotNull(hint, pieces).joinToString(", ")
                    liveRegion = LiveRegionMode.Polite
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (change.signum() == 0) "Exacto, sin cambio" else "Su cambio",
                color = colors.accentInk.copy(alpha = 0.75f),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            if (pieces != null) {
                Text(
                    pieces,
                    color = colors.accentInk.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        AnimatedContent(
            targetState = moneyLabel(Money.format(change)),
            transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
            label = "cash-change",
        ) { value ->
            Text(
                value,
                color = colors.accentInk,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.2).sp,
            )
        }
    }
}

private val MxnBills = listOf("1000", "500", "200", "100", "50", "20").map(::BigDecimal)
private val MxnCoins = listOf("10", "5", "2", "1", "0.50").map(::BigDecimal)

/**
 * Con qué billetes y monedas se da el cambio, de mayor a menor: «1 billete de $20 y 1 moneda de $5». Null sin
 * cambio. Los centavos que no llegan a $0.50 no se dicen: no hay moneda para darlos.
 */
internal fun cashChangePieces(change: BigDecimal): String? {
    if (change.signum() <= 0) return null
    var rest = change.setScale(2, RoundingMode.HALF_UP)
    val parts = mutableListOf<String>()
    (MxnBills.map { it to "billete" } + MxnCoins.map { it to "moneda" }).forEach { (value, noun) ->
        val count = rest.divideToIntegralValue(value).toInt()
        if (count > 0) {
            rest -= value.multiply(BigDecimal(count))
            parts += "$count ${if (count == 1) noun else "${noun}s"} de ${moneyLabel(Money.format(value))}"
        }
    }
    return when (parts.size) {
        0 -> null
        1 -> parts.single()
        else -> parts.dropLast(1).joinToString(", ") + " y " + parts.last()
    }
}

/** «Sin propina · 10% · $48 · 15% · $72 · Otra», como en C02; «Otra» abre el campo del monto libre. */
@Composable
private fun ChargeTipRow(
    choice: TipChoice,
    base: BigDecimal,
    problem: String?,
    enabled: Boolean,
    colors: OperationalColors,
    onChange: (TipChoice) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        ChargeLabel("PROPINA", colors)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChargeTipChip("Sin propina", choice == TipChoice.None, enabled, colors, Modifier.weight(1f)) {
                onChange(TipChoice.None)
            }
            TipPercents.take(2).forEach { percent ->
                val option = TipChoice.Percent(percent)
                ChargeTipChip(
                    "$percent% · ${moneyLabel(Money.format(option.amountFor(base)))}",
                    choice == option,
                    enabled,
                    colors,
                    Modifier.weight(1f),
                ) { onChange(option) }
            }
            ChargeTipChip("Otra", choice is TipChoice.Custom, enabled, colors, Modifier.weight(1f)) {
                onChange(TipChoice.Custom((choice as? TipChoice.Custom)?.amount.orEmpty()))
            }
        }
        val custom = choice as? TipChoice.Custom
        if (custom != null) {
            BasicTextField(
                value = custom.amount,
                onValueChange = { raw -> onChange(TipChoice.Custom(MoneyInput.sanitize(raw))) },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(colors.textPrimary),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                decorationBox = { inner ->
                    if (custom.amount.isEmpty()) Text("Monto de la propina", color = colors.textMuted, fontSize = 15.sp)
                    inner()
                },
            )
            if (problem != null) {
                Text(
                    problem,
                    color = if (colors.isDark) LocalVaiinillaColors.current.coral else CashShortInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

/** «Más opciones ···»: quitar un artículo o rechazar todo el pedido, detrás de un toque y con su confirmación. */
@Composable
private fun CashierMoreOptions(
    expanded: Boolean,
    enabled: Boolean,
    colors: OperationalColors,
    onToggle: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .physicalPress(scale = PhysicalPressScale.Small, enabled = enabled, onClick = onToggle)
                    .heightIn(min = 48.dp)
                    .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (expanded) "Menos opciones" else "Más opciones",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.width(6.dp))
                MoreDots(colors.textSecondary)
            }
            if (!expanded) {
                Text(
                    "Quitar un artículo o cancelar el pedido, con confirmación",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Toca «Quitar» en un artículo o rechaza todo el pedido.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = onReject,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(
                        "Rechazar pedido",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (enabled) (if (colors.isDark) LocalVaiinillaColors.current.coral else CashShortInk) else colors.textMuted,
                    )
                }
            }
        }
    }
}

/** Por qué la propina escrita no sirve; null si está bien o no es un monto libre. */
internal fun tipProblem(
    choice: TipChoice,
    base: BigDecimal,
): String? {
    val custom = choice as? TipChoice.Custom ?: return null
    val text = custom.amount.trim()
    if (text.isEmpty()) return null
    val value =
        text.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 && it.scale() <= 2 }
            ?: return "Escribe un monto válido, con hasta dos decimales."
    return if (value > base) "La propina no puede ser mayor al pedido." else null
}

/** "Cambio: $50.00" con el efectivo ya escrito; "Faltan $X" si no alcanza; null si aún no hay monto. */
internal fun cashChangeHint(change: BigDecimal?): String? =
    when {
        change == null -> null
        change.signum() < 0 -> "Faltan ${moneyLabel(Money.format(change.negate()))}"
        else -> "Cambio: ${moneyLabel(Money.format(change))}"
    }

/** El cobro en efectivo que el cajero va armando: lo recibido y la propina elegida. */
internal data class CashierCashDraft(
    /** El total del pedido cuando se empezó a escribir. */
    val orderTotal: String,
    val received: String,
    val tip: TipChoice = TipChoice.None,
)
