package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.NumberTicker
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.operational.WaiterUiState

internal const val CUSTOMER_NAME_MAX = 120
internal const val COUNTER_LINE_QUANTITY_MAX = 20
private val MorphSpring = spring<Float>(dampingRatio = 0.89f, stiffness = 180f)

/** Opciones que trae cada grupo al abrir el producto: las mínimas obligatorias, como en el carrito del cliente. */
internal fun opcionesIniciales(producto: Product): Set<Int> =
    producto.optionGroups.flatMapTo(linkedSetOf()) { grupo ->
        grupo.options.take(grupo.minimumSelections).map { it.id }
    }

/**
 * Marca o desmarca una opción con la misma regla del carrito del cliente: en un grupo de una sola selección
 * reemplaza la anterior; una opción se quita solo si el grupo no es obligatorio.
 */
internal fun alternarOpcionMostrador(
    seleccionadas: Set<Int>,
    grupo: OptionGroup,
    opcionId: Int,
): Set<Int> {
    val idsDelGrupo = grupo.options.mapTo(mutableSetOf()) { it.id }
    return when {
        opcionId in seleccionadas && grupo.minimumSelections == 0 -> seleccionadas - opcionId
        opcionId in seleccionadas -> seleccionadas
        grupo.maximumSelections == 1 -> (seleccionadas - idsDelGrupo) + opcionId
        seleccionadas.count { it in idsDelGrupo } < grupo.maximumSelections -> seleccionadas + opcionId
        else -> seleccionadas
    }
}

internal fun opcionesValidas(linea: CartLine): Boolean =
    runCatching { ContractRules.validateSelections(linea.product, linea.selectedOptionIds) }.isSuccess

/** Lo que dice la barra fija: cuántos artículos, cuánto y qué pasa al tocar. */
internal fun counterOrderBarLabel(
    lines: List<CartLine>,
    action: String,
): String {
    val count = lines.sumOf { it.quantity }
    val items = if (count == 1) "1 artículo" else "$count artículos"
    return "$items · ${moneyLabel(Money.counterPreview(lines))} · $action"
}

/** Productos cuyo nombre contiene lo buscado, sin importar acentos ni mayúsculas. */
internal fun counterOrderMatches(
    products: List<Product>,
    query: String,
): List<Product> {
    val needle = query.trim().foldAccents()
    if (needle.isEmpty()) return products
    return products.filter { it.name.foldAccents().contains(needle) }
}

/** Las categorías que tienen algo en el menú, en el orden del catálogo, para los chips de C03. */
internal fun counterOrderCategories(catalog: Catalog?): List<Category> {
    val used = catalog?.products.orEmpty().mapTo(mutableSetOf()) { it.categoryId }
    return catalog?.categories.orEmpty().filter { it.id in used }.sortedBy { it.order }
}

