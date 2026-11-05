package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.SharedTableOrder
import com.vaiinilla.app.domain.model.SharedTableRent
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.ui.components.ClientCap
import com.vaiinilla.app.ui.components.ClientCard
import com.vaiinilla.app.ui.components.ClientCardDivider
import com.vaiinilla.app.ui.components.ClientPillButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.RollingText
import com.vaiinilla.app.ui.components.WaiterCallHost
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.initialOf
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.order.MenuWaiterCallState
import com.vaiinilla.app.ui.order.MenuWaiterPhase
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.launch

const val SHARED_TABLE_PAY_TAG = "shared-table-pay"

/** «Tu parte + lo de Mariana»: lo que cubre el monto de abajo, con los nombres de lo que dijo que paga. */
internal fun tablePayLine(table: SharedTable): String {
    val claimed =
        table.orderedGroups
            .filter { group -> !group.isMe && group.orders.any { it.iPayIt && it.pendingPayment } }
            .mapNotNull { it.alias }
            .distinct()
    return when (claimed.size) {
        0 -> "Tu parte"
        1 -> "Tu parte + lo de ${claimed.first()}"
        else -> "Tu parte + lo de " + claimed.dropLast(1).joinToString(", ") + " y " + claimed.last()
    }
}

/** «Renta de pista · pagada $300 (2 h) · 4 jugadores». */
internal fun sharedRentLine(rent: SharedTableRent): String =
    buildString {
        append("Renta de ${Venue.court} · pagada ${tableMoney(rent.paid)}")
        val minutes = rent.minutes
        if (minutes > 0) {
            append(" (")
            append(
                when {
                    minutes % 60 == 0 -> "${minutes / 60} h"
                    minutes > 60 -> "${minutes / 60} h ${minutes % 60}"
                    else -> "$minutes min"
                },
            )
            append(")")
        }
        rent.players?.let { append(" · $it ${if (it == 1) "jugador" else "jugadores"}") }
    }

private data class OtherRow(
    val alias: String,
    val order: SharedTableOrder?,
)

/**
 * La cuenta de la mesa compartida (Doop L04): tu parte y la de toda la mesa, tus pedidos, los de los demás con
 * «Esto lo pago yo», tu mesero y el pago de tu parte. Se abre desde el renglón de la mesa en Pedidos.
 */
