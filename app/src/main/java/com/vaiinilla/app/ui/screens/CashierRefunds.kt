package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.repository.PendingRefund
import com.vaiinilla.app.domain.repository.RefundMethod
import com.vaiinilla.app.ui.components.moneyLabel
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
            Text("Devoluciones pendientes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
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
    // El servidor rechaza una devolución en efectivo con la caja cerrada; mejor no ofrecerla (CAJA-22).
    val cashBlocked = cashOpen == false
    val shape = RoundedCornerShape(18.dp)
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                )
                Text(
                    "${refundOriginLabel(refund.origin)} · se pagó con ${refund.originalMethod.replace("+", " y ")}",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (canConfirm) {
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
