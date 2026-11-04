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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.vaiinilla.app.domain.model.answerTimeLabel
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField

// 0 es «sin plazo».
private val ANSWER_TIME_OPTIONS =
    listOf(
        30 to "30 min",
        60 to "1 hora",
        180 to "3 horas",
        1440 to "1 día",
        0 to "Sin plazo",
    )

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
 * Entrega a domicilio y encargos en una sola página, como el marco 02b: la zona con su mapa, cuánto pagan por adelantado
 * y si se acepta cada encargo. Lo que se guarda son dos cosas en el servidor, pero el dueño ve un solo «Guardar flujo».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminDeliveryForm(
    savedDelivery: HomeDeliverySettings?,
    savedPreorders: PreorderSettings?,
    location: StoreLocation?,
    worksFromHome: Boolean,
    locating: Boolean,
    saving: Boolean,
    enabled: Boolean,
    readOnly: Boolean,
    colors: OperationalColors,
    onLocate: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSave: (HomeDeliverySettings?, PreorderSettings?) -> Unit,
) {
    var delivery by remember { mutableStateOf(savedDelivery) }
    var preorders by remember { mutableStateOf(savedPreorders) }
    var place by remember { mutableStateOf("") }
    var movingStore by remember { mutableStateOf(false) }
    LaunchedEffect(savedDelivery) { delivery = savedDelivery }
    LaunchedEffect(savedPreorders) { preorders = savedPreorders }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        delivery?.let { current ->
            AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                AdminToggleRow(
                    title = "Llevar a domicilio",
                    checked = current.enabled,
                    colors = colors,
                    onChange = { delivery = current.copy(enabled = it) },
                    detail = if (worksFromHome) "Desde tu casa" else "Desde tu local",
                    enabled = enabled,
                )
                AnimatedVisibility(
                    visible = current.enabled,
                    enter = expandVertically(spring(stiffness = 400f)) + fadeIn(),
                    exit = shrinkVertically(spring(stiffness = 400f)) + fadeOut(),
                ) {
                    Column(
                        Modifier.padding(top = 6.dp, bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DeliveryMap(current.radiusKm, colors) { movingStore = !movingStore }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OperationalField(
                                value = current.radiusKm,
                                onValueChange = { delivery = current.copy(radiusKm = decimalInput(it)) },
                                label = "Radio (km)",
                                colors = colors,
                                placeholder = "5",
                                enabled = enabled,
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                            )
                            OperationalField(
                                value = current.fee,
                                onValueChange = { delivery = current.copy(fee = decimalInput(it)) },
                                label = "Envío",
                                colors = colors,
                                placeholder = "40",
                                enabled = enabled,
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                            )
                            OperationalField(
                                value = current.minimum,
                                onValueChange = { delivery = current.copy(minimum = decimalInput(it)) },
                                label = "Mínimo",
                                colors = colors,
                                placeholder = "Sin",
                                enabled = enabled,
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        when {
                            location == StoreLocation.Missing ->
                                AdminHint(
                                    "Sin ubicación, la entrega llega a cualquier dirección. Ponla para limitarla por kilómetros.",
                                    colors,
                                )
                            worksFromHome ->
                                AdminHint("Tus clientes solo ven la zona, no tu casa.", colors)
                        }
                        if (location != null && !readOnly && (movingStore || location == StoreLocation.Missing)) {
                            OperationalField(
                                value = place,
                                onValueChange = { place = it.take(MapsLink.MAX_TEXT) },
                                label = "Enlace de Google Maps o dirección",
                                colors = colors,
                                placeholder = "Pega el enlace de Compartir",
                                enabled = enabled && !locating,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AdminSecondaryButton(
                                    title = if (locating) "Buscando…" else "Ubicar",
                                    colors = colors,
                                    modifier = Modifier.weight(1f),
                                    outlined = true,
                                    enabled = enabled && place.isNotBlank() && !locating,
                                ) { onLocate(place) }
                                AdminSecondaryButton(
                                    title = "Usar mi ubicación",
                                    colors = colors,
                                    modifier = Modifier.weight(1.4f),
                                    outlined = true,
                                    enabled = enabled && !locating,
                                    onClick = onUseCurrentLocation,
                                )
                            }
                        }
                    }
                }
            }
        }
        val currentDelivery = delivery
        val currentPreorders = preorders
        if (currentDelivery != null || currentPreorders != null) {
            AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                if (currentPreorders != null) {
                    val deposit = currentPreorders.depositPercent
                    val upfront =
                        when {
                            deposit != null -> Upfront.DEPOSIT
                            currentDelivery?.payOnDelivery == true -> Upfront.NOTHING
                            else -> Upfront.ALL
                        }
                    Text(
                        "¿Cuánto pagan por adelantado?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                    )
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Upfront.entries.forEach { option ->
                            AdminFilterChip(
                                label = option.label(deposit),
                                selected = option == upfront,
                                colors = colors,
                                onClick = {
                                    if (!enabled) return@AdminFilterChip
                                    preorders =
                                        currentPreorders.copy(
                                            depositPercent =
                                                (deposit ?: DEFAULT_DEPOSIT).takeIf {
                                                    option ==
                                                        Upfront.DEPOSIT
                                                },
                                        )
                                    delivery =
                                        currentDelivery?.copy(
                                            payOnDelivery =
                                                when (option) {
                                                    Upfront.ALL -> false
                                                    Upfront.DEPOSIT, Upfront.NOTHING -> true
                                                },
                                        )
                                },
                                height = 38.dp,
                            )
                        }
                    }
                }
                if (currentDelivery != null) {
                    AdminToggleRow(
                        title = "Pagar al recibir",
                        checked = currentDelivery.payOnDelivery,
                        colors = colors,
                        onChange = { delivery = currentDelivery.copy(payOnDelivery = it) },
                        detail = "Efectivo o transferencia en la puerta",
                        enabled = enabled,
                    )
                    if (currentDelivery.payOnDelivery) {
                        OperationalField(
                            value = currentDelivery.transferDetails,
                            onValueChange = {
                                delivery =
                                    currentDelivery.copy(
                                        transferDetails = it.take(HomeBusinessRules.MAX_TRANSFER_DETAILS),
                                    )
                            },
                            label = "Datos para transferencia",
                            colors = colors,
                            placeholder = "Banco, CLABE y a nombre de quién",
                            enabled = enabled,
                        )
                    }
                }
            }
        }
        currentPreorders?.let { current ->
            AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                AdminToggleRow(
                    title = "Acepto cada encargo",
                    checked = current.requiresAcceptance,
                    colors = colors,
                    onChange = { preorders = current.copy(requiresAcceptance = it) },
                    detail = "¿En cuánto contestas? Tu cliente lo ve.",
                    enabled = enabled,
                )
                if (current.requiresAcceptance) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ANSWER_TIME_OPTIONS
                            .filter { (minutes, _) ->
                                minutes != UNCOMMON_ANSWER ||
                                    current.answerMinutes == UNCOMMON_ANSWER
                            }.forEach { (minutes, label) ->
                                AdminFilterChip(
                                    label = label,
                                    selected = (current.answerMinutes ?: 0) == minutes,
                                    colors = colors,
                                    onClick = {
                                        if (enabled) preorders = current.copy(answerMinutes = minutes.takeIf { it > 0 })
                                    },
                                    height = 38.dp,
                                )
                            }
                    }
                    Box(Modifier.padding(top = 8.dp, bottom = 6.dp)) {
                        AdminHint(
                            if (current.depositPercent != null) {
                                "Al aceptar, tu cliente tiene 24 horas para pagar el anticipo. Empiezas cuando pague; " +
                                    "si no contestas o no paga a tiempo, se cancela solo."
                            } else {
                                current.answerMinutes?.let {
                                    "Tu cliente ve que contestas en ${answerTimeLabel(it)}. " +
                                        "Si no contestas a tiempo y aún no pagó, se cancela solo."
                                } ?: "Tu cliente no ve un plazo: el encargo espera hasta que contestes."
                            },
                            colors,
                        )
                    }
                }
            }
            AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (current.daysAhead) {
                            0 -> "Solo para hoy"
                            1 -> "Hasta 1 día antes"
                            else -> "Hasta ${current.daysAhead} días antes"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    StepButton("−", "Un día menos", enabled && current.daysAhead > 0, colors) {
                        preorders = current.copy(daysAhead = (current.daysAhead - 1).coerceAtLeast(0))
                    }
                    Box(Modifier.size(8.dp))
                    StepButton(
                        "+",
                        "Un día más",
                        enabled && current.daysAhead < HomeBusinessRules.MAX_DAYS_AHEAD,
                        colors,
                    ) {
                        preorders =
                            current.copy(
                                daysAhead = (current.daysAhead + 1).coerceAtMost(HomeBusinessRules.MAX_DAYS_AHEAD),
                            )
                    }
                }
                OperationalField(
                    value = current.cutoff,
                    onValueChange = { raw ->
                        preorders =
                            current.copy(cutoff = raw.filter { it.isDigit() || it == ':' }.take(5))
                    },
                    label = "Pedir hasta",
                    colors = colors,
                    placeholder = "Todo el día",
                    supporting = "La hora límite del día de la entrega.",
                    enabled = enabled,
                )
            }
        }
        val locationProblem =
            "Pon la ubicación de tu negocio para medir los kilómetros, o deja vacío el radio."
                .takeIf {
                    currentDelivery != null &&
                        currentDelivery.enabled &&
                        location == StoreLocation.Missing &&
                        currentDelivery.radiusKm.isNotBlank()
                }
        val problem =
            currentDelivery?.let { HomeBusinessRules.problem(it) }
                ?: locationProblem
                ?: currentPreorders?.let { HomeBusinessRules.problem(it) }
        val dirty = currentDelivery != savedDelivery || currentPreorders != savedPreorders
        AdminSheetError(problem.takeIf { dirty })
        if (!readOnly) {
            AdminPrimaryButton(
                title = "Guardar flujo",
                colors = colors,
                busy = saving,
                enabled = enabled && dirty && problem == null,
            ) {
                onSave(
                    currentDelivery.takeIf { it != savedDelivery },
                    currentPreorders.takeIf { it != savedPreorders },
                )
            }
        }
    }
}

