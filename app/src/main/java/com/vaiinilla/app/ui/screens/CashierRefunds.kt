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
        else -> "Dinero por devolver"
    }

/** Se devuelve por donde entró; lo pagado con saldo o mezclado, Caja lo devuelve con la terminal o en efectivo. */
internal fun suggestedRefundMethod(originalMethod: String): RefundMethod =
    if (originalMethod == "efectivo") RefundMethod.CASH else RefundMethod.TERMINAL

/**
 * Devoluciones pendientes (G08, H03): dinero que el negocio debe regresar a mano. Caja confirma cuándo y cómo
 * lo devolvió; en efectivo sale del cajón. Sin pendientes no ocupa lugar.
 */
@Composable
internal fun CashierRefundsSection(
    state: PendingRefundsUiState,
    canConfirm: Boolean,
    colors: OperationalColors,
    onConfirm: (PendingRefund, RefundMethod) -> Unit,
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
            RefundRow(refund, state, canConfirm, colors, onConfirm)
        }
    }
}

@Composable
private fun RefundRow(
    refund: PendingRefund,
    state: PendingRefundsUiState,
    canConfirm: Boolean,
    colors: OperationalColors,
    onConfirm: (PendingRefund, RefundMethod) -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val suggested = suggestedRefundMethod(refund.originalMethod)
    val busy = state.confirming?.first == refund.id
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
                        enabled = state.confirming == null,
                        loading = busy && state.confirming?.second == suggested,
                        colors = colors,
                        primary = false,
                        modifier = Modifier.weight(1f),
                    ) { onConfirm(refund, suggested) }
                    if (suggested != RefundMethod.CASH) {
                        ActionPill(
                            label = "En efectivo",
                            enabled = state.confirming == null,
                            loading = busy && state.confirming?.second == RefundMethod.CASH,
                            colors = colors,
                            primary = false,
                            modifier = Modifier.weight(1f),
                        ) { onConfirm(refund, RefundMethod.CASH) }
                    }
                }
            }
        }
    }
}

private val RefundCoral = Color(0xFFE5645A)
