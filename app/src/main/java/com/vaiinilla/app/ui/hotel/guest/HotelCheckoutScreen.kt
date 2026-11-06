package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.canPayAtEnd
import com.vaiinilla.app.ui.order.canPayWithCard
import com.vaiinilla.app.ui.order.cartLineTotal
import com.vaiinilla.app.ui.order.checkoutSpace
import com.vaiinilla.app.ui.order.checkoutTotal
import com.vaiinilla.app.ui.order.hasUnresolvedStripePayment
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** Una línea del resumen del pedido a la habitación. */
data class HotelCheckoutLine(
    val quantity: Int,
    val name: String,
    val total: String,
)

enum class HotelPayChoice { ROOM, CARD }

/**
 * H03: el pedido a la habitación. Resumen, cuándo lo quieres, cómo pagas y una nota. Los precios son los del
 * negocio: nada se suma aquí. «Cuándo» solo ofrece «lo antes posible» porque el servidor no programa pedidos a un
 * cuarto; «cómo pagas» ofrece lo que el hotel encendió.
 */
@Composable
fun HotelCheckoutContent(
    roomName: String,
    lines: List<HotelCheckoutLine>,
    readyMinutes: Int?,
    roomChargeEnabled: Boolean,
    cardEnabled: Boolean,
    selected: HotelPayChoice?,
    onSelect: (HotelPayChoice) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    total: String,
    error: String?,
    creating: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    pendingPaymentNotice: String? = null,
    onResolvePendingPayment: () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    Box(modifier = modifier.fillMaxSize().background(colors.paper)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HotelPageTitle(eyebrow = "Pedido", title = "Entrega a tu cuarto")
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HotelPanel(title = "Resumen") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        lines.forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "${line.quantity} × ${line.name}",
                                    color = colors.ink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    line.total,
                                    color = colors.ink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    style = TextStyle(fontFeatureSettings = "tnum"),
                                )
                            }
                        }
                    }
                }
                HotelPanel(title = "Cuándo lo quieres") {
                    HotelRadioOption(
                        title = "Lo antes posible",
                        subtitle = readyMinutes?.let { "La cocina lo prepara en unos $it min y te lo lleva" } ?: "La cocina lo prepara y te lo lleva",
                        selected = true,
                        onClick = {},
                    )
                }
                HotelPanel(title = "Cómo pagas") {
                    if (roomChargeEnabled) {
                        HotelRadioOption(
                            title = "Cargar a la ${roomName.replaceFirstChar { it.lowercase() }}",
                            subtitle = "Se paga al salir",
                            selected = selected == HotelPayChoice.ROOM,
                            onClick = { onSelect(HotelPayChoice.ROOM) },
                            testTag = "hotel_pay_room",
                        )
                    }
                    if (cardEnabled) {
                        HotelRadioOption(
                            title = "Tarjeta ahora",
                            subtitle = "Pago seguro con Stripe",
                            selected = selected == HotelPayChoice.CARD,
                            onClick = { onSelect(HotelPayChoice.CARD) },
                            testTag = "hotel_pay_card",
                        )
                    }
                    if (!roomChargeEnabled && !cardEnabled) {
                        Text(
                            "Este hotel no recibe pedidos desde la app en este momento.",
                            color = colors.muted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                NoteField(notes, onNotesChange)
                pendingPaymentNotice?.let {
                    Text(
                        it,
                        color = colors.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.physicalPress(onClick = onResolvePendingPayment).padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
                error?.let {
                    Text(
                        it,
                        color = colors.coral,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp).testTag("hotel_checkout_error"),
                    )
                }
            }
            Box(Modifier.height(96.dp))
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(colors.paper.copy(alpha = 0f), colors.paper)))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
        ) {
            HotelPrimaryPill(
                label = "Pedir · $total",
                onClick = onConfirm,
                enabled = selected != null && lines.isNotEmpty(),
                loading = creating,
                testTag = "hotel_checkout_confirm",
            )
        }
    }
}

