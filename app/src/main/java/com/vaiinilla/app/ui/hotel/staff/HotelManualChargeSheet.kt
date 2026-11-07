package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** Cargo manual a la cuenta (minibar, spa, lo que recepción capture a mano). Sin marco en Doop: mismos componentes que S04. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HotelManualChargeSheet(
    stay: Stay,
    busy: Boolean,
    error: String?,
    onSave: (concept: String, amount: String, category: ChargeCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.paper,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        HotelManualChargeContent(
            stay = stay,
            busy = busy,
            error = error,
            onSave = onSave,
            modifier = Modifier.imePadding().navigationBarsPadding(),
        )
    }
}

@Composable
internal fun HotelManualChargeContent(
    stay: Stay,
    busy: Boolean,
    error: String?,
    onSave: (concept: String, amount: String, category: ChargeCategory) -> Unit,
    modifier: Modifier = Modifier,
    initialCategory: ChargeCategory = ChargeCategory.MINIBAR,
) {
    val colors = LocalVaiinillaColors.current
    val operational = rememberOperationalColors()
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    var concept by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    val parsed = parseAmount(amount)
    val label = concept.trim().ifBlank { category.label }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Cargo a la habitación", color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            "${stay.roomName ?: "Habitación"} · ${stay.guestName}",
            color = colors.ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChargeCategory.entries.forEach { option ->
                HotelChip(option.label, selected = option == category, onClick = { category = option })
            }
        }
        OperationalField(
            value = concept,
            onValueChange = { concept = it },
            label = "Concepto",
            colors = operational,
            placeholder = category.label,
        )
        OperationalField(
            value = amount,
            onValueChange = { amount = it },
            label = "Importe",
            colors = operational,
            placeholder = "90.00",
            keyboardType = KeyboardType.Decimal,
            isError = amount.isNotBlank() && parsed == null,
            supporting =
                if (amount.isNotBlank() &&
                    parsed == null
                ) {
                    "Escribe un importe mayor a cero, con hasta dos decimales."
                } else {
                    null
                },
        )
        error?.let { HotelNotice(it) }
        HotelButton(
            label = if (busy) "Agregando…" else "Agregar a la cuenta",
            onClick = { onSave(label, parsed!!, category) },
            kind = HotelButtonKind.Lime,
            enabled = parsed != null && !busy,
            height = 56.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
