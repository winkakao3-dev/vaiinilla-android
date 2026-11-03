package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.qr.QrCodeEncoder
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.openTableCallSpaceId
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.WaiterCallHost
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.canAnnounceArrival
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.CaptionTone
import com.vaiinilla.app.ui.order.FactTone
import com.vaiinilla.app.ui.order.JourneyFact
import com.vaiinilla.app.ui.order.JourneyVenue
import com.vaiinilla.app.ui.order.MenuWaiterCallState
import com.vaiinilla.app.ui.order.OrderJourney
import com.vaiinilla.app.ui.order.cancellationMoneyNote
import com.vaiinilla.app.ui.order.journeyCaption
import com.vaiinilla.app.ui.order.journeyFacts
import com.vaiinilla.app.ui.order.journeyHeadline
import com.vaiinilla.app.ui.order.journeyKindLabel
import com.vaiinilla.app.ui.order.journeyLead
import com.vaiinilla.app.ui.order.journeyMoneyBack
import com.vaiinilla.app.ui.order.journeyNote
import com.vaiinilla.app.ui.order.journeyNoun
import com.vaiinilla.app.ui.order.journeyStepIndex
import com.vaiinilla.app.ui.order.journeySteps
import com.vaiinilla.app.ui.order.orderCancellationNotice
import com.vaiinilla.app.ui.order.orderJourney
import com.vaiinilla.app.ui.order.pickupCodeUseful
import com.vaiinilla.app.ui.order.shouldAutoRevealPickupCode
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import java.time.format.DateTimeFormatter
import kotlin.math.min

const val JOURNEY_SHOW_CODE_TAG = "journey-show-code"
const val JOURNEY_ARRIVED_TAG = "journey-arrived"
const val JOURNEY_WAITER_TAG = "journey-waiter"

// El código siempre va oscuro sobre crema, también en tema claro: un QR invertido no lo leen todos los lectores.
private val CodePaper = Color(0xFFF5F1E5)
private val CodeInk = Color(0xFF1D1E1C)
private val CodeMuted = Color(0xFF6C6B63)
private val ARRIVAL_HOUR = DateTimeFormatter.ofPattern("HH:mm")

private enum class ButtonLook { INK, OUTLINE, LIME }

private class JourneyAction(
    val text: String,
    val look: ButtonLook,
    val icon: ImageVector?,
    val tag: String? = null,
    val onClick: () -> Unit,
)

/**
 * El seguimiento de un pedido abierto desde Pedidos, como los dibujos P1 a P13 de Doop: arriba en qué va y qué
 * sigue, la barra de pasos, la tarjeta con foto, lo que pediste y lo que pagaste, un aviso y los botones de ese tipo.
 */
