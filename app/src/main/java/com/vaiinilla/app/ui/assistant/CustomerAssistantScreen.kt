package com.vaiinilla.app.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.assistant.CustomerAssistantJson
import com.vaiinilla.app.domain.assistant.AssistantCourtCard
import com.vaiinilla.app.domain.assistant.AssistantOrderLine
import com.vaiinilla.app.domain.assistant.AssistantOrderProposal
import com.vaiinilla.app.domain.assistant.AssistantProductCard
import com.vaiinilla.app.domain.assistant.AssistantWaiterOffer
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import kotlinx.coroutines.launch

/** Lo que el asistente de pedidos puede hacer fuera de la plática; lo conecta la navegación. */
class CustomerAssistantActions(
    /** Un producto suelto: lo agrega o, si pide opciones, abre su ficha en el menú. true si quedó en el carrito. */
    val addProduct: (AssistantProductCard) -> Boolean,
    /**
     * El pedido propuesto al carrito, quitando antes lo que puso el pedido anterior ([replacing]). Regresa lo que no
     * entró (agotado, pide opciones…).
     */
    val addOrder: (order: AssistantOrderProposal, replacing: List<AssistantOrderLine>) -> List<String>,
    val openCart: () -> Unit,
    /** Tocar la tarjeta de un producto abre su ficha (la misma del menú). */
    val openProduct: (AssistantProductCard) -> Unit,
    /** Tocar una pista o su «Rentar» abre Reservas con esa pista elegida. */
    val openCourt: (AssistantCourtCard) -> Unit,
    /** null: aquí no hay a quién llamar (sin mesa o sin llamadas). */
    val callWaiter: (suspend (AssistantWaiterOffer) -> Result<Unit>)?,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerAssistantScreen(
    state: CustomerAssistantState,
    venueName: String?,
    place: String?,
    customerName: String?,
    actions: CustomerAssistantActions,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onRestart: () -> Unit,
    onTurnDone: (Int) -> Unit,
    onOrderInCart: (Int, List<AssistantOrderLine>) -> Unit,
    onSubmitName: (String) -> Unit,
    onBack: () -> Unit,
    guestName: String = "",
) {
    val colors = LocalVaiinillaColors.current
    var draft by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    // Por turno: lo que no entró al carrito o cómo salió la llamada al mesero.
    val notes = remember(state.conversation) { mutableStateMapOf<Int, String>() }
    val added = remember(state.conversation) { mutableStateMapOf<Int, Boolean>() }
    val scope = rememberCoroutineScope()
    val rows = state.turns.size + 2
    LaunchedEffect(state.turns.size, state.thinking, state.error, state.needsName) {
        list.animateScrollToItem((rows - 1).coerceAtLeast(0))
    }
    val send = { text: String ->
        if (state.canSend && text.isNotBlank()) {
            onSend(text)
            draft = ""
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Header(venueName, place, colors, showRestart = state.turns.isNotEmpty(), onBack = onBack, onRestart = onRestart)
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (state.turns.isEmpty()) {
                item(key = "bienvenida") { Welcome(customerName, place, colors, onPick = send) }
            }
            itemsIndexed(state.turns, key = { index, _ -> index }) { index, turn ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bubble(turn.text, mine = turn.fromCustomer, colors = colors)
                    turn.products.forEach { card ->
                        ProductRow(
                            card = card,
                            added = added[card.productId] == true,
                            colors = colors,
                            onAdd = { if (actions.addProduct(card)) added[card.productId] = true },
                            onOpen = { actions.openProduct(card) },
                        )
                    }
                    turn.courts.forEach { court ->
                        CourtRow(court = court, colors = colors, onRent = { actions.openCourt(court) })
                    }
                    turn.order?.let { order ->
                        OrderCard(
                            order = order,
                            place = place,
                            done = index in state.done,
                            superseded = index != state.latestOrderTurn,
                            note = notes[index],
                            colors = colors,
                            onChange = { draft = "Quiero cambiar " },
                            onAdd = {
                                val skipped = actions.addOrder(order, state.cartOrder)
                                onOrderInCart(index, order.lines.filterNot { it.product.name in skipped })
                                if (skipped.isEmpty()) {
                                    actions.openCart()
                                } else {
                                    notes[index] =
                                        "No agregué ${skipped.joinToString(", ")}: elige sus opciones en el menú o ya no hay."
                                }
                            },
                            onOpenCart = actions.openCart,
                        )
                    }
                    val call = actions.callWaiter
                    turn.waiter?.takeIf { call != null }?.let { offer ->
                        WaiterCard(
                            offer = offer,
                            done = index in state.done,
                            note = notes[index],
                            colors = colors,
                            onCall = {
                                scope.launch {
                                    notes[index] = "Llamando…"
                                    requireNotNull(call)(offer).fold(
                                        onSuccess = {
                                            onTurnDone(index)
                                            notes[index] = "Listo, ya le avisamos. Va para allá."
                                        },
                                        onFailure = {
                                            notes[index] = it.toUserFacingMessage("No se pudo llamar. Usa el botón Mesero.")
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            }
            item(key = "estado") {
                when {
                    state.needsName ->
                        NameCard(guestName, state.enteringName, state.nameError, colors, onSubmitName)
                    state.thinking -> Bubble("Escribiendo…", mine = false, colors = colors, muted = true)
                    state.error != null ->
                        Column(Modifier.padding(start = 36.dp)) {
                            Text(state.error, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.ink2)
                            if (!state.closed) Chip("Reintentar", colors, Modifier.padding(top = 8.dp), onRetry)
                        }
                    state.options.isNotEmpty() ->
                        FlowRow(
                            Modifier.padding(start = 36.dp, top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.options.forEach { option -> Chip(option, colors) { send(option) } }
                        }
                    else -> Spacer(Modifier.height(1.dp))
                }
            }
        }
        if (state.turns.isEmpty()) {
            Text(
                "Solo hablo del menú y de tu pedido aquí. Para lo demás, llama al mesero. " +
                    "Tus mensajes los procesa una IA de otra empresa: no compartas datos personales ni de salud.",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            )
        } else if (state.remaining <= 1) {
            Text(
                "Ya platicamos mucho. Empieza una plática nueva con el botón de arriba.",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        Composer(draft, { draft = it.take(CustomerAssistantJson.MAX_LETTERS) }, state.canSend, colors) { send(draft) }
    }
}

@Composable
private fun Header(
    venueName: String?,
    place: String?,
    colors: VaiinillaColors,
    showRestart: Boolean,
    onBack: () -> Unit,
    onRestart: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, "Regresar al menú", colors, onBack)
        Box(
            Modifier.padding(start = 10.dp).size(40.dp).clip(RoundedCornerShape(13.dp)).background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = colors.accentInk, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                "Asistente",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            val subtitle = listOfNotNull(venueName, place?.replaceFirstChar(Char::uppercase)).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showRestart) RoundButton(Icons.Rounded.Refresh, "Empezar de nuevo", colors, onRestart)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Welcome(
    customerName: String?,
    place: String?,
    colors: VaiinillaColors,
    onPick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.paper2)
                .border(1.dp, colors.line, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Caps(customerName?.let { "Hola, $it" } ?: "Hola", colors.accent)
            Text(
                if (place != null) "¿Qué se te antoja? Te ayudo a pedir para $place." else "¿Qué se te antoja? Te ayudo a pedir.",
                fontSize = 23.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Black,
                color = colors.ink,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Conozco el menú de hoy, lo que ya se acabó y los precios. Nada se pide hasta que tú lo confirmes.",
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Caps("Para empezar", colors.muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            STARTERS.forEach { starter -> Chip(starter, colors) { onPick(starter) } }
        }
    }
}

@Composable
private fun Bubble(
    text: String,
    mine: Boolean,
    colors: VaiinillaColors,
    muted: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!mine) {
            Box(
                Modifier.padding(end = 8.dp).size(28.dp).clip(RoundedCornerShape(9.dp)).background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = colors.accentInk, modifier = Modifier.size(15.dp))
            }
        }
        Text(
            text,
            fontSize = 14.5.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color =
                when {
                    mine -> colors.onInk
                    muted -> colors.muted
                    else -> colors.ink
                },
            modifier =
                Modifier
                    .widthIn(max = 290.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (mine) 20.dp else 6.dp,
                            bottomEnd = if (mine) 6.dp else 20.dp,
                        ),
                    ).background(if (mine) colors.ink else colors.paper2)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
        )
    }
}

/** Compra sin cuenta: el nombre con el que el mesero lo ubica, una sola vez, como al confirmar un pedido. */
@Composable
private fun NameCard(
    initial: String,
    entering: Boolean,
    error: String?,
    colors: VaiinillaColors,
    onSubmit: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    Column(
        Modifier
            .padding(start = 36.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.paper2)
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Text("¿Cómo te llamas?", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink)
        Text(
            "Con tu nombre te ubica el mesero. No se lo mandamos al asistente.",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.muted,
            modifier = Modifier.padding(top = 2.dp),
        )
        BasicTextField(
            value = name,
            onValueChange = { name = it.take(NAME_MAX) },
            singleLine = true,
            enabled = !entering,
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.ink),
            cursorBrush = SolidColor(colors.ink),
            keyboardOptions =
                KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit(name) }),
            modifier =
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.paper)
                    .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp)
                    .semantics { contentDescription = "Tu nombre" },
        )
        error?.let {
            Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.coral)
        }
        Text(
            "Al seguir aceptas los Términos y el Aviso de privacidad de Vaiinilla.",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp),
        )
        WideButton(
            if (entering) "Un momento…" else "Seguir",
            colors.accent,
            colors.accentInk,
            Modifier.padding(top = 10.dp).fillMaxWidth(),
        ) { if (!entering) onSubmit(name) }
    }
}

