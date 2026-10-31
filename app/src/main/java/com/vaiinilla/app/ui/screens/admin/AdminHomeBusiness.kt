package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.location.MapsLink
import com.vaiinilla.app.domain.admin.HomeBusinessRules
import com.vaiinilla.app.domain.admin.HomeDeliverySettings
import com.vaiinilla.app.domain.admin.PreorderSettings
import com.vaiinilla.app.domain.admin.StoreLocation
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField

/** "Se prepara el mismo día", "Necesita 1 día" o "Necesita 3 días". */
internal fun preparationLabel(days: Int): String =
    when (days) {
        0 -> "Se prepara el mismo día"
        1 -> "Necesita 1 día"
        else -> "Necesita $days días"
    }

/**
 * Repostería (CASA-04): cuántos días necesita el producto y cuántos se pueden entregar por día. El número cambia
 * con un resorte para que se note que el toque contó.
 */
@Composable
internal fun ProductPreparationEditor(
    days: Int,
    onDaysChange: (Int) -> Unit,
    maximumPerDay: String,
    onMaximumChange: (String) -> Unit,
    enabled: Boolean,
    colors: OperationalColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ENCARGO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = days,
                    transitionSpec = {
                        val up = targetState > initialState
                        (
                            slideInVertically(
                                spring(stiffness = 500f),
                            ) { if (up) it / 2 else -it / 2 } + fadeIn()
                        ) togetherWith
                            (slideOutVertically(spring(stiffness = 500f)) { if (up) -it / 2 else it / 2 } + fadeOut())
                    },
                    label = "dias-preparacion",
                ) { value ->
                    Text(
                        preparationLabel(value),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }
                Text(
                    "El cliente solo puede pedirlo para una fecha con ese margen.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                )
            }
            StepButton("−", "Un día menos", enabled && days > 0, colors) { onDaysChange((days - 1).coerceAtLeast(0)) }
            Box(Modifier.size(8.dp))
            StepButton("+", "Un día más", enabled && days < HomeBusinessRules.MAX_PREPARATION_DAYS, colors) {
                onDaysChange((days + 1).coerceAtMost(HomeBusinessRules.MAX_PREPARATION_DAYS))
            }
        }
        OperationalField(
            value = maximumPerDay,
            onValueChange = { onMaximumChange(it.filter(Char::isDigit).take(3)) },
            label = "Máximo por día",
            colors = colors,
            placeholder = "Sin límite",
            supporting = "Cuántos puedes preparar para un mismo día.",
            enabled = enabled,
            keyboardType = KeyboardType.Number,
        )
    }
}

@Composable
internal fun StepButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (enabled) colors.textPrimary else colors.cardBorder)
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = colors.background, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

/**
 * Entregas a domicilio del negocio (CASA-02): si lleva pedidos, desde dónde se miden los kilómetros, hasta dónde
 * llega, cuánto cobra y si se le puede pagar al recibir.
 */
