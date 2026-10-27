package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.components.EditorialSearchField
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.operational.WaiterUiState
import com.vaiinilla.app.ui.screens.COUNTER_LINE_QUANTITY_MAX
import com.vaiinilla.app.ui.screens.CUSTOMER_NAME_MAX
import com.vaiinilla.app.ui.screens.OpcionesMostrador
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.alternarOpcionMostrador
import com.vaiinilla.app.ui.screens.counterOrderMatches
import com.vaiinilla.app.ui.screens.opcionesIniciales
import com.vaiinilla.app.ui.screens.opcionesValidas

/**
 * Pedir: el admin toma un pedido para el mostrador o para una mesa, con el mismo flujo del mesero (el mostrador se
 * cobra en efectivo en Caja; la mesa va a su cuenta si el negocio deja pagar al final). Arriba se elige para quién
 * es, en medio se arma con las fotos del menú y abajo queda la barra para mandarlo.
 */
@Composable
internal fun AdminTakeOrderTab(
    state: WaiterUiState,
    colors: OperationalColors,
    header: AdminHeader,
    onPickCounter: () -> Unit,
    onPickTable: (TableSpace) -> Unit,
    onSubmit: (List<CartLine>, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf<Map<Int, CartLine>>(emptyMap()) }
    var wasSending by remember { mutableStateOf(false) }
    val catalog = state.counterCatalog
    val products = catalog?.products.orEmpty().filter { it.available }
    val visible = counterOrderMatches(products, query)
    val spaces = remember(state.tables) { state.tables.map { it.space }.distinctBy { it.id } }
    val destination = state.counterOrderSpace
    val invalid = lines.values.filter { !opcionesValidas(it) }.map { it.product.name }
    val canSend = lines.isNotEmpty() && invalid.isEmpty() && !state.counterOrderSending
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) { if (!state.counterOrderOpen) onPickCounter() }
    // El envío salió bien cuando deja de enviar sin error: se limpia y queda listo para el siguiente, en mostrador.
    LaunchedEffect(state.counterOrderSending, state.counterOrderError) {
        if (wasSending && !state.counterOrderSending && state.counterOrderError == null) {
            lines = emptyMap()
            name = ""
            onPickCounter()
        }
        wasSending = state.counterOrderSending
    }

    fun add(product: Product) {
        val current = lines[product.id]
        lines =
            lines +
            (
                product.id to
                    (
                        current?.copy(quantity = (current.quantity + 1).coerceAtMost(COUNTER_LINE_QUANTITY_MAX))
                            ?: CartLine(product, 1, opcionesIniciales(product))
                    )
            )
    }

    fun remove(productId: Int) {
        val current = lines[productId] ?: return
        lines =
            if (current.quantity <=
                1
            ) {
                lines - productId
            } else {
                lines + (productId to current.copy(quantity = current.quantity - 1))
            }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Side, end = Side, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.bleed(Side)) {
                    AdminHero(
                        title = "Pedir",
                        subtitle = "Nuevo pedido",
                        colors = colors,
                        height = 190.dp,
                        trailing = header.avatar,
                    )
                }
            }
            item(key = "para-quien", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.bleed(Side)) {
                    Text(
                        "¿PARA QUIÉN ES?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(start = 22.dp, top = 14.dp),
                    )
                    AdminChipRow {
                        AdminGlassChip("Mostrador", destination == null, onPickCounter)
                        spaces.forEach { space ->
                            AdminGlassChip(space.name, destination?.id == space.id) { onPickTable(space) }
                        }
                    }
                }
            }
            item(key = "buscar", span = { GridItemSpan(maxLineSpan) }) {
                EditorialSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Buscar en el menú…",
                    seaGlass = true,
                )
            }
            if (catalog == null) {
                item(key = "cargando", span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        if (state.counterOrderError != null) {
                            Text(state.counterOrderError, color = colors.textSecondary, fontSize = 13.sp)
                        } else {
                            CircularProgressIndicator(
                                color = colors.textPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            } else if (visible.isEmpty()) {
                item(key = "nada", span = { GridItemSpan(maxLineSpan) }) {
                    Text("Nada con “${query.trim()}” en el menú.", color = colors.textSecondary, fontSize = 13.sp)
                }
            }
            items(visible, key = { "producto-${it.id}" }) { product ->
                TakeOrderCard(
                    product = product,
                    quantity = lines[product.id]?.quantity ?: 0,
                    colors = colors,
                    modifier = Modifier.animateItem(),
                    onAdd = { add(product) },
                    onRemove = { remove(product.id) },
                )
            }
            val withOptions = lines.values.filter { it.product.optionGroups.isNotEmpty() }
            if (withOptions.isNotEmpty()) {
                item(key = "opciones-titulo", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Opciones",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(
                    withOptions,
                    key = { "opciones-${it.product.id}" },
                    span = { GridItemSpan(maxLineSpan) },
                ) { line ->
                    OpcionesMostrador(
                        modifier = Modifier.animateItem(),
                        linea = line,
                        colors = colors,
                        onOpcion = { group, optionId ->
                            lines[line.product.id]?.let {
                                lines =
                                    lines +
                                    (
                                        line.product.id to
                                            it.copy(
                                                selectedOptionIds =
                                                    alternarOpcionMostrador(it.selectedOptionIds, group, optionId),
                                            )
                                    )
                            }
                        },
                    )
                }
            }
            if (lines.isNotEmpty()) {
                item(key = "nombre", span = { GridItemSpan(maxLineSpan) }) {
                    OperationalField(
                        value = name,
                        onValueChange = { if (it.length <= CUSTOMER_NAME_MAX) name = it },
                        label = "Nombre del cliente (opcional)",
                        colors = colors,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            if (invalid.isNotEmpty()) {
                item(key = "faltan", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Elige las opciones obligatorias de ${invalid.joinToString()}.",
                        color = AdminCoral,
                        fontSize = 13.sp,
                    )
                }
            }
            state.counterOrderError?.takeIf { catalog != null }?.let { message ->
                item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    Text(message, color = AdminCoral, fontSize = 13.sp)
                }
            }
        }
        SendBar(
            visible = lines.isNotEmpty(),
            destination = destination?.name ?: "Mostrador",
            lines = lines.values.toList(),
            toAccount = destination != null && state.allowsPayAtEnd,
            enabled = canSend,
            sending = state.counterOrderSending,
            colors = colors,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { onSubmit(lines.values.toList(), name) }
        ToastPill(
            message = state.toastMessage,
            containerColor = colors.textPrimary,
            contentColor = colors.background,
            badgeColor = if (state.toastIsError) AdminCoral else colors.accentLime,
            badgeContentColor = if (state.toastIsError) colors.background else colors.accentInk,
            icon = if (state.toastIsError) Icons.Rounded.PriorityHigh else Icons.Rounded.Check,
        )
    }
}

private val Side = 20.dp

private fun Modifier.bleed(margin: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = (margin * 2).roundToPx()
        val width = constraints.maxWidth + extra
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
    }

/** Un producto con su foto: un toque suma uno; con algo en el pedido sale la cantidad y un botón para quitar. */
@Composable
private fun TakeOrderCard(
    product: Product,
    quantity: Int,
    colors: OperationalColors,
    modifier: Modifier,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(0.84f)
                .physicalPress(enabled = quantity < COUNTER_LINE_QUANTITY_MAX, scale = PhysicalPressScale.ProductCard) {
                    haptics.click()
                    onAdd()
                }.semantics { contentDescription = "Agregar ${product.name}" }
                .clip(RoundedCornerShape(26.dp))
                .background(colors.cardInner)
                .then(
                    if (quantity >
                        0
                    ) {
                        Modifier.border(2.dp, colors.textPrimary, RoundedCornerShape(26.dp))
                    } else {
                        Modifier
                    },
                ),
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier.fillMaxSize(),
            placeholder = { m ->
                Box(m.background(colors.cardInner), contentAlignment = Alignment.Center) {
                    Text(
                        product.name
                            .trim()
                            .firstOrNull()
                            ?.uppercase()
                            .orEmpty(),
                        color = colors.textMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 40.sp,
                    )
                }
            },
            card = true,
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Scrim)
                    .padding(start = 14.dp, end = 56.dp, top = 34.dp, bottom = 12.dp),
        ) {
            Text(
                product.name,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                moneyLabel(product.counterPrice),
                color = Color.White.copy(alpha = 0.88f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        AnimatedVisibility(
            visible = quantity > 0,
            enter = if (reduced) fadeIn() else scaleIn(spring(dampingRatio = 0.55f, stiffness = 600f)) + fadeIn(),
            exit = if (reduced) fadeOut() else scaleOut(spring(stiffness = 700f)) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.textPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text("$quantity", color = colors.background, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
        AnimatedVisibility(
            visible = quantity > 0,
            enter = if (reduced) fadeIn() else scaleIn(spring(dampingRatio = 0.6f, stiffness = 600f)) + fadeIn(),
            exit = if (reduced) fadeOut() else scaleOut(spring(stiffness = 700f)) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .physicalPress(scale = PhysicalPressScale.Small) {
                        haptics.selection()
                        onRemove()
                    }.semantics { contentDescription = "Quitar uno de ${product.name}" }
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Remove,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** La barra fija de abajo: para quién, cuánto lleva y el botón que lo manda. Sube con un resorte al agregar algo. */
@Composable
private fun SendBar(
    visible: Boolean,
    destination: String,
    lines: List<CartLine>,
    toAccount: Boolean,
    enabled: Boolean,
    sending: Boolean,
    colors: OperationalColors,
    modifier: Modifier,
    onSend: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            if (reduced) {
                fadeIn()
            } else {
                slideInVertically(spring(dampingRatio = 0.82f, stiffness = 420f)) { it } + fadeIn()
            },
        exit = if (reduced) fadeOut() else slideOutVertically(spring(stiffness = 600f)) { it } + fadeOut(),
    ) {
        val count = lines.sumOf { it.quantity }
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.textPrimary)
                    .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "$destination · ${if (count == 1) "1 producto" else "$count productos"}",
                    color = colors.background,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    moneyLabel(Money.counterPreview(lines)),
                    color = colors.background.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            Box(
                Modifier
                    .height(46.dp)
                    .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                        haptics.impact()
                        onSend()
                    }.clip(CircleShape)
                    .background(if (enabled) colors.background else colors.background.copy(alpha = 0.4f))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        color = colors.textPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        if (toAccount) "A la cuenta" else "Enviar a Caja",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

private val Scrim =
    Brush.verticalGradient(
        0f to Color.Transparent,
        0.45f to Color.Black.copy(alpha = 0.55f),
        1f to Color.Black.copy(alpha = 0.78f),
    )