@Composable
private fun ProductRow(
    card: AssistantProductCard,
    added: Boolean,
    colors: VaiinillaColors,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        Modifier
            .padding(start = 36.dp)
            .fillMaxWidth()
            .alpha(if (card.available) 1f else 0.55f)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.paper2)
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClickLabel = "Ver detalle", onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(
            imageUrl = card.imageUrl.orEmpty(),
            contentDescription = card.name,
            card = true,
            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(card.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink, maxLines = 2)
            val detail =
                if (!card.available) {
                    "Agotado hoy"
                } else {
                    listOfNotNull(moneyLabel(card.price), card.minutes?.let { "$it min" }).joinToString(" · ")
                }
            Text(detail, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.muted)
        }
        if (card.available) {
            SmallButton(
                label =
                    when {
                        added -> "Agregado"
                        card.needsOptions -> "Elegir"
                        else -> "+ Agregar"
                    },
                colors = colors,
                enabled = !added,
                onClick = onAdd,
            )
        }
    }
}

/**
 * Una pista que mencionó el asistente, con datos del servidor: si está libre ahora (y hasta cuándo) o cuándo se
 * desocupa, precio por hora y cupo. Solo las que se rentan desde la app llevan «Rentar».
 */
@Composable
private fun CourtRow(
    court: AssistantCourtCard,
    colors: VaiinillaColors,
    onRent: () -> Unit,
) {
    val rentable = court.pricePerHour != null
    Row(
        Modifier
            .padding(start = 36.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.paper2)
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
            .clickable(enabled = rentable, role = Role.Button, onClickLabel = "Rentar", onClick = onRent)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (court.freeNow) colors.accent else colors.line),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                null,
                tint = if (court.freeNow) colors.accentInk else colors.ink2,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(court.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink, maxLines = 2)
            Text(
                courtAvailability(court),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (court.freeNow) colors.accent else colors.muted,
            )
            val detail =
                listOfNotNull(
                    court.pricePerHour?.let { "${moneyLabel(it)} por hora" },
                    court.cardPricePerHour
                        ?.takeIf { card ->
                            val cash = court.pricePerHour?.toBigDecimalOrNull()
                            val byCard = card.toBigDecimalOrNull()
                            cash == null || byCard == null || cash.compareTo(byCard) != 0
                        }
                        ?.let { "con tarjeta ${moneyLabel(it)}" },
                    court.capacity?.let { if (it == 1) "1 persona" else "Hasta $it personas" },
                ).joinToString(" · ")
                    .ifEmpty { if (rentable) "" else "Se renta en el lugar" }
            if (detail.isNotEmpty()) {
                Text(detail, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.muted)
            }
        }
        if (rentable) {
            SmallButton(label = "Rentar", colors = colors, enabled = true, onClick = onRent)
        }
    }
}

