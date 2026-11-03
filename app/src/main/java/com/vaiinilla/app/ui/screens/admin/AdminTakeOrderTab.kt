package com.vaiinilla.app.ui.screens.admin

import com.vaiinilla.app.domain.model.OptionGroup
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.operational.WaiterUiState
import com.vaiinilla.app.ui.screens.COUNTER_LINE_QUANTITY_MAX
import com.vaiinilla.app.ui.screens.CUSTOMER_NAME_MAX
import com.vaiinilla.app.ui.screens.OpcionesMostrador
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.alternarOpcionMostrador
import com.vaiinilla.app.ui.screens.counterOrderMatches
import com.vaiinilla.app.ui.screens.opcionesIniciales
import com.vaiinilla.app.ui.screens.opcionesValidas

/**
 * Pedir, como el lienzo de Doop: el admin toma un pedido para el mostrador o para una mesa con el flujo del mesero (el
 * mostrador se cobra en Caja; la mesa va a su cuenta si el negocio deja pagar al final). Un producto con opciones
 * abre su hoja; la barra lima de abajo abre el resumen, donde va el nombre y se manda.
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
    var choosing by remember { mutableStateOf<CartLine?>(null) }
    var reviewing by remember { mutableStateOf(false) }
    var wasSending by remember { mutableStateOf(false) }
    val catalog = state.counterCatalog
    val products = catalog?.products.orEmpty().filter { it.available }
    val visible = counterOrderMatches(products, query)
    val spaces = remember(state.tables) { state.tables.map { it.space }.distinctBy { it.id } }
    val destination = state.counterOrderSpace
    val toAccount = destination != null && state.allowsPayAtEnd

    LaunchedEffect(Unit) { if (!state.counterOrderOpen) onPickCounter() }
    // El envío salió bien cuando deja de enviar sin error: se limpia y queda listo para el siguiente, en mostrador.
    LaunchedEffect(state.counterOrderSending, state.counterOrderError) {
        if (wasSending && !state.counterOrderSending && state.counterOrderError == null) {
            lines = emptyMap()
            name = ""
            reviewing = false
            onPickCounter()
        }
        wasSending = state.counterOrderSending
    }

    fun step(
        productId: Int,
        delta: Int,
    ) {
        val current = lines[productId] ?: return
        val quantity = (current.quantity + delta).coerceAtMost(COUNTER_LINE_QUANTITY_MAX)
        lines = if (quantity <= 0) lines - productId else lines + (productId to current.copy(quantity = quantity))
    }

    fun add(product: Product) {
        when {
            lines.containsKey(product.id) -> step(product.id, 1)
            product.optionGroups.isNotEmpty() -> choosing = CartLine(product, 1, opcionesIniciales(product))
            else -> lines = lines + (product.id to CartLine(product, 1, opcionesIniciales(product)))
        }
    }

    Box(Modifier.fillMaxSize()) {
        AdminHeroPage(
            cover = header.photoUrl,
            caption = "Administración · Nuevo pedido",
            title = "Pedir",
            colors = colors,
            compactTitle = "Pedir",
            compactLogo = header.logoUrl,
            trailing = { header.avatar() },
        ) {
            item(key = "para-quien") {
                Column(Modifier.adminRise(1)) {
                    AdminCapsLabel("¿Para quién es?", colors, Modifier.padding(top = 14.dp, bottom = 8.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DestinationChip(
                            "Mostrador",
                            destination == null,
                            colors,
                            showsIcon = true,
                            onClick = onPickCounter,
                        )
                        spaces.forEach { space ->
                            DestinationChip(space.name, destination?.id == space.id, colors) { onPickTable(space) }
                        }
                    }
                }
            }
            item(
                key = "buscar",
            ) { SearchField(query, colors, Modifier.padding(top = 12.dp).adminRise(2)) { query = it } }
            when {
                catalog == null ->
                    item(key = "cargando") {
                        Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            if (state.counterOrderError != null) {
                                AdminHint(state.counterOrderError, colors)
                            } else {
                                CircularProgressIndicator(
                                    color = colors.textPrimary,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                visible.isEmpty() ->
                    item(key = "nada") {
                        Box(Modifier.padding(start = 4.dp, top = 14.dp)) {
                            AdminHint("Nada con «${query.trim()}» en el menú.", colors)
                        }
                    }
                else ->
                    visible.chunked(2).forEachIndexed { index, pair ->
                        item(key = "fila-${pair.first().id}") {
                            Row(
                                Modifier.animateItem().adminRise(3).padding(top = if (index == 0) 14.dp else 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                pair.forEach { product ->
                                    TakeOrderCard(
                                        product = product,
                                        quantity = lines[product.id]?.quantity ?: 0,
                                        colors = colors,
                                        modifier = Modifier.weight(1f),
                                        onAdd = { add(product) },
                                        onSubtract = { step(product.id, -1) },
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
            }
            state.counterOrderError?.takeIf { catalog != null && !reviewing }?.let { message ->
                item(key = "error") {
                    Text(
                        message,
                        color = colors.coralText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            item(key = "espacio-barra") { Spacer(Modifier.height(if (lines.isEmpty()) 0.dp else 84.dp)) }
        }
        ToastPill(
            message = state.toastMessage,
            containerColor = colors.textPrimary,
            contentColor = colors.background,
            badgeColor = if (state.toastIsError) AdminCoral else colors.accentLime,
            badgeContentColor = if (state.toastIsError) colors.background else colors.accentInk,
            icon = if (state.toastIsError) Icons.Rounded.PriorityHigh else Icons.Rounded.Check,
        )
        Box(Modifier.align(Alignment.BottomCenter)) {
            OrderBar(
                visible = lines.isNotEmpty(),
                destination = destination?.name ?: "Mostrador",
                lines = lines.values.toList(),
                toAccount = toAccount,
                colors = colors,
            ) { reviewing = true }
        }
    }
    choosing?.let { line ->
        OptionsSheet(
            line = line,
            destination = destination?.name ?: "Mostrador",
            colors = colors,
            onChange = { choosing = it },
            onDismiss = { choosing = null },
        ) {
            lines = lines + (line.product.id to line)
            choosing = null
        }
    }
    if (reviewing && lines.isNotEmpty()) {
        ReviewSheet(
            lines = lines.values.toList(),
            name = name,
            destination = destination?.name ?: "Mostrador",
            toAccount = toAccount,
            sending = state.counterOrderSending,
            error = state.counterOrderError,
            colors = colors,
            onName = { if (it.length <= CUSTOMER_NAME_MAX) name = it },
            onStep = ::step,
            onOption = { line, group, optionId ->
                lines =
                    lines +
                    (
                        line.product.id to
                            line.copy(
                                selectedOptionIds = alternarOpcionMostrador(line.selectedOptionIds, group, optionId),
                            )
                    )
            },
            onDismiss = { reviewing = false },
        ) { onSubmit(lines.values.toList(), name) }
    }
}

@Composable
private fun DestinationChip(
    label: String,
    selected: Boolean,
    colors: OperationalColors,
    showsIcon: Boolean = false,
    onClick: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .height(42.dp)
            .physicalPress(scale = PhysicalPressScale.Small) {
                haptics.selection()
                onClick()
            }.semantics { this.selected = selected }
            .clip(shape)
            .background(
                animateSelectionColor(if (selected) colors.textPrimary else colors.cardBackground, "destino-fondo"),
            ).border(1.dp, if (selected) colors.textPrimary else colors.cardBorder, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val tint = if (selected) colors.background else colors.textSecondary
        if (showsIcon) {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = tint, maxLines = 1)
    }
}

@Composable
private fun SearchField(
    query: String,
    colors: OperationalColors,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Buscar producto",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
                cursorBrush = SolidColor(colors.textPrimary),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Buscar producto" },
            )
        }
    }
}

/** Un producto con su foto: tocarlo suma; con algo en el pedido, la pastilla lima con menos, cantidad y más. */
@Composable
private fun TakeOrderCard(
    product: Product,
    quantity: Int,
    colors: OperationalColors,
    modifier: Modifier,
    onAdd: () -> Unit,
    onSubtract: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(0.84f)
            .physicalPress(enabled = quantity < COUNTER_LINE_QUANTITY_MAX, scale = PhysicalPressScale.ProductCard) {
                haptics.selection()
                onAdd()
            }.semantics {
                contentDescription =
                    "Agregar ${product.name}" + if (quantity > 0) ", $quantity en el pedido" else ""
            }.clip(shape)
            .background(colors.cardInner)
            .border(
                if (quantity >
                    0
                ) {
                    2.dp
                } else {
                    0.dp
                },
                if (quantity > 0) colors.accentLime else Color.Transparent,
                shape,
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
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Scrim)
                .padding(start = 14.dp, end = 12.dp, top = 34.dp, bottom = 12.dp),
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
        Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            AnimatedContent(
                targetState = quantity > 0,
                transitionSpec = {
                    if (reduced) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        (scaleIn(spring(dampingRatio = 0.65f, stiffness = 520f), 0.6f) + fadeIn()) togetherWith
                            (scaleOut(spring(stiffness = 700f), 0.6f) + fadeOut())
                    }
                },
                label = "pastilla",
            ) { inOrder ->
                if (inOrder) {
                    Row(
                        Modifier
                            .height(
                                34.dp,
                            ).clip(CircleShape)
                            .background(colors.accentLime)
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Stepper(Icons.Rounded.Remove, "Quitar uno de ${product.name}", colors.accentInk, onSubtract)
                        RollingCount(quantity, colors.accentInk)
                        Stepper(Icons.Rounded.Add, "Agregar otro ${product.name}", colors.accentInk, onAdd)
                    }
                } else {
                    Box(
                        Modifier
                            .size(34.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = Color(0xFF1D1E1C),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stepper(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Box(
        Modifier
            .size(width = 32.dp, height = 34.dp)
            .physicalPress(scale = PhysicalPressScale.Small) {
                haptics.selection()
                onClick()
            }.semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

/** La cantidad cambia rodando: sube al sumar y baja al quitar. */
@Composable
private fun RollingCount(
    value: Int,
    color: Color,
    fontSize: Int = 15,
) {
    val reduced = reducedMotion()
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val up = targetState > initialState
            if (reduced) {
                fadeIn() togetherWith fadeOut()
            } else {
                (
                    slideInVertically(
                        spring(dampingRatio = 0.8f, stiffness = 600f),
                    ) { if (up) it else -it } + fadeIn()
                ) togetherWith
                    (slideOutVertically(spring(stiffness = 800f)) { if (up) -it else it } + fadeOut())
            }
        },
        label = "cantidad",
        modifier = Modifier.widthIn(min = 16.dp),
        contentAlignment = Alignment.Center,
    ) { count -> Text("$count", fontSize = fontSize.sp, fontWeight = FontWeight.Black, color = color) }
}

/** La barra lima del lienzo: cuántos, para quién y cuánto; tocarla abre el resumen. */
@Composable
private fun OrderBar(
    visible: Boolean,
    destination: String,
    lines: List<CartLine>,
    toAccount: Boolean,
    colors: OperationalColors,
    onOpen: () -> Unit,
) {
    val reduced = reducedMotion()
    val haptics = rememberVaiinillaHaptics()
    AnimatedVisibility(
        visible = visible,
        enter =
            if (reduced) {
                fadeIn()
            } else {
                slideInVertically(spring(dampingRatio = 0.82f, stiffness = 420f)) { it } +
                    fadeIn()
            },
        exit = if (reduced) fadeOut() else slideOutVertically(spring(stiffness = 600f)) { it } + fadeOut(),
    ) {
        val missing = lines.any { !opcionesValidas(it) }
        Row(
            Modifier
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                .fillMaxWidth()
                .height(58.dp)
                .physicalPress(scale = PhysicalPressScale.Small) {
                    haptics.click()
                    onOpen()
                }.shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.accentLime)
                .padding(start = 12.dp, end = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(colors.accentInk),
                contentAlignment = Alignment.Center,
            ) {
                RollingCount(lines.sumOf { it.quantity }, colors.accentLime, fontSize = 14)
            }
            Text(
                "$destination · ${moneyLabel(Money.counterPreview(lines))}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = colors.accentInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(colors.accentInk)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when {
                        missing -> "Faltan opciones"
                        toAccount -> "A la cuenta"
                        else -> "Enviar a Caja"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accentLime,
                )
            }
        }
    }
}

/** Las opciones de un producto antes de sumarlo; cerrar la hoja no lo agrega. */
/** Elegir las opciones de un producto a pantalla completa, como el marco 32: foto, grupos y abajo cuántos y agregar. */
@Composable
private fun OptionsSheet(
    line: CartLine,
    destination: String,
    colors: OperationalColors,
    onChange: (CartLine) -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.background),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(190.dp).background(colors.cardBackground)) {
                    line.product.imageUrl?.let {
                        ProductImage(imageUrl = it, contentDescription = "", modifier = Modifier.fillMaxSize())
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))),
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text(line.product.name, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(
                            "${moneyLabelCents(line.product.counterPrice)} · $destination",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                        AdminRoundButton(Icons.Rounded.Close, "Cerrar", colors, onDismiss)
                    }
                }
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    line.product.optionGroups.forEach { group ->
                        OptionGroupBlock(group, line.selectedOptionIds, colors) { optionId ->
                            onChange(
                                line.copy(
                                    selectedOptionIds = alternarOpcionMostrador(line.selectedOptionIds, group, optionId),
                                ),
                            )
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardBorder))
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .height(54.dp)
                        .clip(CircleShape)
                        .border(1.dp, colors.cardBorder, CircleShape)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Stepper(Icons.Rounded.Remove, "Uno menos", colors.textPrimary) {
                        onChange(line.copy(quantity = (line.quantity - 1).coerceAtLeast(1)))
                    }
                    RollingCount(line.quantity, colors.textPrimary)
                    Stepper(Icons.Rounded.Add, "Uno más", colors.textPrimary) {
                        onChange(line.copy(quantity = (line.quantity + 1).coerceAtMost(COUNTER_LINE_QUANTITY_MAX)))
                    }
                }
                val missing =
                    line.product.optionGroups.firstOrNull { group ->
                        group.options.count { it.id in line.selectedOptionIds } < group.minimumSelections
                    }
                AdminPrimaryButton(
                    title =
                        if (missing == null) {
                            "Agregar ${line.quantity} · ${moneyLabel(Money.counterPreview(listOf(line)))}"
                        } else {
                            "Elige ${missing.name.lowercase()}"
                        },
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    enabled = missing == null,
                    onClick = onAdd,
                )
            }
        }
    }
}