private fun String.foldAccents(): String =
    java.text.Normalizer
        .normalize(lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

/**
 * Pedido de mostrador: el staff arma el pedido para un cliente que no usa la app. Solo efectivo, que se cobra en
 * Caja. Una cuadrícula de productos (un toque suma uno), un buscador y una barra fija con el conteo y el total
 * de mostrador; el total final lo confirma el servidor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CounterOrderSheet(
    state: WaiterUiState,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onSubmit: (List<CartLine>, String) -> Unit,
    /** Qué hace el botón: el mesero lo manda a Caja; en Caja queda en "Por cobrar". */
    submitAction: String = "Enviar a Caja",
    /** Solo en Caja con la caja abierta: crea el pedido y abre su cobro. El otro botón lo deja por cobrar (G03). */
    onSubmitAndCollect: ((List<CartLine>, String) -> Unit)? = null,
) {
    var nombre by remember { mutableStateOf("") }
    var buscar by remember { mutableStateOf("") }
    var lineas by remember { mutableStateOf<Map<Int, CartLine>>(emptyMap()) }
    var categoria by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val catalogo = state.counterCatalog
    val categorias = counterOrderCategories(catalogo)
    // Lo agotado se queda a la vista, en gris y sin tocar, para que el cajero lo diga en vez de buscarlo (C03).
    val productos =
        catalogo?.products.orEmpty().filter { categoria == null || it.categoryId == categoria }
    val visibles = counterOrderMatches(productos, buscar)
    val opcionesInvalidas =
        lineas.values
            .filter { linea -> !opcionesValidas(linea) }
            .map { it.product.name }
    val puedeEnviar = lineas.isNotEmpty() && opcionesInvalidas.isEmpty() && !state.counterOrderSending

    fun sumar(producto: Product) {
        val actual = lineas[producto.id]
        lineas =
            if (actual == null) {
                lineas + (producto.id to CartLine(producto, 1, opcionesIniciales(producto)))
            } else {
                lineas +
                    (
                        producto.id to
                            actual.copy(quantity = (actual.quantity + 1).coerceAtMost(COUNTER_LINE_QUANTITY_MAX))
                    )
            }
    }

    fun restar(productoId: Int) {
        val actual = lineas[productoId] ?: return
        lineas =
            if (actual.quantity <=
                1
            ) {
                lineas - productoId
            } else {
                lineas + (productoId to actual.copy(quantity = actual.quantity - 1))
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.cardBackground)
                            .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        state.counterOrderSpace?.let { "Pedido para ${it.name}" } ?: "Pedido de mostrador",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.counterOrderSpace == null) {
                        Text("Para llevar", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.size(44.dp))
            }
            CounterInlineField(
                value = buscar,
                onValueChange = { buscar = it },
                placeholder = "Buscar en el menú",
                colors = colors,
                background = colors.cardBackground,
                shape = CircleShape,
                height = 48.dp,
                leadingIcon = Icons.Outlined.Search,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 14.dp),
            )
            if (categorias.size > 1) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(top = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoriaChip("Todo", elegida = categoria == null, colors = colors) { categoria = null }
                    categorias.forEach { cat ->
                        CategoriaChip(cat.name, elegida = categoria == cat.id, colors = colors) { categoria = cat.id }
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 7.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (catalogo == null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            state.counterOrderError ?: "Cargando menú…",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                        )
                    }
                } else if (visibles.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            if (buscar.isBlank()) "Nada en esta categoría." else "Nada con “${buscar.trim()}” en el menú.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
                items(visibles, key = { "producto-${it.id}" }) { producto ->
                    ProductoMostradorTile(
                        modifier = Modifier.animateItem(),
                        producto = producto,
                        cantidad = lineas[producto.id]?.quantity ?: 0,
                        colors = colors,
                        onSumar = { sumar(producto) },
                        onRestar = { restar(producto.id) },
                    )
                }
                val conOpciones = lineas.values.filter { it.product.optionGroups.isNotEmpty() }
                if (conOpciones.isNotEmpty()) {
                    item(key = "opciones-titulo", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Opciones",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.animateItem().padding(top = 8.dp),
                        )
                    }
                    items(
                        conOpciones,
                        key = { "opciones-${it.product.id}" },
                        span = { GridItemSpan(maxLineSpan) },
                    ) { linea ->
                        OpcionesMostrador(
                            modifier = Modifier.animateItem(),
                            linea = linea,
                            colors = colors,
                            onOpcion = { grupo, opcionId ->
                                lineas[linea.product.id]?.let {
                                    lineas =
                                        lineas +
                                        (
                                            linea.product.id to
                                                it.copy(
                                                    selectedOptionIds =
                                                        alternarOpcionMostrador(it.selectedOptionIds, grupo, opcionId),
                                                )
                                        )
                                }
                            },
                        )
                    }
                }
            }
            val barShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .shadow(14.dp, barShape, spotColor = Color(0x59000000))
                        .clip(barShape)
                        .background(colors.cardBackground)
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (opcionesInvalidas.isNotEmpty()) {
                    Text(
                        "Elige las opciones obligatorias de ${opcionesInvalidas.joinToString()}.",
                        color = CounterError,
                        fontSize = 13.sp,
                    )
                }
                state.counterOrderError?.takeIf { catalogo != null }?.let { mensaje ->
                    Text(mensaje, color = CounterError, fontSize = 13.sp)
                }
                if (lineas.isNotEmpty()) {
                    CounterOrderSummary(lineas.values.toList(), colors)
                    CounterInlineField(
                        value = nombre,
                        onValueChange = { if (it.length <= CUSTOMER_NAME_MAX) nombre = it },
                        placeholder = "Nombre del cliente (opcional)",
                        colors = colors,
                        background = colors.cardInner,
                        shape = RoundedCornerShape(14.dp),
                        height = 44.dp,
                    )
                }
                if (onSubmitAndCollect != null && lineas.isNotEmpty()) {
                    // Unos pagan y se van; otros ordenan y se sientan: el cajero elige.
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionPill(
                            label = submitAction,
                            enabled = puedeEnviar,
                            loading = false,
                            colors = colors,
                            outlined = true,
                            shape = CircleShape,
                            height = 56.dp,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f),
                        ) { onSubmit(lineas.values.toList(), nombre) }
                        ActionPill(
                            label = "Cobrar ya · ${moneyLabel(Money.counterPreview(lineas.values.toList()))}",
                            enabled = puedeEnviar,
                            loading = state.counterOrderSending,
                            colors = colors,
                            shape = CircleShape,
                            height = 56.dp,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f),
                        ) { onSubmitAndCollect(lineas.values.toList(), nombre) }
                    }
                    Text(
                        "«$submitAction» lo manda a cocina y lo cobras cuando se siente.",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    ActionPill(
                        label =
                            if (lineas.isEmpty()) {
                                "Toca un producto para agregarlo"
                            } else {
                                counterOrderBarLabel(lineas.values.toList(), submitAction)
                            },
                        enabled = puedeEnviar,
                        loading = state.counterOrderSending,
                        colors = colors,
                        shape = CircleShape,
                        height = 56.dp,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Black,
                    ) { onSubmit(lineas.values.toList(), nombre) }
                }
            }
        }
    }
}

