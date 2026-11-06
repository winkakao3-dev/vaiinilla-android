package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

private const val CODE_LENGTH = 4

/** El pedido de room service listo, partido en los renglones que lleva el mesero: «1× Café» → «1 × Café». */
internal fun deliveryLines(itemsSummary: String): List<String> =
    itemsSummary
        .split(", ")
        .map { it.trim().replaceFirst(Regex("^(\\d+)\\s*[×x]\\s*"), "$1 × ") }
        .filter { it.isNotEmpty() }

/**
 * S02: entrega a la habitación. Se abre sobre el tablero del mesero cuando el pedido listo va a una habitación.
 * El huésped puede dictar el código de su pedido, o se entrega en la puerta sin código.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelDeliverSheet(
    room: String,
    itemsSummary: String,
    acting: Boolean,
    onConfirm: (code: String?) -> Unit,
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
    onConfirm: (code: String?) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    note: String? = null,
    initialCode: String = "",
) {
    val colors = LocalVaiinillaColors.current
    var code by rememberSaveable { mutableStateOf(initialCode) }
    val partial = code.isNotEmpty() && code.length < CODE_LENGTH
    HotelStaffPage(
        kicker = if (floor != null) "Listo para entregar · Piso $floor" else "Listo para entregar",
        title = room,
        modifier = modifier,
        titleSize = 48,
        onBack = onBack,
        bottomBar = {
            HotelButton(
                label =
                    when {
                        acting -> "Entregando…"
                        code.length == CODE_LENGTH -> "Confirmar entrega con el código"
                        partial -> "El código lleva $CODE_LENGTH dígitos"
                        else -> "Entregado en la puerta · sin código"
                    },
                onClick = { onConfirm(code.takeIf { it.length == CODE_LENGTH }) },
                kind = HotelButtonKind.Lime,
                enabled = !acting && !partial,
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
        HotelCard(padding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp)) {
            HotelCaps("CÓDIGO QUE TE DICE EL HUÉSPED")
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(CODE_LENGTH) { index ->
                    val digit = code.getOrNull(index)
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(shape)
                                .background(colors.paper)
                                .border(1.5.dp, if (digit != null) colors.accent else colors.ink.copy(alpha = 0.2f), shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            digit?.toString() ?: "_",
                            color = if (digit != null) colors.ink else colors.muted,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
        Keypad(
            onDigit = { if (code.length < CODE_LENGTH) code += it },
            onDelete = { code = code.dropLast(1) },
        )
    }
}

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit -> Key(digit.toString(), "Dígito $digit", { onDigit(digit) }, Modifier.weight(1f)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f))
            Key("0", "Dígito 0", { onDigit('0') }, Modifier.weight(1f))
            Key("⌫", "Borrar", onDelete, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Key(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            modifier
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.paper2)
                .physicalPress(onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
