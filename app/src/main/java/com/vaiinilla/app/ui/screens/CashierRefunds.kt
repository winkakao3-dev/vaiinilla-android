package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.repository.PendingRefund
import com.vaiinilla.app.domain.repository.RefundMethod
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.operational.PendingRefundsUiState

/** Por qué hay que devolver, como lo dice la web de Caja. */
internal fun refundOriginLabel(origin: String): String =
    when (origin) {
        "sobrante_cuenta" -> "Se abonó de más a la cuenta"
        "rechazo_articulo" -> "Se quitó un artículo ya pagado"
        "cancelacion" -> "Se canceló un pedido ya pagado"
        "reserva_cancelada" -> "Se canceló una reserva ya pagada"
        else -> "Dinero por devolver"
    }

/**
 * Se devuelve por donde entró: con tarjeta (terminal o Stripe) en la terminal; lo pagado en efectivo, con saldo o
 * mezcla de ambos no pasó por la terminal, así que se sugiere efectivo (CAJA-22).
 */
internal fun suggestedRefundMethod(originalMethod: String): RefundMethod {
    val methods = originalMethod.split("+")
    return if (methods.any { it == "terminal" || it == "stripe" }) RefundMethod.TERMINAL else RefundMethod.CASH
}

/** `entrega_efectivo` o `entrega_transferencia`: se cobró en la puerta de un pedido a domicilio. */
internal fun collectedAtDoor(originalMethod: String): Boolean = originalMethod.startsWith("entrega_")

internal fun originalMethodLabel(originalMethod: String): String =
    when (originalMethod) {
        "entrega_efectivo" -> "se cobró en la puerta en efectivo"
        "entrega_transferencia" -> "se cobró en la puerta por transferencia"
        else -> "se pagó con ${originalMethod.replace("+", " y ")}"
    }