@Composable
internal fun AdminHomeDeliveryCard(
    saved: HomeDeliverySettings,
    location: StoreLocation?,
    worksFromHome: Boolean,
    locating: Boolean,
    enabled: Boolean,
    readOnly: Boolean,
    colors: OperationalColors,
    onLocate: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSave: (HomeDeliverySettings) -> Unit,
) {
    var draft by remember { mutableStateOf(saved) }
    var place by remember { mutableStateOf("") }
    LaunchedEffect(saved) { draft = saved }
    AdminCard(colors) {
        AdminSectionTitle("Entregas a domicilio", colors)
        AdminChoice(
            question = "¿Llevas pedidos a domicilio?",
            options = listOf(true to "Sí, los llevo", false to "No, los recogen"),
            selected = draft.enabled,
            colors = colors,
            enabled = enabled,
        ) { draft = draft.copy(enabled = it) }
        AnimatedVisibility(
            visible = draft.enabled,
            enter = expandVertically(spring(stiffness = 400f)) + fadeIn(),
            exit = shrinkVertically(spring(stiffness = 400f)) + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (worksFromHome) "Tu negocio" else "Ubicación de tu negocio",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                when (location) {
                    is StoreLocation.At ->
                        AdminHint(
                            if (worksFromHome) {
                                "Los kilómetros se miden desde este punto. Tus clientes solo ven la zona, no tu casa."
                            } else {
                                "Los kilómetros se miden desde este punto."
                            },
                            colors,
                        )
                    StoreLocation.Missing ->
                        AdminHint(
                            "Sin ubicación, la entrega llega a cualquier dirección. Ponla para limitarla por kilómetros.",
                            colors,
                        )
                    null -> AdminHint("Los kilómetros se miden desde la ubicación de tu negocio.", colors)
                }
                if (location != null && !readOnly) {
                    OperationalField(
                        value = place,
                        onValueChange = { place = it.take(MapsLink.MAX_TEXT) },
                        label = "Enlace de Google Maps o dirección",
                        colors = colors,
                        placeholder = "Pega el enlace de Compartir",
                        enabled = enabled && !locating,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionPill(
                            label = if (locating) "Buscando…" else "Ubicar",
                            enabled = enabled && place.isNotBlank() && !locating,
                            loading = locating,
                            colors = colors,
                            primary = false,
                            modifier = Modifier.weight(1f),
                        ) { onLocate(place) }
                        ActionPill(
                            label = "Usar mi ubicación actual",
                            enabled = enabled && !locating,
                            loading = false,
                            colors = colors,
                            primary = false,
                            modifier = Modifier.weight(1.4f),
                            onClick = onUseCurrentLocation,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OperationalField(
                        value = draft.radiusKm,
                        onValueChange = { draft = draft.copy(radiusKm = decimalInput(it)) },
                        label = "Hasta (km)",
                        colors = colors,
                        placeholder = "5",
                        enabled = enabled,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    OperationalField(
                        value = draft.fee,
                        onValueChange = { draft = draft.copy(fee = decimalInput(it)) },
                        label = "Costo del envío",
                        colors = colors,
                        placeholder = "40",
                        enabled = enabled,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
                OperationalField(
                    value = draft.minimum,
                    onValueChange = { draft = draft.copy(minimum = decimalInput(it)) },
                    label = "Pedido mínimo",
                    colors = colors,
                    placeholder = "Sin mínimo",
                    supporting = "Sin contar el envío.",
                    enabled = enabled,
                    keyboardType = KeyboardType.Decimal,
                )
                AdminChoice(
                    question = "¿Te pueden pagar al recibir?",
                    options = listOf(true to "Sí, efectivo o transferencia", false to "No, pagan al pedir"),
                    selected = draft.payOnDelivery,
                    colors = colors,
                    enabled = enabled,
                ) { draft = draft.copy(payOnDelivery = it) }
                if (draft.payOnDelivery) {
                    OperationalField(
                        value = draft.transferDetails,
                        onValueChange = {
                            draft =
                                draft.copy(transferDetails = it.take(HomeBusinessRules.MAX_TRANSFER_DETAILS))
                        },
                        label = "Datos para transferencia",
                        colors = colors,
                        placeholder = "Banco, CLABE y a nombre de quién",
                        supporting = "Tu cliente los ve si elige transferencia. Déjalo vacío si solo aceptas efectivo.",
                        enabled = enabled,
                        singleLine = false,
                        minLines = 2,
                    )
                }
            }
        }
        val locationProblem =
            "Pon la ubicación de tu negocio para medir los kilómetros, o deja vacío «Hasta (km)»."
                .takeIf { draft.enabled && location == StoreLocation.Missing && draft.radiusKm.isNotBlank() }
        val problem = HomeBusinessRules.problem(draft) ?: locationProblem
        AdminSheetError(problem.takeIf { draft != saved })
        if (!readOnly) {
            SaveRow(
                dirty = draft != saved,
                problem = problem,
                enabled = enabled,
                saveLabel = "Guardar entregas",
                colors = colors,
                onDiscard = { draft = saved },
                onSave = { onSave(draft) },
            )
        }
    }
}

/** Encargos para otro día (CASA-03): con cuánta anticipación, hasta qué hora, si se aceptan y el anticipo. */
@Composable
internal fun AdminPreordersCard(
    saved: PreorderSettings,
    enabled: Boolean,
    readOnly: Boolean,
    colors: OperationalColors,
    onSave: (PreorderSettings) -> Unit,
) {
    var draft by remember { mutableStateOf(saved) }
    LaunchedEffect(saved) { draft = saved }
    AdminCard(colors) {
        AdminSectionTitle("Encargos", colors)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = draft.daysAhead,
                transitionSpec = { fadeIn(spring(stiffness = 500f)) togetherWith fadeOut(spring(stiffness = 500f)) },
                label = "dias-adelanto",
                modifier = Modifier.weight(1f),
            ) { days ->
                Text(
                    when (days) {
                        0 -> "Solo para hoy"
                        1 -> "Hasta 1 día antes"
                        else -> "Hasta $days días antes"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
            StepButton("−", "Un día menos", enabled && draft.daysAhead > 0, colors) {
                draft = draft.copy(daysAhead = (draft.daysAhead - 1).coerceAtLeast(0))
            }
            Box(Modifier.size(8.dp))
            StepButton("+", "Un día más", enabled && draft.daysAhead < HomeBusinessRules.MAX_DAYS_AHEAD, colors) {
                draft = draft.copy(daysAhead = (draft.daysAhead + 1).coerceAtMost(HomeBusinessRules.MAX_DAYS_AHEAD))
            }
        }
        AdminHint("Cada producto puede pedir más días de preparación.", colors)
        OperationalField(
            value = draft.cutoff,
            onValueChange = { raw -> draft = draft.copy(cutoff = raw.filter { it.isDigit() || it == ':' }.take(5)) },
            label = "Pedir hasta",
            colors = colors,
            placeholder = "Todo el día",
            supporting = "La hora límite del día de la entrega.",
            enabled = enabled,
        )
        AdminChoice(
            question = "¿Aceptas cada encargo antes de prepararlo?",
            options = listOf(true to "Sí, yo los acepto", false to "No, entran solos"),
            selected = draft.requiresAcceptance,
            colors = colors,
            enabled = enabled,
        ) { draft = draft.copy(requiresAcceptance = it) }
        AdminChoice(
            question = "¿Pides anticipo?",
            options = listOf(0 to "No", 30 to "30 %", 50 to "50 %"),
            selected = draft.depositPercent ?: 0,
            colors = colors,
            enabled = enabled,
        ) { draft = draft.copy(depositPercent = it.takeIf { percent -> percent > 0 }) }
        AdminHint(
            if (draft.depositPercent != null) {
                "Tu cliente paga el anticipo con tarjeta y el resto al recibir."
            } else {
                "Tu cliente paga todo al pedir o al recibir, como lo permitas."
            },
            colors,
        )
        val problem = HomeBusinessRules.problem(draft)
        AdminSheetError(problem.takeIf { draft != saved })
        if (!readOnly) {
            SaveRow(
                dirty = draft != saved,
                problem = problem,
                enabled = enabled,
                saveLabel = "Guardar encargos",
                colors = colors,
                onDiscard = { draft = saved },
                onSave = { onSave(draft) },
            )
        }
    }
}

@Composable
private fun SaveRow(
    dirty: Boolean,
    problem: String?,
    enabled: Boolean,
    saveLabel: String,
    colors: OperationalColors,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionPill(
            label = "Descartar",
            enabled = enabled && dirty,
            loading = false,
            colors = colors,
            primary = false,
            modifier = Modifier.weight(1f),
            onClick = onDiscard,
        )
        ActionPill(
            label = saveLabel,
            enabled = enabled && dirty && problem == null,
            loading = false,
            colors = colors,
            modifier = Modifier.weight(1f),
            onClick = onSave,
        )
    }
}

/** Solo números y un punto: la coma se lee como de miles (decisión del 25 oct) y se quita. */
private fun decimalInput(raw: String): String {
    val clean = raw.filter { it.isDigit() || it == '.' }
    val dot = clean.indexOf('.')
    return if (dot <
        0
    ) {
        clean.take(6)
    } else {
        clean.substring(0, dot + 1) + clean.substring(dot + 1).replace(".", "").take(2)
    }
}
