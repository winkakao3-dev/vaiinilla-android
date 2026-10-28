package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.SharedTableAmounts
import com.vaiinilla.app.domain.model.SharedTableOrder
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialTextField
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.sharedtable.SharedTableUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import java.math.BigDecimal

/**
 * El estado de un pedido de la mesa. A la cuenta, "cobrado" solo dice que Cocina lo recibió:
 * mientras no se pague, está en la cuenta.
 */
internal fun tableOrderStateLabel(order: SharedTableOrder): String =
    when {
        order.pendingPayment && order.status == OrderState.PAID -> "En la cuenta"
        order.pendingPayment -> order.status.label
        else -> "${order.status.label} · pagado"
    }

/** Lado derecho de un pedido por cobrar: quién lo paga, o el botón y el pago_yo que envía. */
internal sealed interface TableClaim {
    data class OtherPays(
        val alias: String,
    ) : TableClaim

    data class Toggle(
        val label: String,
        val payIt: Boolean,
    ) : TableClaim
}

/**
 * Null si ya no está por cobrar. Quién paga sale de iPayIt (lo_pago_yo), no del alias: dos
 * personas pueden llamarse igual.
 */
internal fun tableOrderClaim(order: SharedTableOrder): TableClaim? =
    when {
        !order.pendingPayment -> null
        order.payer != null && !order.iPayIt -> TableClaim.OtherPays(order.payer)
        order.iPayIt -> TableClaim.Toggle("Lo pago yo ✓", payIt = false)
        else -> TableClaim.Toggle("Esto lo pago yo", payIt = true)
    }

private fun money(value: BigDecimal): String = moneyLabel(value.setScale(2).toPlainString())

/** Solo elige el texto: los montos de "tu parte" vienen de la API, sin recalcular. */
internal fun myShareHint(
    share: SharedTableAmounts,
    alias: String,
): String? =
    when {
        share.total.signum() == 0 -> null
        share.pending.signum() == 0 -> "Tu parte ya está pagada."
        else -> "Págala en caja o con tu mesero. Di tu nombre: $alias."
    }

/** Cómo se nombra el espacio compartido en la invitación: "Mesa compartida", "Barra compartida"... */
internal data class SharedSpaceWords(
    val title: String,
    val thePlace: String,
) {
    companion object {
        fun of(spaceType: String?): SharedSpaceWords {
            val noun = SpaceCopy.kindLabel(spaceType)
            val masculine = spaceType == "asiento" || spaceType == "drive_thru"
            return SharedSpaceWords(
                title = if (masculine) "$noun compartido" else "$noun compartida",
                thePlace = (if (masculine) "el " else "la ") + noun.lowercase(),
            )
        }
    }
}

/**
 * La mesa compartida en Mis pedidos, con la misma tarjeta oscura que los pedidos. Sin mesa,
 * invita a unirse con un alias si el cliente llegó por el QR de una mesa.
 */