private enum class Upfront {
    ALL,
    DEPOSIT,
    NOTHING,
    ;

    fun label(deposit: Int?): String =
        when (this) {
            ALL -> "Todo"
            DEPOSIT -> "Anticipo ${deposit ?: DEFAULT_DEPOSIT}%"
            NOTHING -> "Nada"
        }
}

private const val DEFAULT_DEPOSIT = 50

private const val UNCOMMON_ANSWER = 180

/** La zona dibujada, sin mapa real: sirve para ver el radio; tocarla deja cambiar la ubicación del negocio. */
@Composable
private fun DeliveryMap(
    radiusKm: String,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    val lime = colors.accentLime
    val line = colors.textPrimary.copy(alpha = 0.07f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardInner)
            .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "Zona de entrega. Toca para cambiar la ubicación."
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = size.width / 5f
            for (i in 1..4) {
                drawLine(line, Offset(step * i, 0f), Offset(step * i - 30f, size.height), 5f)
            }
            for (i in 1..3) {
                val y = size.height / 4f * i
                drawLine(line, Offset(0f, y), Offset(size.width, y + 14f), 5f)
            }
            val radius = size.height * 0.46f
            drawCircle(lime.copy(alpha = 0.16f), radius)
            drawCircle(lime.copy(alpha = 0.8f), radius, style = Stroke(1.5.dp.toPx()))
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .size(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(lime),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(15.dp),
            )
        }
        if (radiusKm.isNotBlank()) {
            Text(
                "Radio $radiusKm km",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(colors.background.copy(alpha = 0.8f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
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
