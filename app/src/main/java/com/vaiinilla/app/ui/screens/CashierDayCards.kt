package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.PendingRefund
import com.vaiinilla.app.domain.model.RefundMethod
import com.vaiinilla.app.domain.model.RefundOrigin
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.UnpaidAccounts
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.operational.CashierBookingsUiState
import com.vaiinilla.app.ui.operational.CashierRefundsUiState
import com.vaiinilla.app.ui.operational.CashierUnpaidUiState
import com.vaiinilla.app.ui.operational.bookingStatusLabel
import com.vaiinilla.app.ui.operational.cashChange
import com.vaiinilla.app.ui.operational.offersCashRefund
import com.vaiinilla.app.ui.operational.suggestedRefundMethod
import com.vaiinilla.app.ui.operational.unpaidAgeLabel
import com.vaiinilla.app.ui.operational.unpaidSummary
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun refundOriginLabel(origin: RefundOrigin): String =
    when (origin) {
        RefundOrigin.OVERPAID_ACCOUNT -> "Se abonó de más a la cuenta"
        RefundOrigin.REJECTED_ITEM -> "Se quitó un artículo ya pagado"
        RefundOrigin.CANCELLED_ORDER -> "Se canceló un pedido ya pagado"
    }

/** "efectivo+terminal" se muestra como "efectivo y terminal". */
internal fun refundPaidWithLabel(originalMethod: String): String = originalMethod.replace("+", " y ")

private val BOOKING_CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun clockLabel(instant: Instant): String = BOOKING_CLOCK.format(instant.atZone(ZoneId.systemDefault()))

/** Acciones de la tarjeta de reservas; cada una delega en el ViewModel. */
data class CashierBookingsActions(
    val onRefresh: () -> Unit = {},
    val onStartPayment: (Reservation) -> Unit = {},
    val onReceivedChange: (String) -> Unit = {},
    val onSubmitPayment: () -> Unit = {},
    val onDismissPayment: () -> Unit = {},
    val onAskCancel: (Reservation) -> Unit = {},
    val onDismissCancel: () -> Unit = {},
    val onConfirmCancel: () -> Unit = {},
    val onNoticeShown: () -> Unit = {},
)

/**
 * Devoluciones pendientes de Caja: dinero que el negocio debe regresar a mano. Cada una se confirma
 * después de pedir confirmación, porque en efectivo sale del cajón.
 */
@Composable
fun CashierRefundsCard(
    state: CashierRefundsUiState,
    enabled: Boolean,
    onConfirm: (PendingRefund, RefundMethod) -> Unit,
    onNoticeShown: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val haptics = rememberVaiinillaHaptics()
    var askingFor by remember { mutableStateOf<Pair<PendingRefund, RefundMethod>?>(null) }

    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(2_200)
            onNoticeShown()
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.notice?.let {
                Text(it, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary)
            }
            (state.errorMessage ?: state.loadError)?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            state.refunds.forEachIndexed { index, refund ->
                val busy = state.confirming?.refundId == refund.id
                val suggested = suggestedRefundMethod(refund.originalMethod)
                Row(
                    modifier = Modifier.fillMaxWidth().arrive(index, key = refund.id),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Devolver ${moneyLabel(refund.amount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary,
                        )
                        Text(
                            "${refundOriginLabel(refund.origin)} · " +
                                "se pagó con ${refundPaidWithLabel(refund.originalMethod)}",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary,
                        )
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    haptics.selection()
                                    askingFor = refund to suggested
                                },
                                enabled = enabled && state.confirming == null,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = colors.textPrimary,
                                        contentColor = colors.cardBackground,
                                    ),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Devuelto en ${suggested.wire}", fontWeight = FontWeight.Bold)
                            }
                            if (offersCashRefund(refund.originalMethod)) {
                                OutlinedButton(
                                    onClick = {
                                        haptics.selection()
                                        askingFor = refund to RefundMethod.CASH
                                    },
                                    enabled = enabled && state.confirming == null,
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text("En efectivo")
                                }
                            }
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    askingFor?.let { (refund, method) ->
        ConfirmActionSheet(
            title = "Confirmar devolución",
            message =
                "Devolviste ${moneyLabel(refund.amount)} en ${method.wire}. " +
                    "Esta confirmación no se puede deshacer.",
            confirmLabel = "Sí, devuelto",
            cancelLabel = "Cancelar",
            colors = colors,
            onConfirm = {
                askingFor = null
                haptics.success()
                onConfirm(refund, method)
            },
            onDismiss = { askingFor = null },
        )
    }
}

