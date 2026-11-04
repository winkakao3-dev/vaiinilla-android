package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.repository.PendingRefund
import com.vaiinilla.app.domain.repository.RefundMethod
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.operational.PendingRefundsUiState
import com.vaiinilla.app.ui.screens.admin.AdminCapsLabel
import com.vaiinilla.app.ui.screens.admin.AdminPanel
import com.vaiinilla.app.ui.screens.admin.AdminPrimaryButton
import com.vaiinilla.app.ui.screens.admin.AdminSectionPage
import com.vaiinilla.app.ui.screens.admin.AdminSegmented

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
 * El aviso de C01: «1 devolución pendiente», con su monto y «Ver». El detalle se abre en su página para que las
 * devoluciones no empujen la fila hacia abajo. Sin pendientes solo queda el aviso de la última confirmación.
 */
@Composable
internal fun CashierRefundsEntry(
    state: PendingRefundsUiState,
    canConfirm: Boolean,
    cashOpen: Boolean?,
    colors: OperationalColors,
    onConfirm: (PendingRefund, RefundMethod, String?) -> Unit,
    modifier: Modifier = Modifier,
    orders: List<OrderDetail> = emptyList(),
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
        Dialog(
            onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            CashierRefundsPage(state, canConfirm, cashOpen, colors, orders, onConfirm) { open = false }
        }
    }
}

/** Devoluciones como el marco C05: una página con cada dinero por regresar. */
@Composable
internal fun CashierRefundsPage(
    state: PendingRefundsUiState,
    canConfirm: Boolean,
    cashOpen: Boolean?,
    colors: OperationalColors,
    orders: List<OrderDetail>,
    onConfirm: (PendingRefund, RefundMethod, String?) -> Unit,
    onBack: () -> Unit,
) {
    val total = state.refunds.sumOf { it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
    AdminSectionPage(
        title = "Devoluciones",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
        subtitle = "Dinero que hay que regresarle a alguien. Lo pagado con saldo ya regresó solo a su cartera.",
    ) {
        item(key = "pendientes") {
            AdminCapsLabel(
                "Pendientes · ${state.refunds.size} · ${moneyLabel(
                    com.vaiinilla.app.domain.model.Money
                        .format(total),
                )}",
                colors,
            )
        }
        items(state.refunds, key = { it.id }) { refund ->
            RefundRow(
                refund,
                state,
                canConfirm,
                cashOpen,
                colors,
                orders.firstOrNull {
                    it.summary.id == refund.orderId
                },
                onConfirm,
            )
        }
    }
}

internal fun refundsEntryTitle(count: Int): String =
    if (count == 1) "1 devolución pendiente" else "$count devoluciones pendientes"

/** «$85 · Se quitó un artículo ya pagado» con una sola; con varias, el total. */
internal fun refundsEntryDetail(refunds: List<PendingRefund>): String {
    refunds.singleOrNull()?.let { return "${moneyLabel(it.amount)} · ${refundOriginLabel(it.origin)}" }
    val total = refunds.sumOf { it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
    return "${moneyLabel(
        com.vaiinilla.app.domain.model.Money
            .format(total),
    )} en total"
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
                "DEVOLUCIONES · ${state.refunds.size} · ${moneyLabel(
                    com.vaiinilla.app.domain.model.Money
                        .format(total),
                )}",
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
            RefundRow(refund, state, canConfirm, cashOpen, colors, null, onConfirm)
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
    order: OrderDetail?,
    onConfirm: (PendingRefund, RefundMethod, String?) -> Unit,
) {
    // El servidor rechaza una devolución en efectivo con la caja cerrada; mejor no ofrecerla (CAJA-22). Lo cobrado en
    // la puerta de un pedido a domicilio lo tiene quien entregó, no el cajón: devolverlo no pide la caja (CASA-09).
    val cashBlocked = cashOpen == false && !collectedAtDoor(refund.originalMethod)
    val suggested = suggestedRefundMethod(refund.originalMethod)
    val busy = state.confirming?.first == refund.id
    // «Otro» no deja rastro de a dónde fue el dinero: se pide el motivo para que el dueño cuadre la caja (EQUIPO-11).
    var chosen by remember(refund.id) { mutableStateOf(if (cashBlocked) RefundMethod.OTHER else suggested) }
    var reason by remember(refund.id) { mutableStateOf("") }
    val amount = moneyLabel(refund.amount)
    val title =
        when {
            order != null ->
                listOfNotNull(
                    "Pedido #${order.summary.folio}",
                    staffCustomerName(order),
                ).joinToString(" · ")
            refund.origin == "reserva_cancelada" -> "Reserva"
            else -> "Dinero por devolver"
        }
    AdminPanel(colors, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = colors.textPrimary)
                Text(
                    listOfNotNull(
                        refundOriginLabel(refund.origin),
                        receiptLocalTime(refund.createdAt),
                    ).joinToString(" · "),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(amount, fontWeight = FontWeight.Black, fontSize = 20.sp, color = colors.textPrimary)
        }
        if (canConfirm) {
            AdminCapsLabel("¿Cómo se lo regresas?", colors, Modifier.padding(top = 12.dp, bottom = 6.dp), inset = false)
            val options =
                listOfNotNull(
                    (suggested to "En ${suggested.label.lowercase()}").takeIf { suggested != RefundMethod.OTHER },
                    (RefundMethod.CASH to "En efectivo").takeIf { suggested != RefundMethod.CASH },
                    RefundMethod.OTHER to "Otro",
                )
            AdminSegmented(
                options = options,
                selected = chosen,
                colors = colors,
                enabled = state.confirming == null,
                onSelect = { chosen = it },
            )
            Spacer(Modifier.height(10.dp))
            if (chosen == RefundMethod.OTHER) {
                OperationalField(
                    value = reason,
                    onValueChange = { reason = it.take(REASON_MAX) },
                    label = "Motivo",
                    colors = colors,
                    placeholder = "Ej. transferencia al cliente",
                    enabled = state.confirming == null,
                )
                Spacer(Modifier.height(10.dp))
            } else if (chosen == RefundMethod.CASH && !collectedAtDoor(refund.originalMethod)) {
                Text(
                    if (cashBlocked) {
                        "Abre la caja para devolver en efectivo."
                    } else {
                        "Sale de la caja: el efectivo esperado baja $amount."
                    },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            val ready =
                state.confirming == null &&
                    when (chosen) {
                        RefundMethod.OTHER -> reason.isNotBlank()
                        RefundMethod.CASH -> !cashBlocked
                        else -> true
                    }
            AdminPrimaryButton(
                title =
                    if (chosen == RefundMethod.OTHER && reason.isBlank()) {
                        "Escribe el motivo para confirmar"
                    } else {
                        "Ya se lo di · $amount"
                    },
                colors = colors,
                busy = busy,
                enabled = ready,
                fill = colors.accentLime,
                contentColor = colors.accentInk,
            ) { onConfirm(refund, chosen, reason.trim().takeIf { chosen == RefundMethod.OTHER }) }
        }
    }
}

private const val REASON_MAX = 80

private val RefundCoral = Color(0xFFE5645A)
