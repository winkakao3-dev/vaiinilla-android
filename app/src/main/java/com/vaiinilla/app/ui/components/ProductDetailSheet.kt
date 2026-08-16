package com.vaiinilla.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailSheet(
    product: Product,
    categoryName: String,
    selectedOptionIds: Set<Int>,
    defaultOptionIds: Set<Int> = emptySet(),
    quantity: Int,
    previewPrice: String,
    previewTotal: String,
    canAdd: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onToggleOption: (groupId: Int, optionId: Int) -> Unit,
    onClearOptionalGroup: (groupId: Int) -> Unit,
    onQuantityChange: (delta: Int) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    val isCustomized = selectedOptionIds != defaultOptionIds
    val interactionSource = remember { MutableInteractionSource() }
    val sheetVisibility = remember { MutableTransitionState(false) }
    var dismissing by remember { mutableStateOf(false) }
    val haptics = rememberVaiinillaHaptics()
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 100.dp.toPx() }

    fun requestDismiss() {
        if (!dismissing) {
            dismissing = true
            sheetVisibility.targetState = false
        }
    }

    val scrimAlpha by
        animateFloatAsState(
            targetValue = if (sheetVisibility.targetState) 0.58f else 0f,
            animationSpec = tween(durationMillis = if (reduceMotion) 0 else 180),
            label = "product-sheet-scrim",
        )
    val sheetEnter =
        if (reduceMotion) {
            fadeIn(animationSpec = tween(durationMillis = 0))
        } else {
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 500f),
            ) +
                scaleIn(
                    initialScale = 0.94f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 500f),
                ) +
                fadeIn(animationSpec = tween(durationMillis = 160))
        }
    val sheetExit =
        if (reduceMotion) {
            fadeOut(animationSpec = tween(durationMillis = 0))
        } else {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 220),
            ) +
                scaleOut(
                    targetScale = 0.96f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = tween(durationMillis = 220),
                ) +
                fadeOut(animationSpec = tween(durationMillis = 140))
        }

    LaunchedEffect(Unit) {
        sheetVisibility.targetState = true
    }
    LaunchedEffect(sheetVisibility.currentState, dismissing) {
        if (dismissing && !sheetVisibility.currentState) {
            onDismiss()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(onClick = ::requestDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visibleState = sheetVisibility,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .align(Alignment.BottomCenter),
            enter = sheetEnter,
            exit = sheetExit,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {},
                                onDragEnd = {
                                    if (dragOffsetY.value > dismissThresholdPx) {
                                        requestDismiss()
                                    } else {
                                        coroutineScope.launch {
                                            dragOffsetY.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 450f))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffsetY.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 450f))
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val newOffset = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                                    coroutineScope.launch { dragOffsetY.snapTo(newOffset) }
                                },
                            )
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {},
                        ),
                color = colors.paper,
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                shadowElevation = 26.dp,
            ) {
                Column {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .width(44.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(colors.ink.copy(alpha = 0.18f)),
                        )
                    }

                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                // Product detail is intentionally non-scrollable: the whole sheet is one
                                // gesture surface and all current catalog content must fit in this viewport.
                                .padding(horizontal = 20.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(
                                        when {
                                            product.optionGroups.size >= 3 -> 120.dp
                                            product.optionGroups.isNotEmpty() -> 160.dp
                                            else -> 190.dp
                                        },
                                    )
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(colors.paper2),
                        ) {
                            ProductImage(
                                imageUrl = product.imageUrl,
                                contentDescription = product.name,
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                            )
                            if (isCustomized) {
                                Surface(
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopStart)
                                            .padding(12.dp),
                                    color = colors.accent,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(
                                        text = "Personalizado",
                                        color = colors.accentInk,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    )
                                }
                            }
                            Surface(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp),
                                color = colors.ink,
                                shape = RoundedCornerShape(17.dp),
                            ) {
                                Text(
                                    text = moneyLabel(previewPrice),
                                    color = colors.paper,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = categoryName.uppercase(),
                                    color = colors.muted,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    text = product.name,
                                    color = colors.ink,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                            IconButton(
                                onClick = ::requestDismiss,
                                modifier =
                                    Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.paper2),
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = colors.ink)
                            }
                        }
                        Text(
                            text = product.description,
                            color = colors.muted,
                            modifier = Modifier.padding(top = 7.dp),
                        )

                        product.optionGroups.forEach { group ->
                            OptionGroupSection(
                                group = group,
                                selectedOptionIds = selectedOptionIds,
                                onToggleOption = onToggleOption,
                                onClearOptionalGroup = onClearOptionalGroup,
                            )
                        }

                        Surface(
                            color = colors.paper2,
                            shape = RoundedCornerShape(17.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                MetaRow("Ingredientes", product.ingredients)
                                MetaRow("Tiempo estimado", estimatedTimeLabel(product.estimatedTimeMinutes))
                                MetaRow("Alérgenos", product.allergens)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Surface(color = colors.paper, shadowElevation = 14.dp) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                            val selectionSurfaceModifier =
                                if (isCustomized) {
                                    Modifier
                                        .border(2.dp, colors.accent, RoundedCornerShape(18.dp))
                                        .background(colors.paper2, RoundedCornerShape(18.dp))
                                        .padding(12.dp)
                                } else {
                                    Modifier
                                }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f).then(selectionSurfaceModifier)) {
                                    Text(
                                        if (isCustomized) "Tu personalización" else "Tu selección",
                                        color = colors.muted,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text =
                                            product.optionGroups
                                                .flatMap { it.options }
                                                .filter { it.id in selectedOptionIds }
                                                .joinToString(" · ") { it.name }
                                                .ifBlank { "Sin opciones adicionales" },
                                        color = colors.ink,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                                QuantityControl(
                                    quantity = quantity,
                                    onMinus = {
                                        haptics.click()
                                        onQuantityChange(-1)
                                    },
                                    onPlus = {
                                        haptics.click()
                                        onQuantityChange(1)
                                    },
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    haptics.impact()
                                    onAdd()
                                },
                                enabled = canAdd,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = colors.accent,
                                        contentColor = colors.accentInk,
                                        disabledContainerColor = colors.paper2,
                                        disabledContentColor = colors.muted,
                                    ),
                            ) {
                                Text(
                                    text = "Agregar · ${moneyLabel(previewTotal)}",
                                    fontWeight = FontWeight.Black,
                                )
                            }
                            errorMessage?.let { message ->
                                Text(
                                    text = message,
                                    color = Color(0xFF9D2E25),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun estimatedTimeLabel(minutes: Int): String = if (minutes in 8..10) "8–10 min" else "$minutes min"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionGroupSection(
    group: OptionGroup,
    selectedOptionIds: Set<Int>,
    onToggleOption: (Int, Int) -> Unit,
    onClearOptionalGroup: (Int) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(group.name, color = colors.ink, fontWeight = FontWeight.ExtraBold)
            Text(
                text = if (group.minimumSelections > 0) "Obligatorio" else "Opcional",
                color = colors.muted,
                fontWeight = FontWeight.Bold,
            )
        }
        FlowRow(
            modifier = Modifier.padding(top = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (group.minimumSelections == 0) {
                val anySelected = group.options.any { it.id in selectedOptionIds }
                OptionChip(
                    text = "Sin extra",
                    selected = !anySelected,
                    onClick = {
                        haptics.selection()
                        onClearOptionalGroup(group.id)
                    },
                )
            }
            group.options.forEach { option ->
                val extra = if (option.extraPrice == "0.00") "" else " +${moneyLabel(option.extraPrice)}"
                OptionChip(
                    text = option.name + extra,
                    selected = option.id in selectedOptionIds,
                    onClick = {
                        haptics.selection()
                        onToggleOption(group.id, option.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun OptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(13.dp))
                .background(if (selected) colors.accent else colors.paper2)
                .clickable(onClick = onClick)
                .heightIn(min = 48.dp)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                }.padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = text, color = colors.ink, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun MetaRow(
    label: String,
    value: String,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = colors.ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            modifier = Modifier.width(108.dp),
        )
        Text(
            text = value,
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuantityControl(
    quantity: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onMinus,
            modifier =
                Modifier
                    .size(48.dp)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.ink),
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Quitar uno", tint = colors.paper)
        }
        Text(
            text = quantity.toString(),
            color = colors.ink,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        IconButton(
            onClick = onPlus,
            modifier =
                Modifier
                    .size(48.dp)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.ink),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Agregar uno", tint = colors.paper)
        }
    }
}