/**
 * El aviso de C01: «1 devolución pendiente», con su monto y «Ver». El detalle se abre en una hoja para que las
 * devoluciones no empujen la fila hacia abajo. Sin pendientes solo queda el aviso de la última confirmación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierRefundsEntry(
    state: PendingRefundsUiState,
    canConfirm: Boolean,
    cashOpen: Boolean?,
    colors: OperationalColors,
    onConfirm: (PendingRefund, RefundMethod, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.refunds.isEmpty()) {
        if (state.refunds.isEmpty()) open = false
    }
    if (state.refunds.isEmpty()) {
        CashierRefundsSection(state, canConfirm, cashOpen, colors, onConfirm, modifier)
        return
    }
    val yellow = if (colors.isDark) Color(0xFFFFD15B) else Color(0xFF8A6100)
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(scale = PhysicalPressScale.Small) { open = true }
                .clip(shape)
                .background(yellow.copy(alpha = 0.1f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Undo,
            contentDescription = null,
            tint = yellow,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                refundsEntryTitle(state.refunds.size),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
            )
            Text(
                refundsEntryDetail(state.refunds),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("Ver", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = yellow)
    }
    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 28.dp),
            ) {
                CashierRefundsSection(state, canConfirm, cashOpen, colors, onConfirm)
            }
        }
    }
}

internal fun refundsEntryTitle(count: Int): String =
    if (count == 1) "1 devolución pendiente" else "$count devoluciones pendientes"

/** «$85 · Se quitó un artículo ya pagado» con una sola; con varias, el total. */
internal fun refundsEntryDetail(refunds: List<PendingRefund>): String {
    refunds.singleOrNull()?.let { return "${moneyLabel(it.amount)} · ${refundOriginLabel(it.origin)}" }
    val total = refunds.sumOf { it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
    return "${moneyLabel(com.vaiinilla.app.domain.model.Money.format(total))} en total"
}
/**
 * Devoluciones pendientes (G08, H03): dinero que el negocio debe regresar a mano. Caja confirma cuándo y cómo
 * lo devolvió; en efectivo sale del cajón. Sin pendientes no ocupa lugar.
 */
@Composable
internal fun CashierRefundsSection(
    state: PendingRefundsUiState,
    canConfirm: Boolean,
    cashOpen: Boolean?,
    colors: OperationalColors,
    onConfirm: (PendingRefund, RefundMethod, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.refunds.isEmpty() && state.notice == null) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.refunds.isNotEmpty()) {
            val total =
                state.refunds.sumOf { it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
            Text(
                "DEVOLUCIONES · ${state.refunds.size} · ${moneyLabel(com.vaiinilla.app.domain.model.Money.format(total))}",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
            Text(
                "Dinero que hay que regresarle a alguien. Lo pagado con saldo ya regresó solo a su cartera.",
                fontSize = 12.5.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        state.notice?.let { notice ->
            Text(
                notice,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (state.noticeIsError) RefundCoral else colors.textSecondary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        state.refunds.forEach { refund ->
            RefundRow(refund, state, canConfirm, cashOpen, colors, onConfirm)
        }
    }
}

@Composable
private fun RefundRow(
    refund: PendingRefund,
    state: PendingRefundsUiState,
    canConfirm: Boolean,
    cashOpen: Boolean?,
    colors: OperationalColors,
    onConfirm: (PendingRefund, RefundMethod, String?) -> Unit,
) {
    // El servidor rechaza una devolución en efectivo con la caja cerrada; mejor no ofrecerla (CAJA-22). Lo cobrado en
    // la puerta de un pedido a domicilio lo tiene quien entregó, no el cajón: devolverlo no pide la caja (CASA-09).
    val cashBlocked = cashOpen == false && !collectedAtDoor(refund.originalMethod)
    val shape = RoundedCornerShape(22.dp)
    val suggested = suggestedRefundMethod(refund.originalMethod)
    val busy = state.confirming?.first == refund.id
    // «Otro» no deja rastro de a dónde fue el dinero: se pide el motivo para que el dueño cuadre la caja (EQUIPO-11).
    var askingReason by remember(refund.id) { mutableStateOf(false) }
    var reason by remember(refund.id) { mutableStateOf("") }
    Surface(
        shape = shape,
        color = colors.cardBackground,
        modifier = Modifier.fillMaxWidth().border(1.dp, colors.cardBorder, shape),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text(
                    "Devolver ${moneyLabel(refund.amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = colors.textPrimary,
                )
                Text(
                    "${refundOriginLabel(refund.origin)} · ${originalMethodLabel(refund.originalMethod)}",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (canConfirm) {
                Text(
                    "¿CÓMO SE LO REGRESAS?",
                    fontWeight = FontWeight.Black,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.textSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionPill(
                        label = "Devuelto en ${suggested.label}",
                        enabled = state.confirming == null && !(cashBlocked && suggested == RefundMethod.CASH),
                        loading = busy && state.confirming?.second == suggested,
                        colors = colors,
                        primary = false,
                        modifier = Modifier.weight(1f),
                    ) { onConfirm(refund, suggested, null) }
                    if (suggested != RefundMethod.CASH) {
                        ActionPill(
                            label = "En efectivo",
                            enabled = state.confirming == null && !cashBlocked,
                            loading = busy && state.confirming?.second == RefundMethod.CASH,
                            colors = colors,
                            primary = false,
                            modifier = Modifier.weight(1f),
                        ) { onConfirm(refund, RefundMethod.CASH, null) }
                    }
                    if (suggested != RefundMethod.OTHER) {
                        // No sale del cajón: se puede confirmar aunque la caja esté cerrada.
                        ActionPill(
                            label = "Otro",
                            enabled = state.confirming == null,
                            loading = busy && state.confirming?.second == RefundMethod.OTHER,
                            colors = colors,
                            primary = false,
                            modifier = Modifier.weight(1f),
                        ) { askingReason = true }
                    }
                }
                if (askingReason) {
                    OperationalField(
                        value = reason,
                        onValueChange = { reason = it.take(REASON_MAX) },
                        label = "Motivo",
                        colors = colors,
                        placeholder = "Ej. transferencia al cliente",
                        enabled = state.confirming == null,
                    )
                    ActionPill(
                        label = "Confirmar devolución",
                        enabled = state.confirming == null && reason.isNotBlank(),
                        loading = busy && state.confirming?.second == RefundMethod.OTHER,
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                    ) { onConfirm(refund, RefundMethod.OTHER, reason.trim()) }
                }
                if (cashBlocked) {
                    Text(
                        "Abre la caja para devolver en efectivo.",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

private const val REASON_MAX = 80

private val RefundCoral = Color(0xFFE5645A)