@Composable
private fun NoteField(
    value: String,
    onChange: (String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.paper2).padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        if (value.isEmpty()) {
            Text("Nota para la cocina (ej. sin cebolla)", color = colors.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().testTag("hotel_checkout_notes"),
            textStyle = TextStyle(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(colors.accent),
        )
    }
}

/** Opción con círculo de elegir, de las hojas «cuándo lo quieres» y «cómo pagas» de Doop. */
@Composable
internal fun HotelRadioOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .physicalPress(onClick = {
                    haptics.selection()
                    onClick()
                })
                .clip(shape)
                .background(if (selected) colors.accent.copy(alpha = 0.14f) else colors.paper2)
                .border(if (selected) 1.5.dp else 1.dp, if (selected) colors.accent else colors.line, shape)
                .semantics(mergeDescendants = true) {
                    role = Role.RadioButton
                    this.selected = selected
                }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(24.dp).border(2.dp, if (selected) colors.accent else colors.muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(12.dp).background(colors.accent, CircleShape))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * La ruta de H03 para el carrito de una habitación: arma [HotelCheckoutContent] con el estado del pedido y manda lo
 * elegido a los mismos puntos del flujo de siempre (pagar al final, tarjeta, confirmar). Si el servidor pide la
 * estancia, el código se escribe en una hoja y el mismo pedido se reintenta.
 */
@Composable
fun HotelCheckoutRoute(
    state: OrderFlowUiState,
    onNotesChange: (String) -> Unit,
    onPayAtEnd: () -> Unit,
    onPaymentChange: (PaymentMethod) -> Unit,
    onConfirm: () -> Unit,
    onCheckoutOpened: () -> Unit,
    onResolvePendingStripePayment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onCheckoutOpened() }
    val roomEnabled = state.canPayAtEnd
    val cardEnabled = state.canPayWithCard
    val selected =
        when {
            state.checkoutPayAtEnd && roomEnabled -> HotelPayChoice.ROOM
            state.checkoutPayment == PaymentMethod.STRIPE && cardEnabled -> HotelPayChoice.CARD
            roomEnabled -> HotelPayChoice.ROOM
            cardEnabled -> HotelPayChoice.CARD
            else -> null
        }
    HotelCheckoutContent(
        roomName = state.checkoutSpace?.name ?: Venue.roomTitle,
        lines = state.cartLines.map { HotelCheckoutLine(it.quantity, it.product.name, moneyLabel(state.cartLineTotal(it))) },
        readyMinutes = state.cartLines.maxOfOrNull { it.product.estimatedTimeMinutes }?.takeIf { it > 0 },
        roomChargeEnabled = roomEnabled,
        cardEnabled = cardEnabled,
        selected = selected,
        onSelect = { choice ->
            if (choice == HotelPayChoice.ROOM) onPayAtEnd() else onPaymentChange(PaymentMethod.STRIPE)
        },
        notes = state.kitchenNotes,
        onNotesChange = onNotesChange,
        total = moneyLabel(state.checkoutTotal),
        error = state.createOrderError,
        creating = state.creatingOrder,
        onConfirm = {
            when (selected) {
                HotelPayChoice.ROOM -> if (!state.checkoutPayAtEnd) onPayAtEnd()
                HotelPayChoice.CARD -> if (state.checkoutPayment != PaymentMethod.STRIPE) onPaymentChange(PaymentMethod.STRIPE)
                null -> return@HotelCheckoutContent
            }
            onConfirm()
        },
        modifier = modifier,
        pendingPaymentNotice = "Tienes un pago pendiente. Revísalo antes de pedir otra vez.".takeIf { state.hasUnresolvedStripePayment },
        onResolvePendingPayment = onResolvePendingStripePayment,
    )
    if (state.needsStayCode) {
        val flow = activityOrderFlow()
        HotelStayCodeSheet(
            linking = state.creatingOrder,
            error = state.stayCodeError,
            onSubmit = { code -> flow?.linkStayAndRetry(code) },
            onDismiss = { flow?.dismissStayCode() },
        )
    }
}