/** Cuentas abiertas que nadie ha cobrado; quien se fue sin pagar sale resaltado. Sin cuentas, no ocupa lugar. */
@Composable
fun CashierUnpaidAccountsCard(state: CashierUnpaidUiState) {
    val colors = rememberOperationalColors()
    val accounts: UnpaidAccounts? = state.accounts
    val rows = accounts?.accounts.orEmpty()
    if (rows.isEmpty() && state.loadError == null) return

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Cuentas sin cobrar", fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary)
            if (accounts != null && rows.isNotEmpty()) {
                Text(
                    unpaidSummary(accounts.total, rows.size, accounts.abandonedCount),
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )
                rows.forEachIndexed { index, account ->
                    val clients = if (account.clients.isEmpty()) "" else account.clients.joinToString(", ")
                    val details =
                        listOfNotNull(
                            "${account.orders} ${if (account.orders == 1) "pedido" else "pedidos"}",
                            clients.takeIf { it.isNotEmpty() },
                            unpaidAgeLabel(account.hours),
                        ).joinToString(" · ")
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .arrive(index, key = account.spaceId)
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                account.spaceName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                moneyLabel(account.total),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.textPrimary,
                            )
                        }
                        Text(details, fontSize = 12.5.sp, color = colors.textSecondary)
                        if (account.abandoned) {
                            Text(
                                "Ya se liberó: se fue sin pagar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = colors.textPrimary,
                            )
                        }
                    }
                }
            }
            state.loadError?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Reservas de cancha de hoy: cobrar en efectivo o liberar la cancha que no llegó. Sin canchas, no se muestra. */
@Composable
fun CashierBookingsCard(
    state: CashierBookingsUiState,
    enabled: Boolean,
    actions: CashierBookingsActions,
) {
    if (!state.hasCourts) return
    val colors = rememberOperationalColors()
    val haptics = rememberVaiinillaHaptics()

    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(2_200)
            actions.onNoticeShown()
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Reservas de hoy", fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary)
                    Text(
                        "Cobra o libera las canchas que apartaron tus clientes.",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                    )
                }
                TextButton(
                    onClick = {
                        haptics.selection()
                        actions.onRefresh()
                    },
                    enabled = !state.loading,
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Actualizar")
                }
            }
            state.notice?.let {
                Text(it, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary)
            }
            state.cancelError?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            state.loadError?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            if (!state.loaded && state.loading) {
                Text("Consultando reservas…", fontSize = 13.sp, color = colors.textSecondary)
            }
            if (state.loaded && state.loadError == null && state.bookings.isEmpty()) {
                Text("No hay reservas para hoy.", fontSize = 13.sp, color = colors.textSecondary)
            }
            state.bookings.forEachIndexed { index, booking ->
                CourtBookingRow(
                    booking = booking,
                    index = index,
                    enabled = enabled && !state.cancelBusy && !state.paymentBusy,
                    actions = actions,
                )
            }
        }
    }

    state.paying?.let { reservation ->
        BookingPaymentSheet(reservation = reservation, state = state, actions = actions)
    }
    state.canceling?.let { reservation ->
        ConfirmActionSheet(
            title = "Liberar cancha",
            message =
                "¿Cancelar la reserva de ${reservation.customerName ?: "el cliente"} en " +
                    "${reservation.courtName ?: "la cancha"}, de ${clockLabel(reservation.start)} a " +
                    "${clockLabel(reservation.end)}? El horario queda libre y el cliente verá su pedido como " +
                    "cancelado por el establecimiento.",
            confirmLabel = "Sí, cancelar reserva",
            cancelLabel = "No, conservar",
            colors = colors,
            onConfirm = actions.onConfirmCancel,
            onDismiss = actions.onDismissCancel,
        )
    }
}

