package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.hotel.CheckoutMethod
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.hotel.hotelViewModel
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/**
 * S04: Check-out y cuenta. La app no cobra: muestra el total y registra con qué pagó el huésped en recepción.
 * Comparte el [HotelReceptionViewModel] con Recepción, que es quien la abre.
 */
@Composable
fun HotelCheckoutScreen(
    stayId: String,
    onDone: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HotelReceptionViewModel = hotelViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(stayId) { viewModel.loadDetail(stayId) }
    HotelCheckoutContent(
        stay = state.detail?.takeIf { it.id == stayId },
        loading = state.detailLoading,
        method = state.method,
        busy = state.busy,
        error = state.error,
        onMethod = viewModel::setMethod,
        onConfirm = { viewModel.checkout(stayId, onDone) },
        onBack = onDone,
        modifier = modifier,
    )
}

@Composable
internal fun HotelCheckoutContent(
    stay: Stay?,
    loading: Boolean,
    method: CheckoutMethod,
    busy: Boolean,
    error: String?,
    onMethod: (CheckoutMethod) -> Unit,
    onConfirm: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    if (stay == null) {
        HotelStaffPage(
            kicker = "Recepción · Check-out",
            title = "Cuenta",
            modifier = modifier,
            titleSize = 36,
            onBack = onBack,
        ) {
            HotelNotice(
                text = error ?: if (loading) "Abriendo la cuenta…" else "No encontramos esa cuenta.",
                tone = if (error != null) colors.coral else colors.muted,
            )
        }
        return
    }
    val bill = checkoutBreakdown(stay)
    val nights = if (bill.nights == 1) "1 noche" else "${bill.nights} noches"
    HotelStaffPage(
        kicker = "Recepción · Check-out",
        title = stay.guestName,
        subtitle =
            listOfNotNull(
                stay.roomName,
                hotelDateRange(stay.checkIn, stay.checkOut).ifBlank {
                    null
                },
                nights,
            ).joinToString(" · "),
        modifier = modifier,
        titleSize = 36,
        onBack = onBack,
        bottomBar = {
            HotelButton(
                label = if (busy) "Cerrando…" else "Cerrar cuenta",
                onClick = onConfirm,
                kind = HotelButtonKind.Lime,
                enabled = !busy,
                height = 60.dp,
                fontSize = 16,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        HotelCard {
            HotelCaps("ESTANCIA", modifier = Modifier.padding(bottom = 6.dp))
            HotelRow(
                left = "$nights × ${hotelMoneyShort(bill.rate)}",
                right = hotelMoney(bill.stayAmount),
            )
        }
        HotelCard {
            HotelCaps("CARGOS A LA HABITACIÓN", modifier = Modifier.padding(bottom = 6.dp))
            if (bill.roomServiceCount > 0) {
                HotelRow(left = "Room service (${bill.roomServiceCount})", right = hotelMoney(bill.roomServiceTotal))
            }
            bill.charges.forEach { (label, amount) -> HotelRow(left = label, right = hotelMoney(amount)) }
            if (bill.roomServiceCount == 0 && bill.charges.isEmpty()) {
                HotelRow(left = "Sin cargos extra", right = null, muted = true)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Total a cobrar", color = colors.muted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                hotelMoney(bill.total),
                color = colors.ink,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.8).sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            // El servidor solo acepta efectivo o tarjeta en el check-out.
            CheckoutMethod.entries.filter { it != CheckoutMethod.TRANSFER }.forEach { option ->
                PayOption(
                    option.label,
                    selected = option == method,
                    onClick = { onMethod(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        error?.let { HotelNotice(it) }
    }
}

/** La forma de pago de Doop: papel 2 con el contorno lima cuando está elegida. */
@Composable
private fun PayOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier =
            modifier
                .height(52.dp)
                .clip(shape)
                .background(if (selected) colors.accent.copy(alpha = 0.08f) else colors.paper2)
                .border(1.5.dp, if (selected) colors.accent else androidx.compose.ui.graphics.Color.Transparent, shape)
                .physicalPress(onClick = onClick)
                .semantics { role = Role.RadioButton },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.ink, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}