@Composable
internal fun OrderJourneyPage(
    order: OrderDetail,
    qrPayload: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    deliveryRequiresQr: Boolean = true,
    venue: JourneyVenue = JourneyVenue(),
    onAnnounceArrival: (() -> Unit)? = null,
    onCurrentWaiterCall: (suspend (Int) -> Result<TableCall?>)? = null,
    onCallWaiter: (suspend (Int, CallReason, String?) -> Result<TableCall>)? = null,
    onCancelWaiter: (suspend (TableCall) -> Result<TableCall>)? = null,
    onOpenMenu: (() -> Unit)? = null,
    guestNotice: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val uri = LocalUriHandler.current
    val journey = orderJourney(order, venue)
    val state = order.summary.state
    val key = order.summary.id
    val matrix = remember(qrPayload) { qrPayload?.let { runCatching { QrCodeEncoder.encode(it) }.getOrNull() } }
    val codeUseful = pickupCodeUseful(order, deliveryRequiresQr)
    val autoCode = shouldAutoRevealPickupCode(order, codeUseful)
    var codeOpen by rememberSaveable(key) { mutableStateOf(false) }
    // El folio en grande se ve solo cuando hay que enseñarlo; el QR, cuando este teléfono lo tiene.
    val showCode = codeUseful && (autoCode || codeOpen)
    BackHandler(enabled = codeOpen && !autoCode) { codeOpen = false }
    ScreenBrightnessBoost(active = showCode && matrix != null)

    // Llega un nuevo estado mientras se mira: listo vibra como éxito, lo demás como un toque.
    var seenState by remember(key) { mutableStateOf(state) }
    LaunchedEffect(state) {
        if (state != seenState) {
            if (state == OrderState.READY || state == OrderState.DELIVERED) haptics.success() else haptics.selection()
            seenState = state
        }
    }

    val spaceId = openTableCallSpaceId(order)
    val waiter =
        if (spaceId != null && onCurrentWaiterCall != null && onCallWaiter != null && onCancelWaiter != null) {
            remember(spaceId) { MenuWaiterCallState(spaceId, onCurrentWaiterCall, onCallWaiter, onCancelWaiter, null) }
        } else {
            null
        }
    val arrival = onAnnounceArrival.takeIf { order.canAnnounceArrival(venue.driveThru) }
    val actions =
        buildList {
            val open = state !in setOf(OrderState.CANCELED, OrderState.NOT_PICKED_UP, OrderState.EXPIRED)
            if (codeUseful && !autoCode && !codeOpen && matrix != null) {
                add(
                    JourneyAction("Mostrar código", ButtonLook.OUTLINE, Icons.Outlined.QrCode2, JOURNEY_SHOW_CODE_TAG) {
                        codeOpen = true
                    },
                )
            }
            if (waiter != null && open) {
                val label =
                    if (journey == OrderJourney.SEAT || journey == OrderJourney.COURT_FOOD) {
                        "Llamar al personal"
                    } else {
                        "Llamar al mesero"
                    }
                add(JourneyAction(label, ButtonLook.INK, Icons.Outlined.NotificationsNone, JOURNEY_WAITER_TAG, waiter::tap))
            }
            venue.whatsappUrl?.let { url ->
                val lone = state == OrderState.NOT_PICKED_UP
                add(
                    JourneyAction(
                        if (lone) "Escribir por WhatsApp" else "WhatsApp",
                        if (lone) ButtonLook.INK else ButtonLook.OUTLINE,
                        Icons.Outlined.ChatBubbleOutline,
                    ) { runCatching { uri.openUri(url) } },
                )
            }
            if (!open && onOpenMenu != null) {
                add(
                    JourneyAction(
                        if (state == OrderState.CANCELED) "Pedir otra cosa" else "Pedir de nuevo",
                        if (state == OrderState.CANCELED) ButtonLook.INK else ButtonLook.OUTLINE,
                        null,
                        onClick = onOpenMenu,
                    ),
                )
            }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.paper)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
    ) {
        JourneyTopRow(
            title = "${journeyNoun(journey)} #${order.summary.folio} · ${journeyKindLabel(journey)}",
            venueName = venue.name,
            onBack = {
                haptics.click()
                if (codeOpen && !autoCode) codeOpen = false else onBack()
            },
            colors = colors,
        )
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp)) {
            JourneyHeader(order, journey, colors)
            if (state == OrderState.CANCELED) {
                orderCancellationNotice(order)?.let { notice ->
                    notice.reason?.let { CancelQuote(it, notice.title, colors, Modifier.arrive(3, key)) }
                }
            }
            val steps = journeySteps(order, journey)
            if (steps.isNotEmpty()) {
                JourneySteps(steps, journeyStepIndex(order, journey), colors, Modifier.padding(top = 16.dp).arrive(3, key))
            }
        }
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).animateContentSize(spring(0.9f, 380f)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            journeyMoneyBack(order)?.let { title ->
                MoneyBackCard(title, cancellationMoneyNote(order), colors, Modifier.arrive(4, key))
            }
            AnimatedVisibility(
                visible = showCode,
                enter = fadeIn(spring(stiffness = 300f)) + scaleIn(spring(0.62f, 320f), initialScale = 0.85f),
                exit = fadeOut(spring(stiffness = 400f)) + scaleOut(spring(stiffness = 400f), targetScale = 0.92f),
            ) {
                FolioCodeCard(order, matrix, colors)
            }
            OrderCard(order, journey, showCode, colors, Modifier.arrive(4, key))
            // La tarjeta del dinero ya dice a dónde regresó; la nota no lo repite.
            journeyNote(order, journey)
                ?.takeUnless { journeyMoneyBack(order) != null && it == cancellationMoneyNote(order) }
                ?.let { DashedNote(it, colors, Modifier.arrive(5, key)) }
            guestNotice?.let { DashedNote(it, colors, Modifier.arrive(5, key)) }
            if (arrival != null) {
                ArrivedButton(arrival, colors, Modifier.arrive(5, key))
            } else if (journey == OrderJourney.DRIVE_THRU && order.arrivedAt != null && state != OrderState.DELIVERED) {
                Text(
                    "Avisaste que llegaste a las ${BusinessTime.format(order.arrivedAt, ARRIVAL_HOUR)}.",
                    color = colors.ink2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.accent.copy(alpha = 0.16f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
            JourneyActions(actions, colors, Modifier.arrive(6, key))
        }
    }
    waiter?.let { WaiterCallHost(state = it, place = order.summary.space?.name) }
}

@Composable
private fun JourneyTopRow(
    title: String,
    venueName: String?,
    onBack: () -> Unit,
    colors: VaiinillaColors,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp).arrive(0),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onBack)
                    .clip(CircleShape)
                    .background(colors.paper2)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Regresar"
                    },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            if (venueName.isNullOrBlank()) {
                Text(title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(title, color = colors.muted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(venueName, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun VaiinillaColors.tone(tone: CaptionTone): Color =
    when (tone) {
        CaptionTone.WAITING -> yolk
        CaptionTone.DONE -> accent
        CaptionTone.PROBLEM -> coral
    }

/** Etiqueta, título y qué sigue. Cambian con un resorte cuando el pedido avanza. */
@Composable
private fun JourneyHeader(
    order: OrderDetail,
    journey: OrderJourney,
    colors: VaiinillaColors,
) {
    val key = order.summary.id
    val caption = journeyCaption(order, journey)
    val captionColor by animateColorAsState(colors.tone(caption.tone), spring(stiffness = 200f), label = "journey_caption")
    val swap = {
        (fadeIn(spring(stiffness = 220f)) + slideInVertically(spring(0.82f, 260f)) { it / 3 }) togetherWith
            (fadeOut(spring(stiffness = 400f)) + slideOutVertically(spring(stiffness = 400f)) { -it / 4 })
    }
    AnimatedContent(caption.text, transitionSpec = { swap() }, label = "journey_cap", modifier = Modifier.arrive(1, key)) {
        Text(it.uppercase(), color = captionColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.2.sp)
    }
    AnimatedContent(
        journeyHeadline(order, journey),
        transitionSpec = { swap() },
        label = "journey_title",
        modifier = Modifier.padding(top = 8.dp).arrive(2, key),
    ) {
        Text(it, color = colors.ink, fontSize = 29.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.2).sp)
    }
    journeyLead(order, journey)?.let { lead ->
        AnimatedContent(lead, transitionSpec = { swap() }, label = "journey_lead", modifier = Modifier.padding(top = 10.dp).arrive(3, key)) {
            Text(it, color = colors.ink2, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** La barra de pasos: lima lo que ya pasó, yema el paso actual (late), línea lo que falta. */
@Composable
private fun JourneySteps(
    steps: List<String>,
    current: Int,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = reducedMotion()
    val pulse =
        if (reduceMotion) {
            1f
        } else {
            val transition = rememberInfiniteTransition(label = "journey_now")
            transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
                label = "journey_now_alpha",
            ).value
        }
    Column(modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            steps.indices.forEach { index ->
                val now = index == current
                val target =
                    when {
                        index < current -> colors.accent
                        now -> colors.yolk
                        else -> colors.line
                    }
                val color by animateColorAsState(target, spring(stiffness = 160f), label = "journey_step_$index")
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .graphicsLayer { alpha = if (now) pulse else 1f }
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            steps.forEachIndexed { index, label ->
                val target =
                    when {
                        index < current -> colors.ink2
                        index == current -> colors.yolk
                        else -> colors.muted
                    }
                val color by animateColorAsState(target, spring(stiffness = 160f), label = "journey_label_$index")
                Text(label, color = color, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
    }
}

/** El folio en grande y, si este teléfono lo tiene, el QR (P2 y P10). */
@Composable
private fun FolioCodeCard(
    order: OrderDetail,
    matrix: com.google.zxing.common.BitMatrix?,
    colors: VaiinillaColors,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(CodePaper)
                .padding(18.dp)
                .semantics(mergeDescendants = true) { contentDescription = "Código del pedido ${order.summary.folio}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (matrix != null) {
            Canvas(Modifier.size(150.dp)) {
                val module = min(size.width / matrix.width, size.height / matrix.height)
                val left = (size.width - module * matrix.width) / 2f
                val top = (size.height - module * matrix.height) / 2f
                for (x in 0 until matrix.width) {
                    for (y in 0 until matrix.height) {
                        if (matrix[x, y]) {
                            drawRect(CodeInk, Offset(left + x * module, top + y * module), Size(module, module))
                        }
                    }
                }
            }
        }
        Column {
            Text("FOLIO", color = CodeMuted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp)
            Text(
                "${order.summary.folio}",
                color = CodeInk,
                fontSize = 56.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2.5).sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                if (matrix != null) "Subimos el brillo para que lo lean" else "Dilo para que te lo entreguen",
                color = Color(0xFF4A4A44),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

private fun OrderItem.line(): String = "${quantity} $productName"

/** La foto, lo que pediste y las filas de dónde y cuánto. Con un producto rechazado se ve producto por producto (P12). */
@Composable
private fun OrderCard(
    order: OrderDetail,
    journey: OrderJourney,
    compact: Boolean,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val live = order.items.filter { it.rejection == null }
    val anyRejected = order.items.any { it.rejection != null }
    val facts = journeyFacts(order, journey)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.paper2),
    ) {
        if (anyRejected) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                order.items.forEachIndexed { index, item ->
                    if (index > 0) Divider(colors)
                    ItemRow(item, colors)
                }
            }
        } else {
            if (!compact) {
                CardPhoto(order, journey, live, colors)
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Text(
                    live.joinToString(" · ") { it.line() }.ifBlank { "Tu pedido" },
                    color = colors.ink,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val details =
                    live.flatMap { item ->
                        item.options.map { it.name } + listOfNotNull(item.notes.trim().takeIf { it.isNotEmpty() }?.let { "«$it»" })
                    }
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString(" · "),
                        color = colors.ink2,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
        if (facts.isNotEmpty()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) {
                facts.forEach { fact -> FactRow(fact, colors) }
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CardPhoto(
    order: OrderDetail,
    journey: OrderJourney,
    live: List<OrderItem>,
    colors: VaiinillaColors,
) {
    val imageUrl = live.firstNotNullOfOrNull { it.imageUrl?.takeIf(String::isNotBlank) }
    val venuePhoto = LocalVenueArt.current.photo
    // Sin foto del producto ni del negocio, la tarjeta empieza con lo que pediste: no se deja un bloque vacío.
    if (imageUrl == null && venuePhoto == null) return
    val reduceMotion = reducedMotion()
    val drift = remember(order.summary.id) { Animatable(1f) }
    LaunchedEffect(order.summary.id, reduceMotion) {
        if (!reduceMotion) drift.animateTo(1.07f, tween(9_000, easing = LinearEasing))
    }
    val label =
        order.summary.space?.name?.takeIf { it.isNotBlank() }
            ?: when {
                journey == OrderJourney.PREORDER_PICKUP || journey == OrderJourney.HOME_DELIVERY -> "Tu encargo"
                live.sumOf { it.quantity } > 1 -> "${live.sumOf { it.quantity }} productos"
                else -> "Tu pedido"
            }
    Box(Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
        val zoom = Modifier.fillMaxSize().graphicsLayer {
            scaleX = drift.value
            scaleY = drift.value
        }
        if (imageUrl != null) {
            ProductImage(imageUrl = imageUrl, contentDescription = "", modifier = zoom, contentScale = ContentScale.Crop)
        } else if (venuePhoto != null) {
            Image(bitmap = venuePhoto, contentDescription = null, contentScale = ContentScale.Crop, modifier = zoom)
        }
        Text(
            label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 10.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun ItemRow(
    item: OrderItem,
    colors: VaiinillaColors,
) {
    val off = item.rejection != null
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        val url = item.imageUrl?.takeIf(String::isNotBlank)
        if (url != null) {
            ProductImage(
                imageUrl = url,
                contentDescription = "",
                card = true,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).graphicsLayer { alpha = if (off) 0.5f else 1f },
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                item.line(),
                color = if (off) colors.muted else colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                textDecoration = if (off) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.rejection?.reason?.let { "$it · no se te cobra" } ?: "Se está preparando",
                color = if (off) colors.coral else colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(moneyLabel(item.subtotal), color = if (off) colors.muted else colors.ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun Divider(colors: VaiinillaColors) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ink.copy(alpha = 0.12f)))
}

@Composable
private fun FactRow(
    fact: JourneyFact,
    colors: VaiinillaColors,
) {
    Divider(colors)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(fact.label, color = colors.muted, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(
            fact.value,
            color =
                when (fact.tone) {
                    FactTone.PLAIN -> colors.ink
                    FactTone.GOOD -> colors.accent
                    FactTone.DUE -> colors.yolk
                },
            fontSize = 14.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun DashedNote(
    text: String,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val edge = colors.ink.copy(alpha = 0.22f)
    Text(
        text,
        color = colors.ink2,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = edge,
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        style =
                            Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                            ),
                    )
                }.padding(horizontal = 14.dp, vertical = 11.dp),
    )
}

/** La cita del negocio al cancelar (P11). */
@Composable
private fun CancelQuote(
    reason: String,
    who: String,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp))
            .background(colors.paper2),
    ) {
        Box(Modifier.width(3.dp).height(68.dp).background(colors.coral))
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text("«$reason»", color = colors.ink, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
            Text(who, color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** A dónde regresa el dinero (P11). */
@Composable
private fun MoneyBackCard(
    title: String,
    detail: String?,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.accent.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Undo, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.ink, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp)
            detail?.let { Text(it, color = colors.ink2, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** «Ya llegué» del drive-thru (P3): lima, con un pulso que invita a tocarlo. */
@Composable
private fun ArrivedButton(
    onAnnounce: () -> Unit,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVaiinillaHaptics()
    val reduceMotion = reducedMotion()
    val ring =
        if (reduceMotion) {
            0f
        } else {
            rememberInfiniteTransition(label = "arrived_ring").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2_200, easing = LinearEasing), RepeatMode.Restart),
                label = "arrived_ring_t",
            ).value
        }
    val accent = colors.accent
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .drawBehind {
                    if (ring > 0f && ring < 0.7f) {
                        val t = ring / 0.7f
                        val spread = 14.dp.toPx() * t
                        drawRoundRect(
                            color = accent.copy(alpha = 0.5f * (1f - t)),
                            topLeft = Offset(-spread, -spread),
                            size = Size(size.width + spread * 2, size.height + spread * 2),
                            cornerRadius = CornerRadius(size.height / 2 + spread),
                        )
                    }
                }.physicalPress(scale = PhysicalPressScale.Small) {
                    haptics.impact()
                    onAnnounce()
                }.clip(RoundedCornerShape(percent = 50))
                .background(accent)
                .semantics { role = Role.Button }
                .testTag(JOURNEY_ARRIVED_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(20.dp))
            Text("Ya llegué", color = colors.accentInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Los botones de abajo: uno a lo ancho, dos lado a lado, tres con el primero a lo ancho. */
@Composable
private fun JourneyActions(
    actions: List<JourneyAction>,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val lead = if (actions.size % 2 == 1) actions.first() else null
        lead?.let { JourneyButton(it, colors, Modifier.fillMaxWidth()) }
        actions.drop(if (lead != null) 1 else 0).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { JourneyButton(it, colors, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun JourneyButton(
    action: JourneyAction,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVaiinillaHaptics()
    val (background, content) =
        when (action.look) {
            ButtonLook.INK -> colors.ink to colors.onInk
            ButtonLook.OUTLINE -> Color.Transparent to colors.ink
            ButtonLook.LIME -> colors.accent to colors.accentInk
        }
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier =
            modifier
                .height(52.dp)
                .physicalPress(scale = PhysicalPressScale.Small) {
                    haptics.click()
                    action.onClick()
                }.clip(shape)
                .background(background)
                .then(if (action.look == ButtonLook.OUTLINE) Modifier.border(1.5.dp, colors.ink.copy(alpha = 0.12f), shape) else Modifier)
                .semantics { role = Role.Button }
                .then(action.tag?.let { Modifier.testTag(it) } ?: Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            action.icon?.let { Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(18.dp)) }
            Text(action.text, color = content, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}