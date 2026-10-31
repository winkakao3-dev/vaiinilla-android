package com.vaiinilla.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.location.HomeAddressLocator
import com.vaiinilla.app.core.location.MapsLink
import com.vaiinilla.app.domain.model.DeliveryAddress
import com.vaiinilla.app.domain.model.HomeCheckoutRules
import com.vaiinilla.app.domain.model.HomePayment
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.asksForDeliveryDate
import com.vaiinilla.app.ui.order.cartTotalFor
import com.vaiinilla.app.ui.order.deliveryDates
import com.vaiinilla.app.ui.order.homeDeliveryOffer
import com.vaiinilla.app.ui.order.homePayments
import com.vaiinilla.app.ui.order.isHomeDelivery
import com.vaiinilla.app.ui.order.preorderOffer
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Lo que el carrito le pide a la pantalla para el pedido a domicilio y los encargos (CASA-05 a 08). */
data class HomeCheckoutActions(
    val onDestinationChange: (OrderDestination) -> Unit = {},
    val onEditAddress: ((DeliveryAddress) -> DeliveryAddress) -> Unit = {},
    val onQuoteStart: () -> Unit = {},
    val onQuoteFailed: (String) -> Unit = {},
    val onQuote: (latitude: Double, longitude: Double, written: String, street: String?) -> Unit = { _, _, _, _ -> },
    val onSelectDate: (String) -> Unit = {},
    val onSelectPayment: (HomePayment) -> Unit = {},
)

/** «Pasas por él» o «A domicilio»: solo sale si el negocio lleva a domicilio. */
@Composable
internal fun HomeDestinationPicker(
    selected: OrderDestination,
    onSelect: (OrderDestination) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(OrderDestination.TAKE_AWAY to "Pasas por él", OrderDestination.HOME_DELIVERY to "A domicilio").forEach { (destination, label) ->
            SelectPill(label, selected == destination, colors, Modifier.weight(1f)) {
                haptics.selection()
                onSelect(destination)
            }
        }
    }
}

