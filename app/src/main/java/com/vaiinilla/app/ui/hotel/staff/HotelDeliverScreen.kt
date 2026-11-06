package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** El pedido de room service listo, partido en los renglones que lleva el mesero: «1× Café» → «1 × Café». */
internal fun deliveryLines(itemsSummary: String): List<String> =
    itemsSummary
        .split(", ")
        .map { it.trim().replaceFirst(Regex("^(\\d+)\\s*[×x]\\s*"), "$1 × ") }
        .filter { it.isNotEmpty() }

/**
 * S02: entrega a la habitación. Se abre sobre el tablero del mesero cuando el pedido listo va a una habitación.
 * El mesero lo deja en la puerta y lo marca entregado: la entrega de habitación no lleva código.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelDeliverSheet(
    room: String,
    itemsSummary: String,
    acting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    floor: Int? = null,
    charge: String? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalVaiinillaColors.current.paper,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        HotelDeliverContent(
            room = room,
            floor = floor,
            lines = deliveryLines(itemsSummary),
            charge = charge,
            acting = acting,
            onConfirm = onConfirm,
            onBack = onDismiss,
        )
    }
}

@Composable
internal fun HotelDeliverContent(
    room: String,
    floor: Int?,
    lines: List<String>,
    charge: String?,
    acting: Boolean,
    onConfirm: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    HotelStaffPage(
        kicker = if (floor != null) "Listo para entregar · Piso $floor" else "Listo para entregar",
        title = room,
        modifier = modifier,
        titleSize = 48,
        onBack = onBack,
        bottomBar = {
            HotelButton(
                label = if (acting) "Entregando…" else "Entregado en la puerta",
                onClick = onConfirm,
                kind = HotelButtonKind.Lime,
                enabled = !acting,
                height = 58.dp,
                fontSize = 16,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        HotelCard {
            HotelCaps("LLEVAS")
            Column(modifier = Modifier.padding(top = 2.dp)) {
                lines.forEach { HotelRow(left = it, right = null) }
                if (note != null) HotelRow(left = "Nota: $note", right = null, muted = true)
            }
            if (charge != null) {
                HotelDivider(modifier = Modifier.padding(vertical = 2.dp))
                HotelRow(left = "Cargo a la habitación", right = hotelMoney(charge))
            }
        }
    }
}
