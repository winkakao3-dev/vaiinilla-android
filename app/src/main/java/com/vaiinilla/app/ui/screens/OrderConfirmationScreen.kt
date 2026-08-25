package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.OrderUser
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.isStripePaymentConfirmedByBackend
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.order.StripePaymentPhase
import com.vaiinilla.app.ui.theme.VaiinillaTheme

private val TicketBg = Color(0xFF0D0D0D)
private val TicketInk = Color(0xFFF3EFE4)
private val TicketMuted = Color(0xFFB8B3AA)
private val TicketMuted2 = Color(0xFF8C8981)
private val TicketPaper = Color(0xFFF3EFE4)
private val TicketLime = Color(0xFFB7D464)
private val TicketLimeInk = Color(0xFF172008)
private val TicketLine = Color(0xB8F3EFE4)
private val TicketLineSoft = Color(0x2EF3EFE4)
private val TicketChipFill = Color(0x14B7D464)
private val TicketChipStroke = Color(0x3DB7D464)

internal fun confirmationTicketTitle(order: OrderDetail): String {
    val names = order.items.map { it.productName }
    return when {
        names.isEmpty() -> "Pedido"
        names.size == 1 -> names.first()
        else -> "${names.size} productos"
    }
}

internal fun confirmationTicketQrPayload(order: OrderDetail): String? = order.pickupToken?.takeIf { it.isNotBlank() }

internal fun confirmationCashPending(order: OrderDetail): Boolean =
    order.summary.paymentMethod == PaymentMethod.CASH &&
        order.summary.state == OrderState.PENDING_PAYMENT

