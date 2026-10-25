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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.CartLine
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

private const val CUSTOMER_NAME_MAX = 120
private const val COUNTER_LINE_QUANTITY_MAX = 20
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

private fun opcionesValidas(linea: CartLine): Boolean =
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
) {
    var nombre by remember { mutableStateOf("") }
    var buscar by remember { mutableStateOf("") }
    var lineas by remember { mutableStateOf<Map<Int, CartLine>>(emptyMap()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val catalogo = state.counterCatalog
    val productos = catalogo?.products.orEmpty().filter { it.available }
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.counterOrderSpace?.let { "Pedido para ${it.name}" } ?: "Pedido de mostrador",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = colors.textPrimary,
                )
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable { onDismiss() },
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
            OperationalField(
                value = buscar,
                onValueChange = { buscar = it },
                label = "Buscar en el menú",
                colors = colors,
                modifier = Modifier.padding(horizontal = 22.dp).padding(top = 12.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                        Text("Nada con “${buscar.trim()}” en el menú.", color = colors.textSecondary, fontSize = 13.sp)
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
                item(key = "nombre", span = { GridItemSpan(maxLineSpan) }) {
                    OperationalField(
                        value = nombre,
                        onValueChange = { if (it.length <= CUSTOMER_NAME_MAX) nombre = it },
                        label = "Nombre del cliente (opcional)",
                        colors = colors,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .padding(horizontal = 22.dp)
                        .navigationBarsPadding()
                        .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                ) { onSubmit(lineas.values.toList(), nombre) }
            }
        }
    }
}

private val CounterError = Color(0xFFE5645A)

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
    val border = animateSelectionColor(if (cantidad > 0) colors.textPrimary else colors.cardBorder, "tile-border")
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .physicalPress(enabled = cantidad < COUNTER_LINE_QUANTITY_MAX, onClick = onSumar)
                    .semantics { contentDescription = "Agregar ${producto.name}" }
                    .clip(shape)
                    .background(colors.cardBackground)
                    .border(if (cantidad > 0) 2.dp else 1.dp, border, shape),
        ) {
            ProductImage(
                imageUrl = producto.imageUrl,
                contentDescription = producto.name,
                modifier = Modifier.fillMaxWidth().height(92.dp),
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
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    producto.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    moneyLabel(producto.counterPrice),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = cantidad > 0,
            enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 600f)) + fadeIn(),
            exit = scaleOut(spring(stiffness = 700f)) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(colors.textPrimary),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .physicalPress(scale = PhysicalPressScale.Small, onClick = onRestar)
                            .semantics { contentDescription = "Quitar uno de ${producto.name}" }
                            .size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("−", color = colors.background, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                NumberTicker(value = cantidad, label = "counter-tile-count") { value ->
                    Text(
                        "$value",
                        color = colors.background,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            }
        }
    }
}

/** Las opciones de un producto ya agregado, con la misma regla del carrito del cliente; lo elegido en tinta. */
@Composable
private fun OpcionesMostrador(
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
