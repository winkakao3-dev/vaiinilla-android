package com.vaiinilla.app.ui.screens.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField

/** Lo que la sección de Caja necesita del resto del modo. */
class AdminCashActions(
    val onOpenCash: (String, () -> Unit) -> Unit,
    val onCloseCash: (String, () -> Unit) -> Unit,
)

/**
 * Caja: como el POS de la web con una cuenta de administración. Abre y cierra la sesión de caja; tomar pedidos vive
 * en Pedidos, y cobrar y entregar quedan para una cuenta de Caja.
 */
@Composable
internal fun AdminCashSection(
    state: AdminUiState,
    colors: OperationalColors,
    readOnly: Boolean,
    actions: AdminCashActions,
    onBack: () -> Unit,
) {
    var opening by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val open = state.cashSessionId != null
    val enabled = !readOnly && !state.saving

    AdminSectionPage(
        title = "Caja",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.notice != null && state.noticeIsError,
        onBack = onBack,
    ) {
        item {
            AdminCard(colors) {
                AdminSectionTitle("Sesión de caja", colors)
                when {
                    !state.cashLoaded -> AdminHint("Consultando la caja…", colors)
                    open -> AdminValueRow("Estado", "Abierta", colors, strong = true)
                    else -> AdminValueRow("Estado", "Cerrada", colors, strong = true)
                }
                if (state.cashLoaded && !readOnly) {
                    if (open) {
                        ActionPill(
                            label = "Cerrar caja",
                            enabled = enabled,
                            loading = false,
                            colors = colors,
                            primary = false,
                        ) {
                            closing =
                                true
                        }
                    } else {
                        ActionPill(label = "Abrir caja", enabled = enabled, loading = false, colors = colors) {
                            opening =
                                true
                        }
                    }
                }
            }
        }
        item {
            AdminHint(
                "Administración controla la sesión y consulta pedidos; por seguridad, los cobros y entregas requieren una cuenta con rol de Caja.",
                colors,
            )
        }
    }

    val error = state.notice.takeIf { state.noticeIsError }
    if (opening) {
        AmountSheet(
            title = "Abrir caja",
            label = "Fondo inicial",
            hint = "El efectivo con el que empieza la caja.",
            action = "Abrir caja",
            colors = colors,
            saving = state.saving,
            error = error,
            onDismiss = { opening = false },
        ) { amount -> actions.onOpenCash(amount) { opening = false } }
    }
    if (closing) {
        AmountSheet(
            title = "Cerrar caja",
            label = "Efectivo contado",
            hint = "Cuenta el efectivo de la caja antes de cerrarla.",
            action = "Cerrar caja",
            colors = colors,
            saving = state.saving,
            error = error,
            onDismiss = { closing = false },
        ) { amount -> actions.onCloseCash(amount) { closing = false } }
    }
}

@Composable
private fun AmountSheet(
    title: String,
    label: String,
    hint: String,
    action: String,
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    AdminSheet(title = title, colors = colors, busy = saving, onDismiss = onDismiss) {
        OperationalField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(11) },
            label = label,
            colors = colors,
            placeholder = "0",
            enabled = !saving,
            keyboardType = KeyboardType.Decimal,
        )
        AdminHint(hint, colors)
        AdminSheetError(error)
        ActionPill(
            label = action,
            enabled = amount.isNotBlank(),
            loading = saving,
            colors = colors,
        ) { onConfirm(amount) }
    }
}
