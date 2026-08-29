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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.ProductOption
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

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
    val visibility = remember { MutableTransitionState(false) }
    var dismissing by remember { mutableStateOf(false) }
    val haptics = rememberVaiinillaHaptics()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 96.dp.toPx() }
    var sheetPullDistancePx by remember(product.id) { mutableFloatStateOf(0f) }

    fun requestDismiss() {
        if (!dismissing) {
            dismissing = true
            visibility.targetState = false
        }
    }

    val sheetNestedScroll =
        remember(product.id, listState, dismissThresholdPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y < 0f && sheetPullDistancePx > 0f) {
                        sheetPullDistancePx = (sheetPullDistancePx + available.y).coerceAtLeast(0f)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    val atTop =
                        listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset == 0
                    if (!atTop || available.y <= 0f) return Offset.Zero
                    sheetPullDistancePx += available.y
                    // Consume the overscroll so the list and dismiss gesture never animate the sheet independently.
                    return Offset(0f, available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (sheetPullDistancePx <= 0f) return Velocity.Zero
                    val shouldDismiss =
                        sheetPullDistancePx >= dismissThresholdPx || available.y >= 1_100f
                    sheetPullDistancePx = 0f
                    if (shouldDismiss) requestDismiss()
                    return if (shouldDismiss) Velocity(0f, available.y) else Velocity.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    sheetPullDistancePx = 0f
                    return Velocity.Zero
                }
            }
        }

    val scrimAlpha by
        animateFloatAsState(
            targetValue = if (visibility.targetState) 0.58f else 0f,
            animationSpec = tween(durationMillis = if (reduceMotion) 0 else 180),
            label = "product-detail-scrim",
        )
    val enter =
        if (reduceMotion) {
            fadeIn(animationSpec = tween(0))
        } else {
            slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = spring(dampingRatio = 0.88f, stiffness = 480f),
            ) +
                scaleIn(
                    initialScale = 0.97f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 480f),
                ) +
                fadeIn(animationSpec = tween(150))
        }
    val exit =
        if (reduceMotion) {
            fadeOut(animationSpec = tween(0))
        } else {
            slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(210),
            ) +
                scaleOut(
                    targetScale = 0.98f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = tween(210),
                ) +
                fadeOut(animationSpec = tween(140))
        }

    LaunchedEffect(Unit) { visibility.targetState = true }
    LaunchedEffect(visibility.currentState, dismissing) {
        if (dismissing && !visibility.currentState) onDismiss()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(onClick = ::requestDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visibleState = visibility,
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                        ),
                    ).padding(top = 8.dp)
                    .align(Alignment.BottomCenter),
            enter = enter,
            exit = exit,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(sheetNestedScroll)
                        .testTag("product-detail-surface")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {},
                        ),
                color = colors.paper,
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                shadowElevation = 24.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).testTag("product-detail-scroll"),
                        contentPadding =
                            androidx.compose.foundation.layout
                                .PaddingValues(bottom = 24.dp),
                    ) {
                        item {
                            ProductHero(
                                product = product,
                                previewPrice = previewPrice,
                                customized = isCustomized,
                                onDismiss = ::requestDismiss,
                            )
                        }
                        item {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 18.dp),
                            ) {
                                Text(
                                    text = categoryName.uppercase(),
                                    color = colors.muted,
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = product.name,
                                    color = colors.ink,
                                    fontSize = 34.sp,
                                    lineHeight = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1.2).sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                if (product.description.isNotBlank()) {
                                    Text(
                                        text = product.description,
                                        color = colors.muted,
                                        fontSize = 16.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                        }

                        product.optionGroups.forEach { group ->
                            item(key = "option-group-${group.id}") {
                                ProductOptionGroup(
                                    group = group,
                                    selectedOptionIds = selectedOptionIds,
                                    onToggleOption = onToggleOption,
                                    onClearOptionalGroup = onClearOptionalGroup,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                )
                            }
                        }

                        item {
                            ProductMetaGrid(
                                ingredients = product.ingredients,
                                estimatedTime = estimatedTimeLabel(product.estimatedTimeMinutes),
                                allergens = product.allergens,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
                            )
                        }
                    }

                    ProductDetailDock(
                        product = product,
                        selectedOptionIds = selectedOptionIds,
                        quantity = quantity,
                        previewTotal = previewTotal,
                        canAdd = canAdd,
                        errorMessage = errorMessage,
                        customized = isCustomized,
                        onQuantityChange = { delta ->
                            haptics.click()
                            onQuantityChange(delta)
                        },
                        onAdd = {
                            haptics.impact()
                            onAdd()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductHero(
    product: Product,
    previewPrice: String,
    customized: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp)
                .height(300.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.paper2),
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        HeroActionButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            description = "Volver",
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
        )
        HeroActionButton(
            icon = Icons.Rounded.Close,
            description = "Cerrar",
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
        )

        if (customized) {
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                color = colors.accent,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    "Personalizado",
                    color = colors.accentInk,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                )
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
            color = colors.ink.copy(alpha = 0.94f),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                moneyLabel(previewPrice),
                color = colors.paper,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 17.dp, vertical = 13.dp),
            )
        }
    }
}