/** «Libre ahora», «Libre hasta las 18:00» u «Ocupada hasta las 19:30». */
internal fun courtAvailability(court: AssistantCourtCard): String =
    when {
        court.freeNow && court.freeUntil != null -> "Libre hasta las ${court.freeUntil}"
        court.freeNow -> "Libre ahora"
        court.busyUntil != null -> "Ocupada hasta las ${court.busyUntil}"
        else -> "Ocupada ahora"
    }

@Composable
private fun OrderCard(
    order: AssistantOrderProposal,
    place: String?,
    done: Boolean,
    superseded: Boolean,
    note: String?,
    colors: VaiinillaColors,
    onChange: () -> Unit,
    onAdd: () -> Unit,
    onOpenCart: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.paper2)
            .border(1.dp, colors.line, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Caps(place?.let { "Tu pedido · ${it.replaceFirstChar(Char::uppercase)}" } ?: "Tu pedido", colors.muted)
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            order.lines.forEach { line ->
                Row {
                    Text("${line.quantity}×", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = colors.muted, modifier = Modifier.width(30.dp))
                    Column(Modifier.weight(1f)) {
                        Text(line.product.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = colors.ink)
                        line.note?.let {
                            Text("Nota: $it", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                        }
                        if (line.product.needsOptions) {
                            Text("Elige sus opciones en el menú", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.muted)
                        }
                    }
                    val lineTotal =
                        line.product.price
                            .toBigDecimalOrNull()
                            ?.multiply(line.quantity.toBigDecimal())
                            ?.toPlainString() ?: line.product.price
                    Text(moneyLabel(lineTotal), fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = colors.ink)
                }
            }
        }
        if (order.unavailable.isNotEmpty()) {
            Text(
                "Ya no hay: ${order.unavailable.joinToString(", ")}",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.coral,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (order.toChoose.isNotEmpty()) {
            Text(
                "Elige sus opciones arriba: ${order.toChoose.joinToString(", ")}",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (order.lines.isNotEmpty()) {
            Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(colors.line))
            Row {
                Text("Total", fontSize = 17.sp, fontWeight = FontWeight.Black, color = colors.ink, modifier = Modifier.weight(1f))
                Text(moneyLabel(order.total), fontSize = 17.sp, fontWeight = FontWeight.Black, color = colors.ink)
            }
            Text(
                "Lo mandas a cocina desde tu carrito, con la forma de pago de siempre.",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            note?.let {
                Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.coral, modifier = Modifier.padding(top = 8.dp))
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (done) {
                    WideButton("Ver carrito", colors.ink, colors.onInk, Modifier.weight(1f), onOpenCart)
                } else if (superseded) {
                    Text(
                        "Ya hay una versión más nueva de tu pedido abajo.",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.muted,
                    )
                } else {
                    WideButton("Cambiar algo", colors.line, colors.ink, Modifier.weight(1f), onChange)
                    WideButton("Agregar al carrito", colors.accent, colors.accentInk, Modifier.weight(1.4f), onAdd)
                }
            }
        }
    }
}

@Composable
private fun WaiterCard(
    offer: AssistantWaiterOffer,
    done: Boolean,
    note: String?,
    colors: VaiinillaColors,
    onCall: () -> Unit,
) {
    Column(
        Modifier
            .padding(start = 36.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.paper2)
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(colors.ink), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Notifications, null, tint = colors.onInk, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Llamar al mesero", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink)
                Text(
                    "Motivo: ${offer.detail ?: offer.reason.label}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.muted,
                )
            }
        }
        note?.let {
            Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.ink2, modifier = Modifier.padding(top = 10.dp))
        }
        if (!done) {
            WideButton("Sí, llámalo", colors.ink, colors.onInk, Modifier.padding(top = 12.dp).fillMaxWidth(), onCall)
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    onDraft: (String) -> Unit,
    enabled: Boolean,
    colors: VaiinillaColors,
    onSend: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraft,
            enabled = enabled,
            textStyle = TextStyle(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(colors.ink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            maxLines = 4,
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(colors.paper2)
                    .border(1.dp, colors.line, RoundedCornerShape(25.dp))
                    .semantics { contentDescription = "Pregunta o pide lo que quieras" },
            decorationBox = { field ->
                Box(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) {
                        Text("Pregunta o pide lo que quieras…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.muted)
                    }
                    field()
                }
            },
        )
        val canSend = enabled && draft.isNotBlank()
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (canSend) colors.accent else colors.paper2)
                .clickable(enabled = canSend, role = Role.Button, onClick = onSend)
                .semantics { contentDescription = "Enviar" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.ArrowUpward, null, tint = if (canSend) colors.accentInk else colors.muted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun Caps(
    text: String,
    color: Color,
) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, color = color)
}

@Composable
private fun Chip(
    text: String,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.paper2)
            .border(1.dp, colors.line, RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = colors.ink2, maxLines = 1)
    }
}

@Composable
private fun SmallButton(
    label: String,
    colors: VaiinillaColors,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) colors.ink else colors.accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!enabled) Icon(Icons.Rounded.Check, null, tint = colors.accentInk, modifier = Modifier.size(15.dp).padding(end = 2.dp))
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = if (enabled) colors.onInk else colors.accentInk)
    }
}

@Composable
private fun WideButton(
    label: String,
    background: Color,
    content: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = content, maxLines = 1)
    }
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    colors: VaiinillaColors,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.paper2)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = colors.ink, modifier = Modifier.size(20.dp))
    }
}

private val STARTERS =
    listOf("¿Qué me recomiendas?", "Algo para compartir", "Sin alcohol", "¿Qué no tiene gluten?", "Lo más rápido")

private const val NAME_MAX = 60