@Composable
fun SharedTableCard(
    state: SharedTableUiState,
    qrToken: String?,
    onAliasChange: (String) -> Unit,
    onJoin: () -> Unit,
    onClaim: (folio: Int, payIt: Boolean) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    /** Tipo del espacio del QR: en una barra la invitación habla de la barra, no de una mesa. */
    spaceType: String? = null,
) {
    if (!state.loaded) return
    val table = state.table
    if (table == null && qrToken == null) return
    Surface(
        modifier = modifier.fillMaxWidth().arrive(0, key = table?.spaceId ?: "unirse"),
        color = OrderTrackingCardBg,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .animateContentSize(
                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                    ).padding(16.dp),
        ) {
            if (table == null) {
                JoinTable(state, SharedSpaceWords.of(spaceType), onAliasChange, onJoin)
            } else {
                TableContent(
                    table = table,
                    busy = state.busy,
                    onClaim = onClaim,
                    onLeave = onLeave,
                    claimError = state.error.takeIf { state.errorFolio != null },
                    claimErrorFolio = state.errorFolio,
                )
            }
            state.error?.takeIf { table == null || state.errorFolio == null }?.let {
                Text(
                    it,
                    color = LocalVaiinillaColors.current.coral,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun JoinTable(
    state: SharedTableUiState,
    words: SharedSpaceWords,
    onAliasChange: (String) -> Unit,
    onJoin: () -> Unit,
) {
    Text(
        words.title,
        color = OrderTrackingCardText.copy(alpha = 0.55f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
    )
    Text(
        "¿Compartes ${words.thePlace}?",
        color = OrderTrackingCardText,
        fontSize = 16.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 10.dp),
    )
    Text(
        "Únete con un nombre para ver los pedidos de todos y la cuenta de ${words.thePlace}. " +
            "Los demás solo ven ese nombre.",
        color = OrderTrackingCardText.copy(alpha = 0.62f),
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
    )
    // La tarjeta es oscura en cualquier tema: el campo toma el texto de la tarjeta, como en iOS.
    CompositionLocalProvider(
        LocalVaiinillaColors provides
            LocalVaiinillaColors.current.copy(
                ink = OrderTrackingCardText,
                muted = OrderTrackingCardText.copy(alpha = 0.55f),
                line = Color.White.copy(alpha = 0.18f),
            ),
    ) {
        EditorialTextField(
            value = state.aliasInput,
            onValueChange = onAliasChange,
            label = "Tu nombre en ${words.thePlace}",
            placeholder = "Ana",
        )
    }
    Spacer(Modifier.height(12.dp))
    EditorialAccentButton(text = "Unirme a ${words.thePlace}", onClick = onJoin, enabled = state.canJoin)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableContent(
    table: SharedTable,
    busy: Boolean,
    onClaim: (Int, Boolean) -> Unit,
    onLeave: () -> Unit,
    claimError: String? = null,
    claimErrorFolio: Int? = null,
) {
    val noun = SpaceCopy.kindLabel(table.spaceType).lowercase()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            table.spaceName,
            color = OrderTrackingCardText.copy(alpha = 0.55f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
        Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
            Text(
                (if (table.participants.size == 1) "Solo tú" else "${table.participants.size} personas").uppercase(),
                color = OrderTrackingCardText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
    FlowRow(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        table.participants.forEach { person ->
            TableChip(text = if (person.isMe) "${person.alias} (tú)" else person.alias, on = person.isMe)
        }
    }
    val groups = table.orderedGroups.filter { it.orders.isNotEmpty() }
    if (!table.accountOpen || groups.isEmpty()) {
        Text(
            "Aún no hay pedidos en la cuenta de la $noun.",
            color = OrderTrackingCardText.copy(alpha = 0.62f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 14.dp),
        )
    } else {
        groups.forEachIndexed { index, group ->
            Column(modifier = Modifier.padding(top = 14.dp).arrive(index + 1, key = group.alias)) {
                Text(
                    (if (group.isMe) "Tus pedidos" else group.alias ?: "Otros en la $noun").uppercase(),
                    color = OrderTrackingCardText.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                )
                group.orders.forEach { order ->
                    TableOrderRow(order, busy, onClaim, claimError.takeIf { order.folio == claimErrorFolio })
                }
            }
        }
    }
    CardRule()
    SumRow("Pagado", money(table.totals.paid))
    SumRow("Por pagar de la $noun", money(table.totals.pending))
    CardRule()
    SumRow("Tu parte", money(table.myShare.total))
    SumRow("Tu parte pagada", money(table.myShare.paid))
    SumRow("Tu parte por pagar", money(table.myShare.pending), highlight = true)
    myShareHint(table.myShare, table.myAlias)?.let {
        Text(
            it,
            color = OrderTrackingCardText.copy(alpha = 0.62f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
    var confirmingLeave by remember { mutableStateOf(false) }
    SeaGlassButton(
        tone = SeaGlassTone.vainilla,
        onClick = { confirmingLeave = true },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        enabled = !busy,
    ) {
        Text("Salir de la $noun", fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
    if (confirmingLeave) {
        // Un toque suelto no debe sacar a nadie de una cuenta con partes sin pagar (COMPRA-19).
        val pendingLine =
            if (table.myShare.pending.signum() > 0) {
                " Aún tienes ${moneyLabel(table.myShare.pending)} por pagar de tu parte."
            } else {
                ""
            }
        AlertDialog(
            onDismissRequest = { confirmingLeave = false },
            title = { Text("¿Salir de la $noun?") },
            text = { Text("Dejarás de ver la cuenta compartida.$pendingLine") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingLeave = false
                        onLeave()
                    },
                ) { Text("Salir") }
            },
            dismissButton = { TextButton(onClick = { confirmingLeave = false }) { Text("Quedarme") } },
        )
    }
}

@Composable
private fun TableOrderRow(
    order: SharedTableOrder,
    busy: Boolean,
    onClaim: (Int, Boolean) -> Unit,
    error: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    order.itemsSummary.ifBlank { "Pedido #${order.folio}" },
                    color = OrderTrackingCardText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(tableOrderStateLabel(order), color = OrderTrackingCardText.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(order.total), color = OrderTrackingCardText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                when (val claim = tableOrderClaim(order)) {
                    is TableClaim.OtherPays ->
                        Text(
                            "Paga ${claim.alias}",
                            color = OrderTrackingCardText.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    is TableClaim.Toggle ->
                        TableChip(
                            text = claim.label,
                            on = order.iPayIt,
                            enabled = !busy,
                            onClick = { onClaim(order.folio, claim.payIt) },
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    null -> Unit
                }
            }
        }
        error?.let {
            Text(
                it,
                color = LocalVaiinillaColors.current.coral,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(1.dp)
                .border(0.5.dp, Color.White.copy(alpha = 0.12f)),
        )
    }
}

/** Escala del texto nuevo: rebota como un toggle. */
private val ChipBounce = spring<Float>(dampingRatio = 0.44f, stiffness = 420f)

/** Fundido del texto: suave, sin rebote, para que el viejo no se vea encima del nuevo. */
private val ChipFade = spring<Float>(dampingRatio = 0.89f, stiffness = 180f)

/**
 * Píldora de la mesa, encendida en lima. Al cambiar el texto, el nuevo entra con rebote;
 * deshabilitada se atenúa.
 */
@Composable
private fun TableChip(
    text: String,
    on: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    val reduce = reducedMotion()
    val bg by animateColorAsState(if (on) colors.accent else Color.Transparent, label = "chip-bg")
    val fg by animateColorAsState(if (on) colors.accentInk else OrderTrackingCardText, label = "chip-fg")
    val dim by animateFloatAsState(if (enabled) 1f else 0.5f, label = "chip-dim")
    val base =
        modifier
            .graphicsLayer { alpha = dim }
            .border(1.dp, if (on) Color.Transparent else Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
    Surface(
        modifier =
            if (onClick != null) {
                base
                    .semantics {
                        role = Role.Button
                        selected = on
                    }.physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
            } else {
                base
            },
        color = bg,
        shape = RoundedCornerShape(999.dp),
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                if (reduce) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    (fadeIn(ChipFade) + scaleIn(ChipBounce, initialScale = 0.92f)) togetherWith
                        fadeOut(ChipFade) using SizeTransform(clip = false)
                }
            },
            label = "chip-label",
        ) { label ->
            Text(
                label,
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun SumRow(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = if (highlight) colors.accent else OrderTrackingCardText.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = if (highlight) FontWeight.Black else FontWeight.Normal,
        )
        Text(
            value,
            color = OrderTrackingCardText,
            fontSize = 14.sp,
            fontWeight = if (highlight) FontWeight.Black else FontWeight.Normal,
        )
    }
}

@Composable
private fun CardRule() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(1.dp)
                .border(0.5.dp, Color.White.copy(alpha = 0.12f)),
    )
}
