package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.operational.CloseCashUi

/** Efectivo contado válido: número no negativo con hasta dos decimales, normalizado a "725.50". */
fun normalizeCountedCash(raw: String): String? {
    val text = raw.trim().removePrefix("$").replace(",", ".")
    if (!Regex("""\d+(\.\d{1,2})?""").matches(text)) return null
    val parts = text.split(".")
    val cents = (parts.getOrNull(1) ?: "").padEnd(2, '0')
    return "${parts[0].trimStart('0').ifEmpty { "0" }}.$cents"
}

/**
 * Cierre de caja: el cajero cuenta el efectivo y lo confirma. Si el servidor rechaza el cierre porque
 * hay cuentas a pagar al final sin cobrar, la hoja lo explica y lleva directo a esas cuentas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashCloseSheet(
    state: CloseCashUi,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onOpenAccounts: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        CashCloseContent(state, colors, onDismiss, onConfirm, onOpenAccounts)
    }
}

@Composable
fun CashCloseContent(
    state: CloseCashUi,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onOpenAccounts: (() -> Unit)?,
) {
    var amount by remember { mutableStateOf("") }
    val normalized = normalizeCountedCash(amount)
    val haptics = rememberVaiinillaHaptics()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Cerrar caja", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.textPrimary)
        if (state.blockedByAccounts) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.cardBackground, RoundedCornerShape(18.dp))
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Hay cuentas por cobrar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                )
                Text(
                    "Una mesa o cancha pidió pagar al final y todavía no se cobra. " +
                        "Cóbrala antes de cerrar para no perder ese dinero.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.textSecondary,
                )
            }
            if (onOpenAccounts != null) {
                Button(
                    onClick = {
                        haptics.impact()
                        onDismiss()
                        onOpenAccounts()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.accentLime,
                            contentColor = colors.accentInk,
                        ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Text("Ver cuentas por cobrar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        } else {
            Text(
                "Cuenta el efectivo que hay en la caja y anótalo. La sesión queda cerrada al confirmar.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = colors.textSecondary,
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Efectivo contado") },
                placeholder = { Text("0.00") },
                singleLine = true,
                isError = amount.isNotBlank() && normalized == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            state.errorMessage?.let {
                Text(it, fontSize = 13.sp, color = colors.textSecondary)
            }
            Button(
                onClick = {
                    haptics.impact()
                    normalized?.let(onConfirm)
                },
                enabled = !state.closing && normalized != null,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.accentLime,
                        contentColor = colors.accentInk,
                    ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(
                    if (state.closing) "Cerrando…" else "Cerrar caja con ${moneyLabel(normalized ?: "0.00")}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
        Text(
            "Cancelar",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(vertical = 6.dp),
            color = colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
