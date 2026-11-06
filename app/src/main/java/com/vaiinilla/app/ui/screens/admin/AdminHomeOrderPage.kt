package com.vaiinilla.app.ui.screens.admin

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.domain.model.HomeCheckoutRules
import com.vaiinilla.app.domain.model.HomeOrderAction
import com.vaiinilla.app.domain.model.HomeOrderBoard
import com.vaiinilla.app.domain.model.HomeOrderStep
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.paymentWireLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.notices.NoticePanel
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.theme.Yolk
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private fun String?.positive(): String? = this?.takeIf { (it.toBigDecimalOrNull()?.signum() ?: 0) > 0 }

private val StepIcons =
    listOf(
        Icons.Outlined.HourglassTop,
        Icons.Outlined.Check,
        Icons.Outlined.RestaurantMenu,
        Icons.Outlined.DeliveryDining,
        Icons.Outlined.Verified,
    )

/** La etapa en los puntos; null en un pedido que terminó sin entregarse (sin puntos, R26). */
private fun stepIndex(state: OrderState): Int? =
    when (state) {
        OrderState.AWAITING_ACCEPTANCE -> 0
        OrderState.PENDING_PAYMENT, OrderState.PAID -> 1
        OrderState.PREPARING, OrderState.READY -> 2
        OrderState.ON_THE_WAY -> 3
        OrderState.DELIVERED -> 4
        OrderState.CANCELED, OrderState.EXPIRED, OrderState.NOT_PICKED_UP -> null
    }

private fun stateTitle(state: OrderState): String =
    when (state) {
        OrderState.AWAITING_ACCEPTANCE -> "Por aceptar"
        OrderState.PENDING_PAYMENT -> "Aceptado · falta su pago"
        OrderState.PAID -> "Aceptado"
        OrderState.PREPARING -> "Haciéndose"
        OrderState.READY -> "Listo para salir"
        OrderState.ON_THE_WAY -> "En camino"
        OrderState.DELIVERED -> "Entregado"
        OrderState.CANCELED -> "Cancelado"
        OrderState.EXPIRED -> "Expirado"
        OrderState.NOT_PICKED_UP -> "No recogido"
    }

/** «Sáb 7 nov · 10:00–12:00», o null si el pedido no trae día. */
private fun deliveryLine(order: OrderDetail): String? {
    val date = order.summary.deliveryDate ?: return null
    val day = HomeCheckoutRules.dayLabel(date, LocalDate.now(BusinessTime.zone))
    return listOfNotNull(day, order.summary.deliverySlot?.replace("-", "–")).joinToString(" · ")
}

private fun OrderDetail.customerLabel(): String =
    customerName?.takeIf { it.isNotBlank() } ?: user?.name?.takeIf { it.isNotBlank() } ?: "Cliente"

private fun OrderDetail.firstNameLabel(): String = customerLabel().substringBefore(' ')

private fun OrderDetail.noteLabel(): String? =
    (
        kitchenNotes.takeIf { it.isNotBlank() }
            ?: items.firstNotNullOfOrNull { item -> item.notes.takeIf { it.isNotBlank() } }
    )?.let { "Nota: «$it»" }

/**
 * Un pedido a domicilio o encargo a pantalla completa, como los marcos 18 y 19 del lienzo: la etapa en grande, a dónde
 * va, qué lleva y lo que toca hacer ahora. Reemplaza a la hoja solo para estos pedidos.
 */
@Composable
internal fun AdminHomeOrderPage(
    order: OrderDetail,
    state: AdminUiState,
    photos: Map<Int, String?>,
    colors: OperationalColors,
    busy: Boolean,
    error: String?,
    onDeliverAndCollect: (code: String, method: String?) -> Unit,
    onAction: (HomeOrderAction) -> Unit,
    onBack: () -> Unit,
) {
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BackHandler(onBack = onBack)
        AdminDialogHost(colors) {
            Box(Modifier.fillMaxSize().background(colors.background)) {
                val finished = order.summary.state == OrderState.DELIVERED && order.homeDelivery?.hasAmountDue != true
                if (finished) {
                    DeliveredAndPaid(order, state, colors, onBack)
                } else {
                    OrderInProgress(order, photos, colors, busy, error, onDeliverAndCollect, onAction, onBack)
                }
            }
        }
    }
}

