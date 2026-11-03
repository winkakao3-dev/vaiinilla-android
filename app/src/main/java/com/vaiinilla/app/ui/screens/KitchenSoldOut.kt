package com.vaiinilla.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** N03: cuántas comandas por hacer todavía llevan un producto, para avisar al apagarlo. */
internal fun kitchenOrdersWithProduct(
    queue: List<OrderDetail>,
    productName: String,
): Int = queue.count { order -> order.activeItems.any { it.productName.equals(productName, ignoreCase = true) } }

/**
 * Lo de Cocina primero, y dentro de cada grupo por nombre; lo apagado no se esconde para poder volver a prenderlo.
 * Lo archivado ya no está en el menú y no se ofrece.
 */
internal fun kitchenSoldOutProducts(products: List<Product>): List<Product> =
    products.filterNot { it.archived }.sortedWith(compareBy<Product> { it.preparationStation != PreparationStation.KITCHEN }.thenBy { it.name.lowercase() })

/** N03 «¿Qué se acabó?»: Cocina apaga un producto y deja de salir en el menú del cliente y en Caja. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KitchenSoldOutSheet(
    products: List<Product>?,
    failed: Boolean,
    busy: Boolean,
    queue: List<OrderDetail>,
    lastTurnedOff: String?,
    errorMessage: String?,
    colors: OperationalColors,
    onToggle: (Product, Boolean) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        KitchenSoldOutContent(
            products = products,
            failed = failed,
            busy = busy,
            queue = queue,
            lastTurnedOff = lastTurnedOff,
            errorMessage = errorMessage,
            colors = colors,
            onToggle = onToggle,
            onRetry = onRetry,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
internal fun KitchenSoldOutContent(
    products: List<Product>?,
    failed: Boolean,
    busy: Boolean,
    queue: List<OrderDetail>,
    lastTurnedOff: String?,
    errorMessage: String?,
    colors: OperationalColors,
    onToggle: (Product, Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coral = LocalVaiinillaColors.current.coral
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp)) {
        Text("¿Qué se acabó?", fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.9).sp, color = colors.textPrimary)
        Text(
            "Apaga un producto y deja de salir en el menú del cliente y en Caja. Prenderlo lo regresa.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        val warnCount = lastTurnedOff?.let { kitchenOrdersWithProduct(queue, it) } ?: 0
        val message = errorMessage ?: if (warnCount > 0) {
            val comandas = if (warnCount == 1) "1 comanda en fila lleva" else "$warnCount comandas en fila llevan"
            "$comandas $lastTurnedOff. Revísalas y quítalo con «No hay»."
        } else {
            null
        }
        if (message != null) {
            Text(
                message,
                color = coral,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier =
                    Modifier
                        .padding(bottom = 12.dp)
                        .fillMaxWidth()
                        .arrive(key = message)
                        .clip(RoundedCornerShape(18.dp))
                        .background(coral.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
        when {
            products == null && failed ->
                OperationalErrorCard(
                    title = "No se pudo cargar el menú",
                    message = "Revisa la conexión. Si sigue igual, tu cuenta de Cocina aún no puede ver los productos; mientras, pídeselo a Caja.",
                    colors = colors,
                    onRetry = onRetry,
                )
            products == null ->
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                }
            products.isEmpty() ->
                Text("Todavía no hay productos en el menú.", color = colors.textSecondary, fontSize = 14.sp)
            else -> {
                val sorted = kitchenSoldOutProducts(products)
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.cardBackground),
                ) {
                    itemsIndexed(sorted, key = { _, it -> it.id }) { index, product ->
                        if (index > 0) HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 14.dp))
                        KitchenSoldOutRow(
                            product = product,
                            enabled = !busy,
                            colors = colors,
                            onToggle = { onToggle(product, it) },
                            modifier = Modifier.animateItem().arrive(index, product.id),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KitchenSoldOutRow(
    product: Product,
    enabled: Boolean,
    colors: OperationalColors,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVaiinillaHaptics()
    val coral = LocalVaiinillaColors.current.coral
    val on = product.available
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(scale = PhysicalPressScale.Small, enabled = enabled) {
                    if (on) haptics.impact() else haptics.success()
                    onToggle(!on)
                }.semantics(mergeDescendants = true) {
                    role = Role.Switch
                    stateDescription = if (on) "Hay" else "Agotado"
                    contentDescription = product.name
                }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(colors.cardInner).alpha(if (on) 1f else 0.5f),
        ) {
            ProductImage(imageUrl = product.imageUrl, contentDescription = product.name, modifier = Modifier.size(46.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                product.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (on) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when {
                    !on -> "Agotado · no sale en el menú"
                    product.preparationStation == PreparationStation.KITCHEN -> "Lo hace Cocina"
                    else -> "Lo entrega Caja"
                },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (on) colors.textSecondary else coral,
            )
        }
        KitchenSwitch(on = on, colors = colors)
    }
}

/** El interruptor del dibujo: lima con la perilla a la derecha si hay, gris a la izquierda si se acabó. */
@Composable
private fun KitchenSwitch(
    on: Boolean,
    colors: OperationalColors,
) {
    val reduced = reducedMotion()
    val track by animateColorAsState(
        if (on) colors.accentLime else colors.cardInner,
        if (reduced) snap() else spring(stiffness = Spring.StiffnessMedium),
        label = "soldout-track",
    )
    val knob by animateColorAsState(
        if (on) colors.accentInk else Color(0xFF8D8B82),
        if (reduced) snap() else spring(stiffness = Spring.StiffnessMedium),
        label = "soldout-knob",
    )
    val x by animateDpAsState(
        if (on) 23.dp else 3.dp,
        if (reduced) snap() else spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "soldout-x",
    )
    Box(modifier = Modifier.size(width = 52.dp, height = 32.dp).clip(CircleShape).background(track)) {
        Box(modifier = Modifier.offset(x = x, y = 3.dp).size(26.dp).clip(CircleShape).background(knob))
    }
}