/** El día del encargo; sale para llevar o a domicilio cuando el negocio toma encargos. */
@Composable
internal fun DeliveryDatePicker(
    state: OrderFlowUiState,
    onSelect: (String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val dates = state.deliveryDates
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CartSectionHead(title = "¿Para cuándo?", meta = state.preorderOffer?.takeIf { it.requiresAcceptance }?.let { "El negocio te confirma" })
        if (dates.isEmpty()) {
            Text("Por ahora no hay días disponibles para este pedido.", color = colors.muted, fontSize = 14.sp)
        } else {
            val today = LocalDate.now(BusinessTime.zone)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dates.forEach { date ->
                    SelectPill(HomeCheckoutRules.dayLabel(date, today), state.homeCheckout.deliveryDate == date, colors) {
                        haptics.selection()
                        onSelect(date)
                    }
                }
            }
        }
        if (state.preorderOffer?.requiresAcceptance == true) {
            Text(
                "El negocio revisa tu pedido y te confirma. Si no puede, te devuelve lo que pagaste.",
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

/**
 * A dónde se lleva, si llega y cuánto cuesta el envío, y cómo se paga. La dirección se busca en el teléfono (un
 * enlace de Maps o la calle) y el negocio responde si llega; el resultado entra con un fundido para que se note.
 */
@Composable
internal fun HomeDeliverySection(
    state: OrderFlowUiState,
    actions: HomeCheckoutActions,
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberVaiinillaHaptics()
    val offer = state.homeDeliveryOffer ?: return
    if (!state.isHomeDelivery) return
    val address = state.homeCheckout.address

    fun quote(find: suspend () -> MapsLink.Point?) {
        if (state.quotingDelivery) return
        val written = address.address
        actions.onQuoteStart()
        scope.launch {
            val point = runCatching { find() }.getOrNull()
            if (point == null) {
                actions.onQuoteFailed(
                    if (MapsLink.find(written) == null) {
                        "No encontramos esa dirección. Agrega la colonia y la ciudad."
                    } else {
                        "Ese enlace no trae el lugar. En Google Maps toca Compartir y pega el enlace otra vez."
                    },
                )
                return@launch
            }
            // Quien reparte necesita leer una calle, no un enlace.
            val street = if (MapsLink.find(written) == null) null else HomeAddressLocator.address(context, point)
            actions.onQuote(point.latitude, point.longitude, written, street)
        }
    }

    fun useCurrentLocation() {
        if (state.quotingDelivery) return
        actions.onQuoteStart()
        scope.launch {
            val point = HomeAddressLocator.currentLocation(context)
            if (point == null) {
                actions.onQuoteFailed("No pudimos saber dónde estás. Escribe tu dirección.")
                return@launch
            }
            val street =
                HomeAddressLocator.address(context, point)
                    ?: "Ubicación %.5f, %.5f".format(point.latitude, point.longitude)
            // La dirección cambia a la calle del punto: se cotiza contra ese texto.
            actions.onEditAddress { it.copy(address = street.take(160)) }
            actions.onQuote(point.latitude, point.longitude, street.take(160), null)
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) useCurrentLocation() else actions.onQuoteFailed("Sin permiso de ubicación. Escribe tu dirección.")
        }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.animateContentSize(spring(stiffness = 400f))) {
        CartSectionHead(
            title = "¿A dónde te lo llevan?",
            meta = offer.radiusKm.takeIf { it.isNotBlank() }?.let { "Hasta $it km" },
        )
        HomeField(
            value = address.address,
            onValueChange = { value -> actions.onEditAddress { it.copy(address = value.take(MapsLink.MAX_TEXT)) } },
            placeholder = "Calle, número y colonia, o un enlace de Maps",
            colors = colors,
            singleLine = false,
        )
        HomeField(
            value = address.reference,
            onValueChange = { value -> actions.onEditAddress { it.copy(reference = value.take(120)) } },
            placeholder = "Referencia, como el color del portón",
            colors = colors,
        )
        HomeField(
            value = address.phone,
            onValueChange = { value -> actions.onEditAddress { it.copy(phone = value.filter(Char::isDigit).take(10)) } },
            placeholder = "Teléfono para avisarte",
            colors = colors,
            keyboardType = KeyboardType.Phone,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectPill(
                if (state.quotingDelivery) "Calculando el envío…" else "Calcular envío",
                selected = false,
                colors = colors,
                modifier = Modifier.weight(1f),
                enabled = address.address.isNotBlank() && !state.quotingDelivery && state.homeCheckout.quote == null,
            ) {
                haptics.click()
                quote { HomeAddressLocator.locate(context, address.address) }
            }
            SelectPill("Usar mi ubicación", selected = false, colors = colors, modifier = Modifier.weight(1f), enabled = !state.quotingDelivery) {
                haptics.click()
                if (HomeAddressLocator.hasLocationPermission(context)) {
                    useCurrentLocation()
                } else {
                    permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }
        }
        AnimatedContent(
            targetState = Triple(state.homeCheckout.quote, state.deliveryQuoteError, state.quotingDelivery),
            transitionSpec = { fadeIn(spring(stiffness = 300f)) togetherWith fadeOut(spring(stiffness = 300f)) },
            label = "cotizacion-envio",
        ) { (quote, error, quoting) ->
            when {
                quoting -> Unit
                error != null -> Text(error, color = colors.coral, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                quote != null && quote.reaches -> {
                    val fee = quote.fee ?: offer.fee
                    val parts =
                        listOfNotNull(
                            "Sí llegan",
                            quote.distanceKm?.let { "$it km" },
                            if (Money.parse(fee).signum() == 0) "envío gratis" else "envío ${moneyLabel(fee)}",
                        )
                    Text(parts.joinToString(" · "), color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                quote != null ->
                    Text(
                        offer.radiusKm.takeIf { it.isNotBlank() }?.let {
                            "Esta dirección queda fuera de la zona de entrega (hasta $it km)."
                        } ?: "Esta dirección queda fuera de la zona de entrega.",
                        color = colors.coral,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                else -> Unit
            }
        }
        if (state.homePayments.isNotEmpty()) {
            CartSectionHead(title = "¿Cómo pagas?", meta = null)
            state.homePayments.forEach { payment ->
                SelectPill(
                    paymentLabel(payment, state),
                    selected = state.homeCheckout.payment == payment,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    haptics.selection()
                    actions.onSelectPayment(payment)
                }
            }
            if (state.homeCheckout.payment == HomePayment.ON_ARRIVAL) {
                offer.transferDetails?.let { details ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.paper2)
                                .padding(14.dp),
                    ) {
                        Text("Para pagar por transferencia", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(details, color = colors.ink, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

private fun paymentLabel(
    payment: HomePayment,
    state: OrderFlowUiState,
): String {
    val cardTotal = state.cartTotalFor(PaymentMethod.STRIPE)
    return when (payment) {
        HomePayment.CARD -> "Tarjeta ahora · ${moneyLabel(cardTotal)}"
        HomePayment.DEPOSIT -> {
            val percent = state.preorderOffer?.depositPercent ?: 0
            val now = Money.parse(cardTotal).multiply(percent.toBigDecimal()).divide(java.math.BigDecimal(100))
            "Anticipo de $percent % con tarjeta · ${moneyLabel(Money.format(now))} ahora y el resto al recibir"
        }
        HomePayment.ON_ARRIVAL ->
            if (state.homeDeliveryOffer?.transferDetails != null) {
                "Al recibir, en efectivo o por transferencia"
            } else {
                "Al recibir, en efectivo"
            }
    }
}

@Composable
private fun SelectPill(
    label: String,
    selected: Boolean,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(if (selected) colors.ink else colors.paper2)
                .border(1.dp, if (selected) colors.ink else colors.line, shape)
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (selected) colors.onInk else if (enabled) colors.ink else colors.muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HomeField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    colors: VaiinillaColors,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = colors.muted, fontSize = 14.sp) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.ink,
                unfocusedBorderColor = colors.line,
                focusedTextColor = colors.ink,
                unfocusedTextColor = colors.ink,
                cursorColor = colors.ink,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Lo del pedido a domicilio que ve el cliente (CASA-08): el código que dicta al recibir, a dónde va, el envío, el
 * anticipo y lo que paga en la puerta. El código entra con un resorte cuando el pedido sale.
 */
@Composable
internal fun HomeDeliveryOrderCard(order: com.vaiinilla.app.domain.model.OrderDetail) {
    val colors = LocalVaiinillaColors.current
    val home = order.homeDelivery ?: return
    val state = order.summary.state
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.paper2)
                .padding(18.dp)
                .animateContentSize(spring(stiffness = 400f)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            when (state) {
                com.vaiinilla.app.domain.model.OrderState.AWAITING_ACCEPTANCE -> "POR ACEPTAR"
                com.vaiinilla.app.domain.model.OrderState.ON_THE_WAY -> "EN CAMINO"
                else -> "A DOMICILIO"
            },
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
        )
        Text(
            when (state) {
                com.vaiinilla.app.domain.model.OrderState.AWAITING_ACCEPTANCE -> "El negocio revisa tu pedido y te confirma."
                com.vaiinilla.app.domain.model.OrderState.ON_THE_WAY ->
                    home.code?.let { "Va en camino. Al recibirlo, dicta el código $it." } ?: "Tu pedido va en camino."
                com.vaiinilla.app.domain.model.OrderState.READY -> "Está listo. Pronto sale para tu casa."
                com.vaiinilla.app.domain.model.OrderState.PAID, com.vaiinilla.app.domain.model.OrderState.PREPARING ->
                    "El negocio aceptó tu pedido."
                else -> state.label
            },
            color = colors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        home.code?.takeIf { state == com.vaiinilla.app.domain.model.OrderState.ON_THE_WAY }?.let { code ->
            Text("Código de entrega", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                code,
                color = colors.ink,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
            )
            Text("Díctalo a quien te entregue: así sabe que llegó contigo.", color = colors.muted, fontSize = 13.sp)
        }
        Text(
            listOf(home.address.address, home.address.reference).filter { it.isNotBlank() }.joinToString(" · "),
            color = colors.ink2,
            fontSize = 14.sp,
        )
        order.summary.deliveryDate?.let { date ->
            Text(
                HomeCheckoutRules.dayLabel(date, LocalDate.now(BusinessTime.zone)) +
                    (order.summary.deliverySlot?.let { " · $it" } ?: ""),
                color = colors.ink2,
                fontSize = 14.sp,
            )
        }
        val lines =
            listOfNotNull(
                home.fee?.let { fee -> if (Money.parse(fee).signum() == 0) "Envío gratis" else "Envío ${moneyLabel(fee)}" },
                home.deposit?.takeIf { Money.parse(it).signum() > 0 }?.let { "Anticipo pagado ${moneyLabel(it)}" },
                home.dueOnArrival?.takeIf { home.hasAmountDue }?.let { "Pagas al recibir ${moneyLabel(it)}" },
            )
        if (lines.isNotEmpty()) Text(lines.joinToString(" · "), color = colors.muted, fontSize = 13.sp)
    }
}
