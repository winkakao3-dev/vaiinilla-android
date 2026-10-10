package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.operational.WaiterUiState

private const val CUSTOMER_NAME_MAX = 120
private const val COUNTER_LINE_QUANTITY_MAX = 20

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

/**
 * Pedido de mostrador: el staff arma el pedido para un cliente que no usa la app. Solo efectivo, que se cobra en
 * Caja. El total y el folio los calcula el servidor; aquí no se suma nada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CounterOrderSheet(
    state: WaiterUiState,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onSubmit: (List<CartLine>, String) -> Unit,
) {
    var nombre by remember { mutableStateOf("") }
    var lineas by remember { mutableStateOf<Map<Int, CartLine>>(emptyMap()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val catalogo = state.counterCatalog
    val productos = catalogo?.products.orEmpty().filter { it.available }
    val opcionesInvalidas =
        lineas.values
            .filter { linea -> !opcionesValidas(linea) }
            .map { it.product.name }
    val puedeEnviar = lineas.isNotEmpty() && opcionesInvalidas.isEmpty() && !state.counterOrderSending

    fun agregar(producto: Product) {
        lineas = lineas + (producto.id to CartLine(producto, 1, opcionesIniciales(producto)))
    }

    fun quitar(productoId: Int) {
        lineas = lineas - productoId
    }

    fun cambiar(
        productoId: Int,
        transformar: (CartLine) -> CartLine,
    ) {
        lineas[productoId]?.let { lineas = lineas + (productoId to transformar(it)) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Pedido de mostrador",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = colors.textPrimary,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(1.dp, colors.cardBorder, CircleShape)
                                .clickable { onDismiss() },
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
            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { if (it.length <= CUSTOMER_NAME_MAX) nombre = it },
                    label = { Text("Nombre del cliente (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (catalogo == null) {
                item {
                    Text(
                        state.counterOrderError ?: "Cargando menú…",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
            catalogo?.categories?.sortedBy { it.order }?.forEach { categoria ->
                val delaCategoria = productos.filter { it.categoryId == categoria.id }
                if (delaCategoria.isEmpty()) return@forEach
                item(key = "categoria-${categoria.id}") {
                    Text(
                        categoria.name.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                    )
                }
                items(delaCategoria, key = { "producto-${it.id}" }) { producto ->
                    val linea = lineas[producto.id]
                    ProductoMostradorRow(
                        producto = producto,
                        linea = linea,
                        colors = colors,
                        onAgregar = { agregar(producto) },
                        onQuitar = { quitar(producto.id) },
                        onCantidad = { delta ->
                            cambiar(producto.id) {
                                it.copy(quantity = (it.quantity + delta).coerceIn(1, COUNTER_LINE_QUANTITY_MAX))
                            }
                        },
                        onOpcion = { grupo, opcionId ->
                            cambiar(producto.id) {
                                it.copy(
                                    selectedOptionIds = alternarOpcionMostrador(it.selectedOptionIds, grupo, opcionId),
                                )
                            }
                        },
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (opcionesInvalidas.isNotEmpty()) {
                        Text(
                            "Revisa las opciones obligatorias de ${opcionesInvalidas.joinToString()}.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                        )
                    }
                    state.counterOrderError?.let { mensaje ->
                        Text(mensaje, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    Text(
                        "Se cobra en efectivo en Caja. El total lo calcula el servidor.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                    Button(
                        onClick = { onSubmit(lineas.values.toList(), nombre) },
                        enabled = puedeEnviar,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.accentLime,
                                contentColor = colors.accentInk,
                                disabledContainerColor = colors.cardInner,
                                disabledContentColor = colors.textMuted,
                            ),
                    ) {
                        Text(
                            if (state.counterOrderSending) "Enviando…" else "Enviar a Caja",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoMostradorRow(
    producto: Product,
    linea: CartLine?,
    colors: OperationalColors,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit,
    onCantidad: (Int) -> Unit,
    onOpcion: (OptionGroup, Int) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                Text(moneyLabel(producto.counterPrice), fontSize = 13.sp, color = colors.textSecondary)
            }
            if (linea == null) {
                Text(
                    "Agregar",
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accentLime)
                            .clickable(onClick = onAgregar)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = colors.accentInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            } else {
                Text(
                    "Quitar",
                    modifier = Modifier.clickable(onClick = onQuitar).padding(8.dp),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                )
            }
        }
        if (linea != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperMostrador(
                    etiqueta = "Cantidad de ${producto.name}",
                    cantidad = linea.quantity,
                    colors = colors,
                    onCantidad = onCantidad,
                )
            }
            producto.optionGroups.forEach { grupo ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        grupo.name + if (grupo.minimumSelections >= 1) " · obligatorio" else " · opcional",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        grupo.options.forEach { opcion ->
                            val elegida = opcion.id in linea.selectedOptionIds
                            Text(
                                opcion.name,
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (elegida) colors.accentLime else colors.cardInner)
                                        .selectable(
                                            selected = elegida,
                                            role = Role.Checkbox,
                                            onClick = { onOpcion(grupo, opcion.id) },
                                        ).padding(horizontal = 10.dp, vertical = 6.dp),
                                color = if (elegida) colors.accentInk else colors.textPrimary,
                                fontSize = 13.sp,
                            )
                        }
                    }
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
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Pedido de mostrador", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
        Text("Tomar pedido", fontSize = 13.sp, color = colors.textSecondary)
    }
}

@Composable
private fun StepperMostrador(
    etiqueta: String,
    cantidad: Int,
    colors: OperationalColors,
    onCantidad: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.semantics { contentDescription = etiqueta },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "−",
            modifier = Modifier.clickable(enabled = cantidad > 1) { onCantidad(-1) }.padding(horizontal = 12.dp),
            color = if (cantidad > 1) colors.textPrimary else colors.textMuted,
            fontWeight = FontWeight.Bold,
        )
        Text("$cantidad", color = colors.textPrimary, fontWeight = FontWeight.Bold)
        Text(
            "+",
            modifier =
                Modifier
                    .clickable(enabled = cantidad < COUNTER_LINE_QUANTITY_MAX) { onCantidad(1) }
                    .padding(horizontal = 12.dp),
            color = if (cantidad < COUNTER_LINE_QUANTITY_MAX) colors.textPrimary else colors.textMuted,
            fontWeight = FontWeight.Bold,
        )
    }
}