@Composable
private fun OptionGroupBlock(
    group: OptionGroup,
    selected: Set<Int>,
    colors: OperationalColors,
    onOption: (Int) -> Unit,
) {
    val chosen = group.options.count { it.id in selected }
    val required = group.minimumSelections >= 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(group.name, fontSize = 17.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
                Text(
                    if (group.maximumSelections == 1) "Elige 1" else "Hasta ${group.maximumSelections}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                )
            }
            when {
                required && chosen >= group.minimumSelections -> AdminBadge("Listo", colors, AdminBadgeTone.OK)
                required -> AdminBadge("Obligatorio", colors, AdminBadgeTone.WARN)
                else -> AdminBadge("Opcional", colors, AdminBadgeTone.NEUTRAL)
            }
        }
        if (group.maximumSelections == 1) {
            group.options.chunked(OPTION_TILES_PER_ROW).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { option ->
                        val on = option.id in selected
                        val bg = animateSelectionColor(if (on) colors.textPrimary else colors.cardBackground, "opcion")
                        val ink = animateSelectionColor(if (on) colors.background else colors.textPrimary, "opcion-tinta")
                        val extra = option.extraPrice.toBigDecimalOrNull()?.takeIf { it.signum() == 1 }
                        Text(
                            option.name + (extra?.let { " +${moneyLabel(it.toPlainString())}" } ?: ""),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ink,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bg)
                                    .border(1.dp, if (on) colors.textPrimary else colors.cardBorder, RoundedCornerShape(14.dp))
                                    .selectable(selected = on, role = Role.RadioButton, onClick = { onOption(option.id) })
                                    .padding(horizontal = 6.dp, vertical = 13.dp),
                        )
                    }
                    repeat(OPTION_TILES_PER_ROW - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        } else {
            group.options.forEach { option ->
                val on = option.id in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardBackground)
                        .selectable(selected = on, role = Role.Checkbox, onClick = { onOption(option.id) })
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (on) colors.accentLime else Color.Transparent)
                            .border(1.5.dp, if (on) colors.accentLime else colors.textMuted, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colors.accentInk,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Text(
                        option.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    option.extraPrice.toBigDecimalOrNull()?.takeIf { it.signum() == 1 }?.let {
                        Text(
                            "+${moneyLabelCents(it.toPlainString())}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

private const val OPTION_TILES_PER_ROW = 3
@Composable
private fun ReviewSheet(
    lines: List<CartLine>,
    name: String,
    destination: String,
    toAccount: Boolean,
    sending: Boolean,
    error: String?,
    colors: OperationalColors,
    onName: (String) -> Unit,
    onStep: (Int, Int) -> Unit,
    onOption: (CartLine, com.vaiinilla.app.domain.model.OptionGroup, Int) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    val missing = lines.filter { !opcionesValidas(it) }
    val total = moneyLabelCents(Money.counterPreview(lines))
    AdminSheet(
        title = if (toAccount) "Pedido de $destination" else "Pedido para $destination",
        colors = colors,
        busy = sending,
        onDismiss = onDismiss,
    ) {
        AdminHint(
            if (toAccount) {
                "Va a cocina y se suma a la cuenta abierta de la mesa. Se cobra al final en Caja."
            } else {
                "Caja lo cobra y lo manda a cocina."
            },
            colors,
        )
        Column {
            lines.forEachIndexed { index, line ->
                if (index > 0) AdminRowDivider(colors)
                val options =
                    line.product.optionGroups
                        .flatMap { it.options }
                        .filter { it.id in line.selectedOptionIds }
                        .joinToString(" · ") { it.name }
                AdminRow(
                    title = line.product.name,
                    colors = colors,
                    detail = options.ifBlank { moneyLabelCents(Money.counterPreview(listOf(line))) },
                    leading = { AdminThumb(line.product.imageUrl, line.product.name, colors) },
                ) {
                    Row(
                        Modifier.clip(CircleShape).border(1.dp, colors.cardBorder, CircleShape),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Stepper(
                            Icons.Rounded.Remove,
                            "Quitar uno de ${line.product.name}",
                            colors.textPrimary,
                        ) { onStep(line.product.id, -1) }
                        RollingCount(line.quantity, colors.textPrimary)
                        Stepper(
                            Icons.Rounded.Add,
                            "Agregar otro ${line.product.name}",
                            colors.textPrimary,
                        ) { onStep(line.product.id, 1) }
                    }
                }
            }
        }
        missing.forEach { line ->
            Text(
                "Elige las opciones de ${line.product.name}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.coralText,
            )
            OpcionesMostrador(linea = line, colors = colors, onOpcion = {
                group,
                optionId,
                ->
                onOption(line, group, optionId)
            })
        }
        if (!toAccount) {
            AdminTextField(
                label = "Nombre del cliente (opcional)",
                value = name,
                onValueChange = onName,
                colors = colors,
                placeholder = "Para llamarlo cuando esté listo",
                enabled = !sending,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Total",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(total, fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
        }
        error?.let { Text(it, color = colors.coralText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        if (toAccount) {
            AdminPrimaryButton(
                title = "A la cuenta de $destination",
                colors = colors,
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                busy = sending,
                enabled = missing.isEmpty() && !sending,
                fill = colors.accentLime,
                contentColor = colors.accentInk,
                onClick = onSend,
            )
        } else {
            AdminPrimaryButton(
                title = "Enviar a Caja",
                colors = colors,
                busy = sending,
                enabled = missing.isEmpty() && !sending,
                onClick = onSend,
            )
        }
    }
}

private val Scrim =
    Brush.verticalGradient(
        0f to Color.Transparent,
        0.45f to Color.Black.copy(alpha = 0.55f),
        1f to Color.Black.copy(alpha = 0.78f),
    )
