package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.domain.model.needsCashCollection
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.orderStateLabel
import com.vaiinilla.app.ui.components.physicalPress
import java.text.Normalizer

/** Qué pedidos de hoy entran en la búsqueda de Caja. */
internal enum class CashierSearchFilter(
    val label: String,
) {
    TODAY("Hoy"),
    TO_COLLECT("Por cobrar"),
    READY("Listos"),
    DELIVERED("Entregados"),
}

/**
 * «Vengo por el de Diego»: busca por nombre (sin importar acentos ni mayúsculas) o por folio entre los
 * pedidos que Caja ya tiene. Lo más nuevo primero.
 */
internal fun cashierOrderSearch(
    orders: List<OrderDetail>,
    query: String,
    filter: CashierSearchFilter,
): List<OrderDetail> {
    val needle = searchKey(query.trim().removePrefix("#"))
    return orders
        .filter { order ->
            when (filter) {
                CashierSearchFilter.TODAY -> true
                CashierSearchFilter.TO_COLLECT -> order.summary.needsCashCollection
                CashierSearchFilter.READY -> order.summary.state == OrderState.READY
                CashierSearchFilter.DELIVERED -> order.summary.state == OrderState.DELIVERED
            }
        }.filter { order ->
            needle.isEmpty() ||
                order.summary.folio.toString() == needle ||
                searchKey(staffCustomerName(order).orEmpty()).contains(needle)
        }.sortedByDescending { it.summary.createdAt }
}

private fun searchKey(text: String): String =
    Normalizer
        .normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()

/** La lupa junto a «Por cobrar»: abre la búsqueda sin empujar la fila hacia abajo. */
@Composable
internal fun CashierSearchButton(
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, CircleShape)
                .physicalPress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = "Buscar un pedido", tint = colors.textPrimary, modifier = Modifier.size(22.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierOrderSearchSheet(
    orders: List<OrderDetail>,
    colors: OperationalColors,
    canOpen: (OrderDetail) -> Boolean,
    onDismiss: () -> Unit,
    onOpen: (OrderDetail) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CashierSearchFilter.TODAY) }
    val results = remember(orders, query, filter) { cashierOrderSearch(orders, query, filter) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "¿Cuál es el tuyo?",
                color = colors.textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                letterSpacing = (-1).sp,
            )
            Text(
                "El cliente dice «vengo por el de Diego» y lo encuentras sin bajar por toda la fila.",
                color = colors.textSecondary,
                fontSize = 13.sp,
            )
            OperationalField(
                value = query,
                onValueChange = { query = it },
                label = "Nombre o folio",
                colors = colors,
                placeholder = "Diego, 11…",
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CashierSearchFilter.entries.forEach { option ->
                    val selected = option == filter
                    val background =
                        animateSelectionColor(if (selected) colors.textPrimary else colors.cardBackground, "search-chip")
                    val ink = animateSelectionColor(if (selected) colors.background else colors.textPrimary, "search-ink")
                    Box(
                        modifier =
                            Modifier
                                .heightIn(min = 44.dp)
                                .clip(CircleShape)
                                .background(background)
                                .border(1.dp, if (selected) Color.Transparent else colors.cardBorder, CircleShape)
                                .physicalPress(onClick = { filter = option })
                                .semantics { this.selected = selected }
                                .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(option.label, color = ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            if (results.isEmpty()) {
                Text(
                    if (query.isBlank()) "No hay pedidos aquí." else "Ningún pedido de hoy se llama así.",
                    color = colors.textSecondary,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(22.dp)),
                ) {
                    results.forEachIndexed { index, order ->
                        if (index > 0) HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                        CashierSearchRow(order, colors, enabled = canOpen(order)) { onOpen(order) }
                    }
                }
            }
            Text(
                "También busca por folio: escribe «11» y sale el #11. Por ahora solo busca en los pedidos de hoy.",
                color = colors.textSecondary,
                fontSize = 12.5.sp,
            )
        }
    }
}

@Composable
private fun CashierSearchRow(
    order: OrderDetail,
    colors: OperationalColors,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val summary = order.summary
    val who =
        listOfNotNull(
            SpaceCopy.staffDestination(summary.destination, summary.space, summary.pickUpAtBar),
            staffCustomerName(order),
        ).joinToString(" · ")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .physicalPress(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(colors.cardInner),
            contentAlignment = Alignment.Center,
        ) {
            Text("#${summary.folio}", color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(who, color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                order.activeItems.joinToString(", ") { it.productName },
                color = colors.textSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(moneyLabel(summary.total), color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(
                // Un pedido a la cuenta sin pagar no está «Cobrado» (R30).
                (if (summary.payAtEnd && summary.paymentPending) "A la cuenta" else orderStateLabel(summary))
                    .uppercase(),
                color = colors.textSecondary,
                fontWeight = FontWeight.Black,
                fontSize = 10.5.sp,
                letterSpacing = 0.6.sp,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(colors.pillBackground)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