private val CounterError = Color(0xFFE5645A)

/** Campo de una sola línea de la hoja, con el texto de ayuda adentro: el buscador y el nombre del cliente. */
@Composable
private fun CounterInlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    colors: OperationalColors,
    background: Color,
    shape: Shape,
    height: Dp,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
        cursorBrush = SolidColor(colors.textPrimary),
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(background)
                .semantics { contentDescription = placeholder },
        decorationBox = { field ->
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leadingIcon != null) {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    field()
                }
            }
        },
    )
}
/** Un chip de categoría arriba de la cuadrícula; el elegido en tinta. Se ve de 34 con un toque de 44. */
@Composable
private fun CategoriaChip(
    label: String,
    elegida: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    val fondo = animateSelectionColor(if (elegida) colors.textPrimary else colors.cardBackground, "categoria-fondo")
    val tinta = animateSelectionColor(if (elegida) colors.background else colors.textPrimary, "categoria-tinta")
    Box(
        modifier =
            Modifier
                .selectable(selected = elegida, role = Role.Tab, onClick = onClick)
                .padding(vertical = 5.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(fondo)
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = tinta, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

/** «3 ARTÍCULOS $66» con lo pedido a la derecha, para revisarlo de un vistazo antes de cobrar (C03). */
@Composable
private fun CounterOrderSummary(
    lines: List<CartLine>,
    colors: OperationalColors,
) {
    val count = lines.sumOf { it.quantity }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (count == 1) "1 ARTÍCULO" else "$count ARTÍCULOS",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = colors.textSecondary,
            )
            Text(
                moneyLabel(Money.counterPreview(lines)),
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                color = colors.textPrimary,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
            lines.take(3).forEach { line ->
                Text(
                    "${line.quantity} × ${line.product.name}",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (lines.size > 3) Text("y ${lines.size - 3} más", fontSize = 12.5.sp, color = colors.textSecondary)
        }
    }
}

/** Ficha de un producto: un toque suma uno; con algo en el pedido muestra el contador y un botón para restar. */
@Composable
private fun ProductoMostradorTile(
    modifier: Modifier = Modifier,
    producto: Product,
    cantidad: Int,
    colors: OperationalColors,
    onSumar: () -> Unit,
    onRestar: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val agotado = !producto.available
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .physicalPress(enabled = !agotado && cantidad < COUNTER_LINE_QUANTITY_MAX, onClick = onSumar)
                    .semantics {
                        contentDescription = if (agotado) "${producto.name}, se acabó" else "Agregar ${producto.name}"
                    }.graphicsLayer { alpha = if (agotado) 0.4f else 1f }
                    .clip(shape)
                    .background(colors.cardBackground),
        ) {
            ProductImage(
                imageUrl = producto.imageUrl,
                contentDescription = producto.name,
                modifier = Modifier.fillMaxWidth().height(78.dp),
                placeholder = { placeholderModifier ->
                    Box(placeholderModifier.background(colors.cardInner), contentAlignment = Alignment.Center) {
                        Text(
                            producto.name
                                .trim()
                                .firstOrNull()
                                ?.uppercase()
                                .orEmpty(),
                            color = colors.textMuted,
                            fontWeight = FontWeight.Black,
                            fontSize = 34.sp,
                        )
                    }
                },
            )
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    producto.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    lineHeight = 15.sp,
                    color = colors.textPrimary,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (agotado) "Se acabó" else moneyLabel(producto.counterPrice),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (agotado) colors.textSecondary else colors.accentLime,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = cantidad > 0,
            enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 600f)) + fadeIn(),
            exit = scaleOut(spring(stiffness = 700f)) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            // La píldora mide 26 como en el lienzo; el toque para restar llega a 48 alrededor de ella.
            Box(
                modifier =
                    Modifier
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onRestar)
                        .semantics(mergeDescendants = true) { contentDescription = "Quitar uno de ${producto.name}" }
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(6.dp)
                            .height(26.dp)
                            .clip(CircleShape)
                            .background(colors.accentLime)
                            .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text("−", color = colors.accentInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    NumberTicker(value = cantidad, label = "counter-tile-count") { value ->
                        Text("$value", color = colors.accentInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/** Las opciones de un producto ya agregado, con la misma regla del carrito del cliente; lo elegido en tinta. */
@Composable
internal fun OpcionesMostrador(
    modifier: Modifier = Modifier,
    linea: CartLine,
    colors: OperationalColors,
    onOpcion: (OptionGroup, Int) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(linea.product.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
        linea.product.optionGroups.forEach { grupo ->
            Text(
                grupo.name + if (grupo.minimumSelections >= 1) " · obligatorio" else " · opcional",
                fontSize = 12.sp,
                color = colors.textSecondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                grupo.options.forEach { opcion ->
                    val elegida = opcion.id in linea.selectedOptionIds
                    val fondo =
                        animateSelectionColor(if (elegida) colors.textPrimary else colors.cardInner, "opcion-fondo")
                    val tinta =
                        animateSelectionColor(if (elegida) colors.background else colors.textPrimary, "opcion-tinta")
                    Text(
                        opcion.name,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(fondo)
                                .border(
                                    1.dp,
                                    if (elegida) colors.textPrimary else colors.cardBorder,
                                    RoundedCornerShape(10.dp),
                                ).selectable(
                                    selected = elegida,
                                    role = Role.Checkbox,
                                    onClick = { onOpcion(grupo, opcion.id) },
                                ).padding(horizontal = 12.dp, vertical = 8.dp),
                        color = tinta,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** Botón de la pantalla del mesero y de caja que abre la hoja de pedido de mostrador. */
@Composable
internal fun CounterOrderEntry(
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                .pressableScale { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Pedido de mostrador", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
        Text("Tomar pedido", fontSize = 13.sp, color = colors.textSecondary)
    }
}

/** Clic con foco y Enter como `clickable`, más un leve encogimiento mientras el dedo está abajo. */
@Composable
private fun Modifier.pressableScale(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = if (reducedMotion()) snap() else spring(dampingRatio = 0.42f, stiffness = 700f),
        label = "counter-press-scale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.clickable(
            enabled = enabled,
            interactionSource = source,
            indication = LocalIndication.current,
            onClick = onClick,
        )
}