@Composable
private fun CourtBookingRow(
    booking: Reservation,
    index: Int,
    enabled: Boolean,
    actions: CashierBookingsActions,
) {
    val colors = rememberOperationalColors()
    val haptics = rememberVaiinillaHaptics()
    val unpaid = booking.state == ReservationState.PENDING_PAYMENT
    val paid =
        booking.state == ReservationState.CONFIRMED ||
            booking.state == ReservationState.IN_PROGRESS ||
            booking.state == ReservationState.FINISHED
    val expiry =
        booking.holdExpiresAt
            ?.takeIf { unpaid }
            ?.let { " · Vence ${clockLabel(it)}" }
            .orEmpty()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .arrive(index, key = booking.id)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${booking.courtName ?: "Cancha"} · ${clockLabel(booking.start)}–${clockLabel(booking.end)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = if (unpaid) colors.accentLime else colors.pillBackground,
            ) {
                Text(
                    bookingStatusLabel(booking.state),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (unpaid) colors.accentInk else colors.textPrimary,
                )
            }
        }
        Text(
            "${booking.customerName ?: "Cliente"} · ${moneyLabel(booking.amount)}$expiry",
            fontSize = 12.5.sp,
            color = colors.textSecondary,
        )
        if (unpaid) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        haptics.selection()
                        actions.onStartPayment(booking)
                    },
                    enabled = enabled,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Cobrar", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        haptics.selection()
                        actions.onAskCancel(booking)
                    },
                    enabled = enabled,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Liberar cancha / Cancelar", color = MaterialTheme.colorScheme.error)
                }
            }
        } else if (paid) {
            Text(
                "Ya está pagada: no se cancela desde aquí hasta definir el reembolso.",
                fontSize = 12.sp,
                color = colors.textSecondary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingPaymentSheet(
    reservation: Reservation,
    state: CashierBookingsUiState,
    actions: CashierBookingsActions,
) {
    val colors = rememberOperationalColors()
    val haptics = rememberVaiinillaHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val change = cashChange(state.received, reservation.amount)

    ModalBottomSheet(
        onDismissRequest = actions.onDismissPayment,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Cobrar reserva en efectivo",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = colors.textPrimary,
            )
            Text(
                "${reservation.courtName ?: "Cancha"} · ${moneyLabel(reservation.amount)}",
                fontSize = 14.sp,
                color = colors.textSecondary,
            )
            OutlinedTextField(
                value = state.received,
                onValueChange = actions.onReceivedChange,
                label = { Text("Efectivo recibido") },
                singleLine = true,
                enabled = !state.paymentBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { change?.let { Text("Cambio: ${moneyLabel(it.toPlainString())}") } },
                modifier = Modifier.fillMaxWidth(),
            )
            state.paymentError?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    haptics.selection()
                    actions.onSubmitPayment()
                },
                enabled = !state.paymentBusy && change != null,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.textPrimary,
                        contentColor = colors.cardBackground,
                    ),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (state.paymentBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.cardBackground,
                    )
                } else {
                    Text("Cobrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Hoja de confirmación para acciones que mueven dinero o liberan un horario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfirmActionSheet(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    colors: OperationalColors,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = colors.textPrimary)
            Text(message, fontSize = 14.sp, color = colors.textSecondary)
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.textPrimary,
                        contentColor = colors.cardBackground,
                    ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(cancelLabel)
            }
        }
    }
}