@Composable
fun SharedTablePage(
    table: SharedTable,
    busy: Boolean,
    error: String?,
    errorFolio: Int?,
    onClaim: (folio: Int, payIt: Boolean) -> Unit,
    onLeave: () -> Unit,
    /** La llamada al mesero de la mesa; null si el negocio no la tiene. */
    waiter: MenuWaiterCallState?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val scope = rememberCoroutineScope()
    val canCallWaiter = waiter != null && waiter.phase != MenuWaiterPhase.UNAVAILABLE
    if (waiter != null) WaiterCallHost(state = waiter, place = SpaceCopy.named(table.spaceType, table.spaceName))
    var confirmingLeave by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().background(colors.paper)) {
        Column(
            Modifier
                .weight(1f)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            TableHeader(table, busy, onLeave = { confirmingLeave = true }, modifier = Modifier.arrive(0))
            ShareTiles(table, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp).arrive(1))
            table.rent?.let { rent ->
                // U05: la renta ya se pagó al apartar; se ve aparte y no entra en lo que falta.
                Text(
                    sharedRentLine(rent),
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp).arrive(1),
                )
            }
            MyOrders(table, error.takeIf { errorFolio != null }, errorFolio, modifier = Modifier.arrive(2))
            OthersAtTable(table, busy, onClaim, error.takeIf { errorFolio != null }, errorFolio, modifier = Modifier.arrive(3))
            if (canCallWaiter) {
                WaiterCard(waiter!!, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).arrive(4))
            }
            error?.takeIf { errorFolio == null }?.let {
                Text(
                    it,
                    color = colors.coralText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        if (table.myShare.pending.signum() > 0) {
            PayBar(
                table = table,
                onPay =
                    if (canCallWaiter) {
                        {
                            // Con una llamada abierta se muestra esa; si no, se pide la cuenta.
                            if (waiter!!.call != null) waiter.tap() else scope.launch { waiter.send(CallReason.CUENTA) }
                        }
                    } else {
                        null
                    },
            )
        }
    }
    if (confirmingLeave) {
        val noun = SharedSpaceWords.of(table.spaceType).thePlace
        // Un toque suelto no debe sacar a nadie de una cuenta con partes sin pagar (COMPRA-19).
        val pendingLine =
            if (table.myShare.pending.signum() > 0) " Aún tienes ${tableMoney(table.myShare.pending)} por pagar de tu parte." else ""
        AlertDialog(
            onDismissRequest = { confirmingLeave = false },
            title = { Text("¿Salir de $noun?") },
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
private fun TableHeader(
    table: SharedTable,
    busy: Boolean,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val people = table.participants.size
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                SharedSpaceWords.of(table.spaceType).title + " · " + (if (people <= 1) "solo tú" else "$people personas"),
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                table.spaceName,
                color = colors.ink,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.1).sp,
            )
        }
        Box(
            modifier =
                Modifier
                    .height(38.dp)
                    .graphicsLayer { alpha = if (busy) 0.5f else 1f }
                    .physicalPress(enabled = !busy, scale = PhysicalPressScale.Small, onClick = onLeave)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.paper2)
                    .semantics { role = Role.Button }
                    .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Salir", color = colors.ink2, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ShareTiles(
    table: SharedTable,
    modifier: Modifier = Modifier,
) {
    // «Tu parte» es lo tuyo; lo que dijiste que pagas de otros se suma abajo, en el pago.
    val mine = table.groups.firstOrNull { it.isMe }?.amounts
    val (mineValue, mineNote) =
        when {
            mine == null || mine.total.signum() == 0 -> "$0" to "Sin pedidos aún"
            mine.pending.signum() > 0 -> tableMoney(mine.pending) to "Por pagar"
            else -> tableMoney(mine.total) to "Ya pagada"
        }
    val all = table.totals
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ShareTile("Tu parte", mineValue, mineNote, Modifier.weight(1f))
        ShareTile(
            "Toda ${SharedSpaceWords.of(table.spaceType).thePlace}",
            tableMoney(all.total),
            when {
                // R21: lo abonado ya está en caja aunque los pedidos sigan abiertos; se dice cuánto falta.
                all.paidIn.signum() > 0 ->
                    "${tableMoney(all.paid + all.paidIn)} pagado · faltan ${tableMoney(all.remaining)}"
                all.paid.signum() > 0 -> "${tableMoney(all.paid)} ya pagado"
                else -> "Nada pagado aún"
            },
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun ShareTile(
    cap: String,
    value: String,
    note: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(colors.paper2)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        ClientCap(cap)
        RollingText(
            text = value,
            style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Black),
            color = colors.ink,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(note, color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun MyOrders(
    table: SharedTable,
    claimError: String?,
    claimErrorFolio: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val orders = table.groups.firstOrNull { it.isMe }?.orders.orEmpty()
    Column(modifier) {
        ClientCap("Tus pedidos", Modifier.padding(start = 20.dp, top = 26.dp, bottom = 10.dp))
        ClientCard(Modifier.padding(horizontal = 16.dp)) {
            if (orders.isEmpty()) {
                TableRow(
                    title = "Aún no pides nada",
                    subtitle = "Lo que pidas aquí sale en esta cuenta",
                )
            }
            orders.forEachIndexed { index, order ->
                if (index > 0) ClientCardDivider()
                val payer = (tableOrderClaim(order) as? TableClaim.OtherPays)?.alias
                TableRow(
                    title = "Pedido #${order.folio}" + order.itemsSummary.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                    subtitle = listOfNotNull(tableOrderStateLabel(order), payer?.let { "lo paga $it" }).joinToString(" · "),
                    error = claimError.takeIf { order.folio == claimErrorFolio },
                ) {
                    Text(tableMoney(order.total), color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private val OtherTints =
    listOf(
        Color(0xFFA8D8F6) to Color(0xFF0F2E44),
        Color(0xFFFFD15B) to Color(0xFF3D2B00),
    )

@Composable
private fun OthersAtTable(
    table: SharedTable,
    busy: Boolean,
    onClaim: (Int, Boolean) -> Unit,
    claimError: String?,
    claimErrorFolio: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val others = table.orderedGroups.filter { !it.isMe }
    val withOrders = others.filter { it.orders.isNotEmpty() }
    val rows =
        withOrders.flatMap { group -> group.orders.map { OtherRow(group.alias ?: "Alguien", it) } } +
            table.participants
                .filter { person -> !person.isMe && withOrders.none { it.alias == person.alias } }
                .map { OtherRow(it.alias, null) }
    if (rows.isEmpty()) return
    val aliases = rows.map { it.alias }.distinct()
    Column(modifier) {
        ClientCap("Otros en ${SharedSpaceWords.of(table.spaceType).thePlace}", Modifier.padding(start = 20.dp, top = 26.dp, bottom = 10.dp))
        ClientCard(Modifier.padding(horizontal = 16.dp)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) ClientCardDivider()
                val tint = OtherTints.getOrNull(aliases.indexOf(row.alias)) ?: (colors.paper to colors.ink2)
                val order = row.order
                TableRow(
                    title = if (order != null) "${row.alias} · #${order.folio}" else row.alias,
                    subtitle =
                        if (order != null) {
                            listOf(order.itemsSummary, tableMoney(order.total)).filter { it.isNotBlank() }.joinToString(" · ")
                        } else {
                            "Aún no pide"
                        },
                    leading = { Initial(row.alias, tint.first, tint.second) },
                    error = claimError.takeIf { order != null && order.folio == claimErrorFolio },
                ) {
                    if (order != null) {
                        when (val claim = tableOrderClaim(order)) {
                            null -> PaidTag()
                            is TableClaim.OtherPays ->
                                Text("Lo paga ${claim.alias}", color = colors.muted, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                            is TableClaim.Toggle ->
                                PayPill(
                                    // Encendida, la píldora ya dice que es tuyo: sin la palomita del renglón viejo.
                                    text = if (claim.payIt) "Esto lo pago yo" else "Lo pago yo",
                                    on = order.iPayIt,
                                    enabled = !busy,
                                    onClick = { onClaim(order.folio, claim.payIt) },
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    title: String,
    subtitle: String,
    leading: (@Composable () -> Unit)? = null,
    error: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            leading?.invoke()
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
            }
            trailing()
        }
        error?.let {
            Text(
                it,
                color = colors.coralText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun Initial(
    alias: String,
    fill: Color,
    ink: Color,
) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(fill).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(initialOf(alias), color = ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PaidTag() {
    val colors = LocalVaiinillaColors.current
    Box(
        Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(colors.accent.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Pagado",
            color = if (colors.isDark) colors.accent else colors.accentInk,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** Escala del texto nuevo: rebota como un interruptor. */
private val PillBounce = spring<Float>(dampingRatio = 0.44f, stiffness = 420f)

/** Fundido del texto, sin rebote, para que el viejo no se vea encima del nuevo. */
private val PillFade = spring<Float>(dampingRatio = 0.89f, stiffness = 180f)

/** «Esto lo pago yo»: se enciende en lima al tomarlo y el texto nuevo entra con rebote. */
@Composable
private fun PayPill(
    text: String,
    on: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val reduce = reducedMotion()
    val fill by animateColorAsState(if (on) colors.accent else Color.Transparent, label = "pay-fill")
    val ink by animateColorAsState(if (on) colors.accentInk else colors.ink2, label = "pay-ink")
    val edge by animateColorAsState(if (on) colors.accent else colors.line, label = "pay-edge")
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier =
            Modifier
                .height(32.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
                .semantics {
                    role = Role.Button
                    selected = on
                }.physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .clip(shape)
                .background(fill)
                .border(1.5.dp, edge, shape)
                .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                if (reduce) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    (fadeIn(PillFade) + scaleIn(PillBounce, initialScale = 0.92f)) togetherWith
                        fadeOut(PillFade) using SizeTransform(clip = false)
                }
            },
            label = "pay-pill-text",
        ) { label ->
            Text(label, color = ink, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

@Composable
private fun WaiterCard(
    waiter: MenuWaiterCallState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val call = waiter.call
    // El cliente nunca ve el nombre del mesero (Kao, 3 nov).
    val subtitle =
        when (call?.status) {
            null -> "Te atiende toda la mesa"
            CallStatus.EN_CAMINO -> "Va en camino"
            else -> "Ya le avisamos"
        }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.paper2)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(colors.paper).clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Tu mesero", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
        }
        val waiting = call == null && waiter.cooldown > 0
        Box(
            modifier =
                Modifier
                    .height(32.dp)
                    .graphicsLayer { alpha = if (waiting) 0.5f else 1f }
                    .physicalPress(enabled = !waiting, scale = PhysicalPressScale.Small, onClick = waiter::tap)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.ink)
                    .semantics { role = Role.Button }
                    .testTag("shared-table-call-waiter")
                    .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    call != null -> "Ver"
                    waiting -> "${waiter.cooldown} s"
                    else -> "Llamar"
                },
                color = colors.paper,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun PayBar(
    table: SharedTable,
    onPay: (() -> Unit)?,
) {
    val colors = LocalVaiinillaColors.current
    val amount = tableMoney(table.myShare.pending)
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.paper),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).semantics(mergeDescendants = true) {}) {
                Text(tablePayLine(table), color = colors.ink2, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(amount, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            if (onPay != null) {
                ClientPillButton(
                    text = "Pagar $amount con tu mesero",
                    onClick = onPay,
                    modifier = Modifier.testTag(SHARED_TABLE_PAY_TAG),
                )
                Text(
                    "O págala en caja. Di tu nombre: ${table.myAlias}.",
                    color = colors.muted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                )
            } else {
                myShareHint(table.myShare, table.myAlias)?.let {
                    Text(it, color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
