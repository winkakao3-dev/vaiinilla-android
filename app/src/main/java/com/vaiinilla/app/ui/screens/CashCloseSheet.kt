package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.operational.CloseCashUi
import java.math.BigDecimal

/** Efectivo contado válido: número no negativo con hasta dos decimales, normalizado a "725.50". */
fun normalizeCountedCash(raw: String): String? {
    val text = raw.trim().removePrefix("$").replace(",", ".")
    if (!Regex("""\d+(\.\d{1,2})?""").matches(text)) return null
    val parts = text.split(".")
    val cents = (parts.getOrNull(1) ?: "").padEnd(2, '0')
    return "${parts[0].trimStart('0').ifEmpty { "0" }}.$cents"
}

/**
 * Cierre de caja: el cajero cuenta el efectivo y lo confirma deslizando, porque no se deshace. Si el
 * servidor rechaza el cierre porque hay cuentas a pagar al final sin cobrar, la hoja lo explica y lleva
 * directo a esas cuentas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashCloseSheet(
    state: CloseCashUi,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onOpenAccounts: (() -> Unit)?,
    ordersToExpire: Int = 0,
    amountToExpire: BigDecimal = BigDecimal.ZERO,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        CashCloseContent(state, colors, onDismiss, onConfirm, onOpenAccounts, ordersToExpire, amountToExpire)
    }
}

@Composable
fun CashCloseContent(
    state: CloseCashUi,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onOpenAccounts: (() -> Unit)?,
    ordersToExpire: Int = 0,
    amountToExpire: BigDecimal = BigDecimal.ZERO,
) {
    var amount by remember { mutableStateOf("") }
    val normalized = normalizeCountedCash(amount)
    val haptics = rememberVaiinillaHaptics()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Cerrar caja", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.textPrimary)
            Box(
                modifier =
                    Modifier
                        .physicalPress(enabled = !state.closing, scale = PhysicalPressScale.Small) { onDismiss() }
                        .size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        AnimatedContent(
            targetState = state.blockedByAccounts,
            transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
            label = "cash-close-step",
        ) { blocked ->
            if (blocked) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Una mesa o cancha pidió pagar al final y todavía no se cobra. " +
                            "Cóbrala antes de cerrar para no perder ese dinero.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = colors.textSecondary,
                    )
                    if (onOpenAccounts != null) {
                        ActionPill(
                            label = "Ver cuentas por cobrar",
                            enabled = true,
                            loading = false,
                            colors = colors,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            haptics.impact()
                            onDismiss()
                            onOpenAccounts()
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Cuenta el efectivo que hay en la caja. Al cerrar termina la sesión de caja.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = colors.textSecondary,
                    )
                    expiringOrdersNotice(ordersToExpire, amountToExpire)?.let {
                        Text(
                            it,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                    }
                    val invalid = amount.isNotBlank() && normalized == null
                    OperationalField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Efectivo contado",
                        colors = colors,
                        placeholder = "0.00",
                        supporting = if (invalid) "Solo números, con hasta dos decimales" else null,
                        isError = invalid,
                        enabled = !state.closing,
                        keyboardType = KeyboardType.Decimal,
                    )
                    state.errorMessage?.let {
                        Text(it, fontSize = 13.sp, color = colors.textSecondary)
                    }
                    SlideToConfirm(
                        label =
                            when {
                                state.closing -> "Cerrando…"
                                normalized == null -> "Escribe el efectivo contado"
                                else -> "Desliza para cerrar con ${moneyLabel(normalized)}"
                            },
                        enabled = normalized != null,
                        busy = state.closing,
                        colors = colors,
                    ) { normalized?.let(onConfirm) }
                }
            }
        }
    }
}

/** Lo que se pierde al cerrar: el servidor expira los pedidos en efectivo que siguen por cobrar. */
internal fun expiringOrdersNotice(
    count: Int,
    amount: BigDecimal,
): String? =
    when (count) {
        0 -> null
        1 -> "Hay 1 pedido por cobrar (${moneyLabel(Money.format(amount))}) que expirará al cerrar."
        else -> "Hay $count pedidos por cobrar (${moneyLabel(Money.format(amount))}) que expirarán al cerrar."
    }

/** Abrir la caja: el cajero cuenta el efectivo del cajón y lo escribe; ese es el fondo con el que arranca el turno. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashOpenSheet(
    opening: Boolean,
    errorMessage: String?,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    suggestedAmount: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        CashOpenContent(opening, errorMessage, colors, onConfirm, suggestedAmount)
    }
}

@Composable
fun CashOpenContent(
    opening: Boolean,
    errorMessage: String?,
    colors: OperationalColors,
    onConfirm: (String) -> Unit,
    suggestedAmount: String? = null,
) {
    // El fondo del servidor ya escrito: si cuadra con el cajón se confirma en un toque (Kao, 25 oct).
    var amount by remember(suggestedAmount) { mutableStateOf(suggestedAmount.orEmpty()) }
    val normalized = normalizeCountedCash(amount)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Abrir caja", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.textPrimary)
        Text(
            "Cuenta el efectivo que hay en el cajón. Con ese fondo empieza el turno y contra él se compara el cierre.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = colors.textSecondary,
        )
        val invalid = amount.isNotBlank() && normalized == null
        OperationalField(
            value = amount,
            onValueChange = { amount = it },
            label = "Efectivo en el cajón",
            colors = colors,
            placeholder = "0.00",
            supporting = if (invalid) "Solo números, con hasta dos decimales" else null,
            isError = invalid,
            enabled = !opening,
            keyboardType = KeyboardType.Decimal,
        )
        errorMessage?.let {
            Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        }
        ActionPill(
            label =
                when {
                    opening -> "Abriendo…"
                    normalized == null -> "Escribe el efectivo del cajón"
                    else -> "Abrir con ${moneyLabel(normalized)}"
                },
            enabled = normalized != null,
            loading = opening,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        ) { normalized?.let(onConfirm) }
    }
}