@Composable
private fun OrderInProgress(
    order: OrderDetail,
    photos: Map<Int, String?>,
    colors: OperationalColors,
    busy: Boolean,
    error: String?,
    onDeliverAndCollect: (String, String?) -> Unit,
    onAction: (HomeOrderAction) -> Unit,
    onBack: () -> Unit,
) {
    val uri = LocalUriHandler.current
    val haptics = rememberVaiinillaHaptics()
    val dialog = LocalAdminDialog.current
    val home = order.homeDelivery
    val step = HomeOrderBoard.step(order)
    var code by remember(order.summary.id) { mutableStateOf("") }
    var method by remember(order.summary.id) { mutableStateOf("efectivo") }
    val due = home?.dueOnArrival.positive()
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "barra") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AdminRoundButton(Icons.AutoMirrored.Outlined.ArrowBack, "Regresar", colors, onBack)
                Spacer(Modifier.weight(1f))
                if (home?.address?.phone?.isNotBlank() == true) {
                    AdminPrimaryButton(
                        title = "Llamar",
                        colors = colors,
                        fullWidth = false,
                        icon = Icons.Outlined.Call,
                    ) {
                        haptics.click()
                        runCatching { uri.openUri("tel:${home.address.phone}") }
                    }
                }
            }
        }
        item(key = "titulo") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminCapsLabel(
                    listOfNotNull("#${order.summary.folio}", "A domicilio", deliveryLine(order)).joinToString(" · "),
                    colors,
                    inset = false,
                )
                Text(
                    stateTitle(order.summary.state),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = colors.textPrimary,
                )
            }
        }
        stepIndex(order.summary.state)?.let { step -> item(key = "etapas") { StepDots(step, colors) } }
        error?.let { message -> item(key = "error") { AdminSheetError(message) } }
        if (home != null) {
            item(key = "direccion") {
                AdminPanel(colors) {
                    AdminRow(
                        title = order.customerLabel(),
                        colors = colors,
                        detail =
                            listOf(
                                home.address.address,
                                home.address.reference,
                            ).filter { it.isNotBlank() }.joinToString("\n"),
                        onClick = {
                            haptics.click()
                            runCatching { uri.openUri(mapsUrl(home.address)) }
                        },
                        leading = { AdminIconBox(Icons.Outlined.LocationOn, colors) },
                    )
                }
            }
        }
        item(key = "productos") {
            AdminPanel(colors) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ThumbStack(order, photos, colors)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            order.items.joinToString(" · ") { it.productName },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                        )
                        order.noteLabel()?.let {
                            Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
                        }
                    }
                }
            }
        }
        if (step == HomeOrderStep.DELIVER) {
            item(key = "codigo") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Código de entrega",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                    )
                    Text(
                        "Pídeselo a ${order.firstNameLabel()} al entregar. Lo ve en su app.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                    CodeBoxes(code, colors, enabled = !busy) { code = it }
                }
            }
        }
        if (due != null && (step == HomeOrderStep.DELIVER || step == HomeOrderStep.COLLECT)) {
            item(key = "cobro") {
                AdminPanel(colors) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Por cobrar al entregar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            moneyLabelCents(due),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.textPrimary,
                        )
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminFilterChip("Efectivo", method == "efectivo", colors, onClick = { method = "efectivo" })
                        AdminFilterChip("Transferencia", method == "transferencia", colors, onClick = {
                            method =
                                "transferencia"
                        })
                    }
                }
            }
        }
        item(key = "accion") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val reject = {
                    dialog(
                        AdminDialogRequest(
                            title = "¿Rechazar el encargo?",
                            message = "A ${order.firstNameLabel()} se le devuelve lo que pagó y le llega el motivo.",
                            confirm = "Rechazar",
                            fieldPlaceholder = "Motivo",
                            accepts = { it.isNotBlank() },
                        ) {
                            onAction(HomeOrderAction.Reject(it))
                            true
                        },
                    )
                }
                when (step) {
                    HomeOrderStep.DECIDE ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminSecondaryButton(
                                "Rechazar",
                                colors,
                                Modifier.weight(1f),
                                outlined = true,
                                enabled = !busy,
                                onClick = reject,
                            )
                            AdminPrimaryButton(
                                "Aceptar",
                                colors,
                                Modifier.weight(1f),
                                icon = Icons.Outlined.Check,
                                busy = busy,
                            ) { onAction(HomeOrderAction.Accept) }
                        }
                    HomeOrderStep.SEND_OUT ->
                        AdminPrimaryButton(
                            "Salió a entregar",
                            colors,
                            busy = busy,
                        ) { onAction(HomeOrderAction.SendOut) }
                    HomeOrderStep.DELIVER ->
                        AdminPrimaryButton(
                            title = if (due != null) "Entregar y cobrar ${moneyLabelCents(due)}" else "Entregar",
                            colors = colors,
                            busy = busy,
                            enabled = code.length == 4,
                        ) { onDeliverAndCollect(code, method.takeIf { due != null }) }
                    HomeOrderStep.COLLECT ->
                        AdminPrimaryButton(
                            title = "Ya me pagó ${moneyLabelCents(due.orEmpty())}",
                            colors = colors,
                            busy = busy,
                        ) { onAction(HomeOrderAction.Collect(method)) }
                    null -> Unit
                }
                if (step == HomeOrderStep.SEND_OUT || step == HomeOrderStep.DELIVER) {
                    AdminSecondaryButton("Cancelar pedido", colors, destructive = true, enabled = !busy) {
                        dialog(
                            AdminDialogRequest(
                                title = "¿Cancelar el pedido?",
                                message = "A ${order.firstNameLabel()} le llega el motivo. Lo que ya pagó se le devuelve.",
                                confirm = "Cancelar pedido",
                                fieldPlaceholder = "Motivo",
                                accepts = { it.isNotBlank() },
                            ) {
                                onAction(HomeOrderAction.Cancel(it, order.summary.version))
                                true
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDots(
    current: Int,
    colors: OperationalColors,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StepIcons.forEachIndexed { index, icon ->
            val reached = index <= current
            Box(
                Modifier
                    .size(if (index == current) 34.dp else 30.dp)
                    .clip(CircleShape)
                    .background(if (reached) colors.accentLime else colors.cardBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (reached) colors.accentInk else colors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (index < StepIcons.lastIndex) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (index < current) colors.accentLime else colors.cardBorder),
                )
            }
        }
    }
}

@Composable
private fun ThumbStack(
    order: OrderDetail,
    photos: Map<Int, String?>,
    colors: OperationalColors,
) {
    val first = order.items.take(2)
    Row {
        first.forEachIndexed { index, item ->
            Box(Modifier.offset(x = if (index == 0) 0.dp else (-14).dp)) {
                AdminThumb(photos[item.productId], item.productName, colors, size = 46.dp)
            }
        }
    }
}

@Composable
private fun CodeBoxes(
    code: String,
    colors: OperationalColors,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    BasicTextField(
        value = code,
        onValueChange = { onChange(it.filter(Char::isDigit).take(4)) },
        enabled = enabled,
        // NumberPassword abría el gestor de contraseñas de Google encima del campo.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(color = Color.Transparent),
        modifier = Modifier.semantics { contentDescription = "Código de entrega" },
        decorationBox = { inner ->
            Box {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { index ->
                        val digit = code.getOrNull(index)?.toString().orEmpty()
                        val active = index == code.length
                        Box(
                            Modifier
                                .weight(1f)
                                .height(62.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBackground)
                                .border(
                                    1.5.dp,
                                    if (digit.isNotEmpty() || active) colors.accentLime else colors.cardBorder,
                                    RoundedCornerShape(16.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(digit, fontSize = 28.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
                        }
                    }
                }
                Box(Modifier.matchParentSize()) { inner() }
            }
        },
    )
}

/** Marco 19: entregado y sin nada por cobrar, con lo que se pagó y cómo van las cuentas del día. */
@Composable
private fun DeliveredAndPaid(
    order: OrderDetail,
    state: AdminUiState,
    colors: OperationalColors,
    onDone: () -> Unit,
) {
    val home = order.homeDelivery
    val deposit = home?.deposit.positive()
    val atDoor =
        order.summary.total
            .toBigDecimalOrNull()
            ?.minus(deposit?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO)
            ?.takeIf { it.signum() > 0 && deposit != null }
    // El método de la puerta, no el agregado del pedido: con anticipo y cobro en la puerta siempre es «mixto» (R39).
    val method = home?.collectedAtDoorMethod?.let(::paymentWireLabel)
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "palomita") {
            Column(
                Modifier.fillMaxWidth().padding(top = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(colors.accentLime),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Check, null, tint = colors.accentInk, modifier = Modifier.size(50.dp)) }
                Spacer(Modifier.height(10.dp))
                AdminCapsLabel("Pedido #${order.summary.folio} · Entregado", colors, inset = false)
                Text(
                    "Entregado\ny cobrado",
                    fontSize = 36.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    textAlign = TextAlign.Center,
                    color = colors.textPrimary,
                )
                Text(
                    "${order.firstNameLabel()} recibió ${order.items.joinToString(" y ") { it.productName }}." +
                        (atDoor?.let { " Ya cobraste los ${moneyLabelCents(it.toPlainString())} que faltaban." } ?: ""),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = colors.textSecondary,
                )
            }
        }
        item(key = "recibo") {
            AdminPanel(colors) {
                deposit?.let {
                    ReceiptRow(
                        Icons.Outlined.CreditCard,
                        "Anticipo con tarjeta",
                        moneyLabelCents(it),
                        colors,
                    )
                }
                atDoor?.let {
                    AdminRowDivider(colors)
                    ReceiptRow(
                        Icons.Outlined.Payments,
                        listOfNotNull("Al recibir", method).joinToString(" · "),
                        moneyLabelCents(it.toPlainString()),
                        colors,
                    )
                    AdminRowDivider(colors)
                }
                ReceiptRow(null, "Total", moneyLabelCents(order.summary.total), colors, strong = true)
            }
        }
        item(key = "cuentas") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminPanel(colors, Modifier.weight(1f)) {
                    Text("Ventas de hoy", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                    Text(
                        state.summary?.sales?.let(::moneyLabelCents) ?: "—",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AdminPanel(colors, Modifier.weight(1f)) {
                    Text("Me deben", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                    Text(
                        moneyLabelCents(HomeOrderBoard.total(state.owed)),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.accentLime,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item(key = "listo") { AdminPrimaryButton("Listo", colors, onClick = onDone) }
    }
}

@Composable
private fun ReceiptRow(
    icon: ImageVector?,
    label: String,
    value: String,
    colors: OperationalColors,
    strong: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        icon?.let {
            Icon(it, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
        }
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (strong) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            fontSize = if (strong) 17.sp else 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary,
        )
    }
}

/** Los montos de un encargo se leen con centavos, como en el lienzo. */
internal fun moneyLabelCents(amount: String): String {
    val value = amount.toBigDecimalOrNull() ?: return moneyLabel(amount)
    return "$" + String.format(java.util.Locale.US, "%,.2f", value)
}

/** La tarjeta de un encargo que espera respuesta (marco 17): qué es, para cuándo, cuánto y aceptar o rechazar ahí mismo. */
@Composable
internal fun AwaitingOrderCard(
    order: OrderDetail,
    photos: Map<Int, String?>,
    colors: OperationalColors,
    busy: Boolean,
    onAccept: () -> Unit,
    onReject: (String) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dialog = LocalAdminDialog.current
    val home = order.homeDelivery
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminPanel(
            colors,
            Modifier
                .border(1.dp, Yolk.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .physicalPress(onClick = onOpen),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AdminBadge("Por aceptar", colors, AdminBadgeTone.WARN)
                Spacer(Modifier.weight(1f))
                sinceLabel(order.summary.createdAt)?.let {
                    Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
                }
            }
            Row(
                Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ThumbStack(order, photos, colors)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "#${order.summary.folio} · ${order.customerLabel()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                    )
                    Text(
                        order.items.joinToString(" · ") { "${it.quantity} ${it.productName}" },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                deliveryLine(order)?.let { IconLabel(Icons.Outlined.CalendarMonth, it, colors) }
                if (home != null) IconLabel(Icons.Outlined.DeliveryDining, "A domicilio", colors)
            }
            Box(Modifier.padding(vertical = 10.dp)) { AdminRowDivider(colors) }
            home?.deposit.positive()?.let {
                MoneyLine("Anticipo pagado con tarjeta", moneyLabelCents(it), colors, tint = colors.accentLime)
            }
            home?.dueOnArrival.positive()?.let { MoneyLine("Por cobrar al entregar", moneyLabelCents(it), colors) }
            order.noteLabel()?.let {
                Text(
                    it,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminSecondaryButton("Rechazar", colors, Modifier.weight(1f), outlined = true, enabled = !busy) {
                    dialog(
                        AdminDialogRequest(
                            title = "¿Rechazar el encargo?",
                            message = "A ${order.firstNameLabel()} se le devuelve lo que pagó y le llega el motivo.",
                            confirm = "Rechazar",
                            fieldPlaceholder = "Motivo",
                            accepts = { it.isNotBlank() },
                        ) {
                            onReject(it)
                            true
                        },
                    )
                }
                AdminPrimaryButton(
                    "Aceptar",
                    colors,
                    Modifier.weight(1f),
                    icon = Icons.Outlined.Check,
                    busy = busy,
                    onClick = onAccept,
                )
            }
        }
        if (home?.deposit.positive() != null) {
            Row(
                Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Outlined.Info, null, tint = colors.textMuted, modifier = Modifier.size(14.dp))
                Text(
                    "Si lo rechazas, a ${order.firstNameLabel()} se le devuelve el anticipo.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun IconLabel(
    icon: ImageVector,
    text: String,
    colors: OperationalColors,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
    }
}

@Composable
private fun MoneyLine(
    label: String,
    value: String,
    colors: OperationalColors,
    tint: Color = colors.textPrimary,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(value, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = tint)
    }
}

/** La semana de los encargos por aceptar, con un punto en cada día que tiene entregas (marco 17). */
@Composable
internal fun WeekStrip(
    orders: List<OrderDetail>,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now(BusinessTime.zone)
    val dates =
        orders
            .mapNotNull {
                it.summary.deliveryDate
            }.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()
    val focus = dates.minOrNull() ?: today
    val monday = focus.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Esta semana", fontSize = 20.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeekLetters.forEachIndexed { index, name ->
                val day = monday.plusDays(index.toLong())
                val selected = day == focus
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) colors.textPrimary else colors.cardBackground)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (day in dates) Yolk else Color.Transparent),
                    )
                    Text(
                        name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) colors.background else colors.textMuted,
                    )
                    Text(
                        "${day.dayOfMonth}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (selected) colors.background else colors.textSecondary,
                    )
                }
            }
        }
    }
}

private val WeekLetters = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")

private fun sinceLabel(iso: String): String? =
    runCatching {
        val minutes =
            java.time.Duration
                .between(Instant.parse(iso), Instant.now())
                .toMinutes()
        when {
            minutes < 1 -> "ahora"
            minutes < 60 -> "hace $minutes min"
            minutes < 1440 -> "hace ${minutes / 60} h"
            minutes < 7 * 1440 -> "hace ${minutes / 1440} d"
            // Después de una semana, la fecha: «hace 340 d» no dice nada.
            else -> NoticePanel.shortDate(Instant.parse(iso).toEpochMilli())
        }
    }.getOrNull()
