package com.vaiinilla.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.SharedTableAmounts
import com.vaiinilla.app.domain.model.SharedTableOrder
import com.vaiinilla.app.domain.model.SharedTableParticipant
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.sharedtable.SharedTableUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
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

internal fun tableMoney(value: BigDecimal): String = moneyLabel(value.setScale(2).toPlainString())

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
    onOrderAlone: () -> Unit,
    /** Abre la cuenta de la mesa (Doop L04). */
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    /** Tipo del espacio del QR: en una barra la invitación habla de la barra, no de una mesa. */
    spaceType: String? = null,
) {
    if (!state.loaded) return
    val table = state.table
    if (table == null && qrToken == null) return
    if (table == null && state.orderedAlone) return
    // En Pedidos la mesa es un renglón; tocarlo abre su cuenta en una página (Doop L10 y L04).
    Surface(
        modifier = modifier.fillMaxWidth().arrive(0, key = table?.spaceId ?: "unirse"),
        color = OrderTrackingCardBg,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .animateContentSize(
                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                    ).padding(
                        PaddingValues(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    ),
        ) {
            if (table == null) {
                JoinTableRow(state, spaceType, onAliasChange, onJoin, onOrderAlone)
            } else {
                TableSummaryRow(table, onOpen = onOpen)
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

/** «Tu parte por pagar · $137»: lo que dice la mesa compartida en su renglón de Pedidos (Doop L10). */
internal fun tableSummaryLine(table: SharedTable): String {
    val share = table.myShare
    val people = table.participants.size
    return when {
        share.total.signum() == 0 -> (if (people <= 1) "Solo tú" else "$people personas") + " · sin pedidos aún"
        share.pending.signum() == 0 -> "Tu parte ya está pagada"
        else -> "Tu parte por pagar · ${tableMoney(share.pending)}"
    }
}

private val FaceColors =
    listOf(
        Color(0xFFA8D8F6) to Color(0xFF0F2E44),
        Color(0xFFFFD15B) to Color(0xFF3D2B00),
    )

/** Quiénes están en la mesa, cuánto te toca y la flecha que abre la cuenta completa. */
@Composable
private fun TableSummaryRow(
    table: SharedTable,
    onOpen: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { role = Role.Button }
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TableFaces(table.participants)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                SharedSpaceWords.of(table.spaceType).title,
                color = OrderTrackingCardText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                tableSummaryLine(table),
                color = OrderTrackingCardText.copy(alpha = 0.62f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = OrderTrackingCardText.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun TableFaces(people: List<SharedTableParticipant>) {
    val shown = people.take(2)
    val extra = people.size - shown.size
    Row(horizontalArrangement = Arrangement.spacedBy((-9).dp)) {
        shown.forEachIndexed { index, person ->
            val (bg, fg) = FaceColors[index]
            TableFace(
                person.alias
                    .trim()
                    .take(1)
                    .uppercase(),
                bg,
                fg,
            )
        }
        if (extra > 0) TableFace("+$extra", Color(0xFF33342F), OrderTrackingCardText.copy(alpha = 0.86f))
    }
}

@Composable
private fun TableFace(
    text: String,
    background: Color,
    content: Color,
) {
    Box(
        modifier =
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(background)
                .border(2.dp, OrderTrackingCardBg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = content, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

/** Sin unirse todavía: el renglón invita y la hoja de L03 pide el nombre. */
@Composable
private fun JoinTableRow(
    state: SharedTableUiState,
    spaceType: String?,
    onAliasChange: (String) -> Unit,
    onJoin: () -> Unit,
    onOrderAlone: () -> Unit,
) {
    val words = SharedSpaceWords.of(spaceType)
    var inviting by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { role = Role.Button }
                .physicalPress(scale = PhysicalPressScale.Small) { inviting = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-9).dp)) {
            val shown = state.othersAtTable.take(2)
            shown.forEachIndexed { index, name ->
                val (bg, fg) = FaceColors[index]
                TableFace(name.trim().take(1).uppercase(), bg, fg)
            }
            val extra = state.othersAtTable.size - shown.size
            if (extra > 0) TableFace("+$extra", Color(0xFF33342F), OrderTrackingCardText.copy(alpha = 0.86f))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "¿Compartes ${words.thePlace}?",
                color = OrderTrackingCardText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Únete con un nombre y paga solo tu parte",
                color = OrderTrackingCardText.copy(alpha = 0.62f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = OrderTrackingCardText.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp),
        )
    }
    if (inviting) {
        SharedTableInviteSheet(
            state = state,
            spaceName = null,
            spaceType = spaceType,
            onAliasChange = onAliasChange,
            onJoin = {
                onJoin()
                inviting = false
            },
            onOrderAlone = onOrderAlone,
            onDismiss = { inviting = false },
        )
    }
}

/** «Luis y Mariana ya están en la mesa.», con «y N más» después de dos nombres; null si no hay nadie. */
internal fun othersAtTableLine(
    names: List<String>,
    thePlace: String,
): String? {
    val shown = names.take(2)
    val rest = names.size - shown.size
    val who =
        when {
            shown.isEmpty() -> return null
            rest > 0 -> shown.joinToString(", ") + " y " + (if (rest == 1) "alguien más" else "$rest más")
            shown.size == 2 -> shown.joinToString(" y ")
            else -> shown.first()
        }
    val verb = if (names.size == 1) "ya está" else "ya están"
    return "$who $verb en $thePlace."
}