@Composable
private fun HeroActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.paper.copy(alpha = 0.90f)),
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = colors.ink,
            modifier = Modifier.size(25.dp),
        )
    }
}

@Composable
private fun ProductOptionGroup(
    group: OptionGroup,
    selectedOptionIds: Set<Int>,
    onToggleOption: (Int, Int) -> Unit,
    onClearOptionalGroup: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val options =
        buildList<ProductOptionChoice> {
            if (group.minimumSelections == 0) {
                add(
                    ProductOptionChoice(
                        option = null,
                        label = "Sin extra",
                        selected = group.options.none { it.id in selectedOptionIds },
                    ),
                )
            }
            group.options.forEach { option ->
                add(
                    ProductOptionChoice(
                        option = option,
                        label =
                            option.name + if (option.extraPrice == "0.00") "" else " +${moneyLabel(option.extraPrice)}",
                        selected = option.id in selectedOptionIds,
                    ),
                )
            }
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                group.name,
                color = colors.ink,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = colors.paper2,
                shape = RoundedCornerShape(99.dp),
            ) {
                Text(
                    if (group.minimumSelections > 0) "Obligatorio" else "Opcional",
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowOptions.forEach { choice ->
                        ProductOptionTile(
                            label = choice.label,
                            selected = choice.selected,
                            onClick = {
                                haptics.selection()
                                val option = choice.option
                                if (option == null) {
                                    onClearOptionalGroup(group.id)
                                } else {
                                    onToggleOption(group.id, option.id)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class ProductOptionChoice(
    val option: ProductOption?,
    val label: String,
    val selected: Boolean,
)

@Composable
private fun ProductOptionTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            modifier
                .heightIn(min = 64.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(if (selected) colors.accent else colors.paper2)
                .border(
                    width = 1.dp,
                    color = if (selected) colors.accent else colors.line,
                    shape = RoundedCornerShape(19.dp),
                ).clickable(onClick = onClick)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = if (selected) colors.accentInk else colors.ink,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .padding(start = 8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.accentInk),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductMetaGrid(
    ingredients: String,
    estimatedTime: String,
    allergens: String,
    modifier: Modifier = Modifier,
) {
    val hasRelevantAllergens =
        allergens.isNotBlank() &&
            !allergens.startsWith("Sin ", ignoreCase = true)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProductMetaCard(
                icon = Icons.Outlined.Eco,
                label = "Ingredientes",
                value = ingredients.ifBlank { "Consulta en cafetería" },
                modifier = Modifier.weight(1f),
            )
            ProductMetaCard(
                icon = Icons.Outlined.Schedule,
                label = "Tiempo estimado",
                value = estimatedTime,
                modifier = Modifier.weight(1f),
            )
        }
        if (hasRelevantAllergens) {
            ProductMetaCard(
                icon = Icons.Outlined.WarningAmber,
                label = "Alérgenos",
                value = allergens,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProductMetaCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = modifier.heightIn(min = 94.dp),
        color = colors.paper2,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colors.accentInk,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                label,
                color = colors.ink,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                value,
                color = colors.ink2,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ProductDetailDock(
    product: Product,
    selectedOptionIds: Set<Int>,
    quantity: Int,
    previewTotal: String,
    canAdd: Boolean,
    errorMessage: String?,
    customized: Boolean,
    onQuantityChange: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val selection =
        product.optionGroups
            .flatMap { it.options }
            .filter { it.id in selectedOptionIds }
            .joinToString(" · ") { it.name }
            .ifBlank { "Sin opciones adicionales" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 18.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        if (customized) "Tu personalización" else "Tu selección",
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        selection,
                        color = colors.ink,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                QuantityControl(
                    quantity = quantity,
                    onMinus = { onQuantityChange(-1) },
                    onPlus = { onQuantityChange(1) },
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAdd,
                enabled = canAdd,
                modifier = Modifier.fillMaxWidth().height(56.dp),
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
                    "Agregar · ${moneyLabel(previewTotal)}",
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            errorMessage?.let { message ->
                Text(
                    message,
                    color = Color(0xFF9D2E25),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
        }
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
        QuantityButton(
            icon = Icons.Rounded.Remove,
            description = "Quitar uno",
            onClick = onMinus,
        )
        Text(
            quantity.toString(),
            color = colors.ink,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        QuantityButton(
            icon = Icons.Rounded.Add,
            description = "Agregar uno",
            onClick = onPlus,
        )
    }
}

@Composable
private fun QuantityButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(colors.ink),
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = colors.paper,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun estimatedTimeLabel(minutes: Int): String =
    when (minutes) {
        in 8..10 -> "8–10 min"
        in 11..15 -> "10–15 min"
        else -> "$minutes min"
    }