@Composable
fun OrderConfirmationScreen(
    order: OrderDetail?,
    onReturnToMenu: () -> Unit,
    onViewTracking: () -> Unit = {},
    onViewSticker: () -> Unit = {},
    stripePaymentPhase: StripePaymentPhase = StripePaymentPhase.IDLE,
    stripePaymentMessage: String? = null,
    retryingStripePayment: Boolean = false,
    onRetryStripePayment: () -> Unit = {},
    onRefreshStripePayment: () -> Unit = {},
    screenshotPrinted: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(TicketBg),
    ) {
        if (order == null) return@Box
        val cashPending = confirmationCashPending(order)
        val stripeOrder = order.summary.paymentMethod == PaymentMethod.STRIPE
        val stripeConfirmed = stripeOrder && order.isStripePaymentConfirmedByBackend()
        val stripeAwaitingConfirmation = stripeOrder && !stripeConfirmed
        val stripeStatusScreen =
            stripeOrder &&
                stripePaymentPhase in
                setOf(
                    StripePaymentPhase.PROCESSING_CONFIRMATION,
                    StripePaymentPhase.PENDING,
                    StripePaymentPhase.TIMED_OUT,
                    StripePaymentPhase.FAILED,
                    StripePaymentPhase.CANCELED,
                )
        if (stripeStatusScreen) {
            StripePaymentPendingScreen(
                order = order,
                phase = stripePaymentPhase,
                message = stripePaymentMessage,
                retrying = retryingStripePayment,
                onRetry = onRetryStripePayment,
                onRefresh = onRefreshStripePayment,
                onViewOrders = onViewTracking,
            )
            return@Box
        }
        val qrPayload = confirmationTicketQrPayload(order)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .border(1.dp, TicketLineSoft, CircleShape)
                            .physicalPress(onClick = onReturnToMenu),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = TicketInk,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text =
                        when {
                            stripeAwaitingConfirmation -> order.payment?.status?.label ?: "Pago pendiente"
                            cashPending -> "Por cobrar"
                            else -> order.summary.state.label
                        },
                    color = TicketLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    modifier =
                        Modifier
                            .background(TicketChipFill, RoundedCornerShape(999.dp))
                            .border(1.dp, TicketChipStroke, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (cashPending || stripeAwaitingConfirmation) "Pago pendiente" else "Pedido confirmado",
                color = TicketMuted2,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = confirmationTicketTitle(order),
                color = TicketInk,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 34.sp,
                letterSpacing = (-1.4).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text =
                    when {
                        stripeAwaitingConfirmation -> stripePaymentMessage ?: "Esperando confirmación segura del pago."
                        cashPending -> "Muéstralo en Caja para pagar."
                        else -> "Tu pedido ya está en marcha."
                    },
                color = TicketMuted,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .background(TicketPaper, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (qrPayload != null) {
                        val qr = (minOf(maxWidth, maxHeight) - 28.dp).coerceAtLeast(72.dp)
                        VaiinillaQrCode(value = qrPayload, qrSize = qr)
                    } else {
                        Text(
                            text = "Código de recogida no disponible",
                            color = TicketBg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
            }
            TicketMetaGrid(order)
            Spacer(Modifier.height(10.dp))
            val visibleItems = order.items.take(3)
            val hiddenCount = order.items.size - visibleItems.size
            visibleItems.forEach { item ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${item.quantity} × ${item.productName}",
                        color = TicketInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        moneyLabel(item.subtotal),
                        color = TicketInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            if (hiddenCount > 0) {
                Text(
                    "+$hiddenCount más",
                    color = TicketMuted2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (stripeOrder && stripePaymentMessage != null) {
                Text(
                    text = stripePaymentMessage,
                    color = TicketMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            val canRetryStripe =
                stripeOrder &&
                    stripePaymentPhase in
                    setOf(
                        StripePaymentPhase.FAILED,
                        StripePaymentPhase.CANCELED,
                    )
            if (canRetryStripe) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .physicalPress(onClick = onRetryStripePayment),
                    color = TicketPaper,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (retryingStripePayment) "Preparando pago…" else "Reintentar pago",
                            color = TicketBg,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .physicalPress(onClick = onViewTracking),
                color = TicketLime,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 8.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Seguir pedido",
                        color = TicketLimeInk,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = (-0.4).sp,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .border(1.5.dp, TicketLineSoft, RoundedCornerShape(16.dp))
                        .physicalPress(onClick = onReturnToMenu),
                color = Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Volver al menú",
                        color = TicketInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketMetaGrid(order: OrderDetail) {
    val cells =
        listOf(
            order.summary.operationalDate to "Fecha",
            "#${order.summary.folio}" to "Pedido",
            moneyLabel(order.summary.total) to "Total",
            order.summary.paymentMethod.label to "Pago",
            order.summary.destination.label to "Destino",
            order.summary.state.label to "Estado",
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(2.dp, TicketLine),
    ) {
        cells.chunked(3).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(TicketLine),
                )
            }
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { colIndex, (value, label) ->
                    if (colIndex > 0) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(64.dp)
                                .background(TicketLine),
                        )
                    }
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(64.dp)
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            value.uppercase(),
                            color = TicketInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.4).sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            label.uppercase(),
                            color = TicketMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrderConfirmationPreview() {
    VaiinillaTheme {
        OrderConfirmationScreen(
            order = confirmationPreviewOrder,
            onReturnToMenu = {},
            onViewTracking = {},
        )
    }
}

private val confirmationPreviewOrder =
    OrderDetail(
        summary =
            OrderSummary(
                id = "preview-confirm",
                folio = 1042,
                operationalDate = "2026-08-13",
                state = OrderState.PENDING_PAYMENT,
                paymentMethod = PaymentMethod.CASH,
                destination = OrderDestination.TAKE_AWAY,
                space = null,
                subtotal = "101.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "101.00",
                version = 1,
                createdAt = "2026-08-13T13:40:00Z",
                updatedAt = "2026-08-13T13:40:00Z",
            ),
        user = OrderUser(name = "David", enrollment = "debug"),
        kitchenNotes = "",
        items =
            listOf(
                OrderItem(
                    id = 1,
                    productId = 2,
                    productName = "Burrito norteño",
                    preparationStation = PreparationStation.KITCHEN,
                    quantity = 1,
                    unitDigitalPrice = "76.00",
                    subtotal = "76.00",
                    options = emptyList(),
                ),
                OrderItem(
                    id = 2,
                    productId = 3,
                    productName = "Agua de jamaica",
                    preparationStation = PreparationStation.KITCHEN,
                    quantity = 1,
                    unitDigitalPrice = "25.00",
                    subtotal = "25.00",
                    options = emptyList(),
                ),
            ),
        pickupToken = "VN-1042-KX",
    )
