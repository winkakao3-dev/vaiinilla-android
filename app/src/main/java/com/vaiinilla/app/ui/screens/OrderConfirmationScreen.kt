package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.OrderUser
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.awaitsAcceptanceBeforePaying
import com.vaiinilla.app.domain.model.awaitsPaymentAfterAcceptance
import com.vaiinilla.app.domain.model.isStripePaymentConfirmedByBackend
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.ui.order.JourneyVenue
import com.vaiinilla.app.ui.order.PurchaseCelebration
import com.vaiinilla.app.ui.order.STRIPE_ABANDON_VISIBLE_AFTER_MS
import com.vaiinilla.app.ui.order.StripePaymentPhase
import com.vaiinilla.app.ui.theme.VaiinillaTheme

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
    stripePaymentPhase: StripePaymentPhase = StripePaymentPhase.IDLE,
    stripePaymentMessage: String? = null,
    retryingStripePayment: Boolean = false,
    onRetryStripePayment: () -> Unit = {},
    onRefreshStripePayment: () -> Unit = {},
    onReturnStripeToCart: (OrderDetail) -> Unit = {},
    abandoningStripePayment: Boolean = false,
    onAbandonStripePayment: (OrderDetail) -> Unit = {},
    abandonVisibleAfterMs: Long = STRIPE_ABANDON_VISIBLE_AFTER_MS,
    purchaseCelebration: PurchaseCelebration? = null,
    onPurchaseCelebrationFinished: (String) -> Unit = {},
    screenshotPrinted: Boolean = false,
    deliveryRequiresQr: Boolean = true,
    showTrackingButton: Boolean = true,
    onCurrentWaiterCall: (suspend (Int) -> Result<TableCall?>)? = null,
    onCallWaiter: (suspend (Int, CallReason, String?) -> Result<TableCall>)? = null,
    onCancelWaiter: (suspend (TableCall) -> Result<TableCall>)? = null,
    /** Sin sesión el pedido solo vive en este teléfono: se le avisa al invitado (A06). */
    guest: Boolean = false,
    venue: JourneyVenue = JourneyVenue(),
    onAnnounceArrival: ((String) -> Unit)? = null,
    /** «Pedir otra cosa» de un pedido cancelado o no recogido; sin él, el botón no se ve. */
    onOpenMenu: (() -> Unit)? = null,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(vainiversoStageColor()),
    ) {
        if (order == null) return@Box
        val activeCelebration = purchaseCelebration?.takeIf { it.orderId == order.summary.id }
        // «¡Pedido recibido!» se quedaba aunque el negocio ya lo hubiera rechazado (S04): al terminar sin entregarse
        // se cierra la celebración y queda la página del pedido, que dice qué pasó y qué pasa con el dinero.
        val endedWithoutDelivery = order.summary.state.isTerminalWithoutDelivery
        LaunchedEffect(activeCelebration?.orderId, endedWithoutDelivery) {
            if (endedWithoutDelivery) activeCelebration?.let { onPurchaseCelebrationFinished(it.orderId) }
        }
        val stripeOrder = order.summary.paymentMethod == PaymentMethod.STRIPE
        val stripeConfirmed = stripeOrder && order.isStripePaymentConfirmedByBackend()
        // Stripe orders must never expose the pickup QR before the backend
        // confirms the payment. READY/PRESENTING are still part of checkout.
        val stripeStatusScreen = stripeOrder && !stripeConfirmed
        // Un encargo que espera al negocio o que ya se aceptó y falta pagar se sigue en la página del pedido (D1, D1b).
        val awaitingBusiness =
            order.awaitsAcceptanceBeforePaying() ||
                (order.awaitsPaymentAfterAcceptance() && stripePaymentPhase !in STRIPE_SHEET_PHASES)
        if (stripeStatusScreen && awaitingBusiness && !order.summary.state.isTerminalWithoutDelivery) {
            OrderJourneyPage(
                order = order,
                qrPayload = null,
                onBack = onViewTracking,
                deliveryRequiresQr = deliveryRequiresQr,
                venue = venue,
                onPay = onRetryStripePayment.takeIf { order.awaitsPaymentAfterAcceptance() },
                paying = retryingStripePayment,
            )
            return@Box
        }
        if (stripeStatusScreen) {
            StripePaymentPendingScreen(
                order = order,
                phase = stripePaymentPhase,
                message = stripePaymentMessage,
                retrying = retryingStripePayment,
                onRetry = onRetryStripePayment,
                onRefresh = onRefreshStripePayment,
                onReturnToCart = { onReturnStripeToCart(order) },
                onViewOrders = onViewTracking,
                abandoning = abandoningStripePayment,
                onAbandon = { onAbandonStripePayment(order) },
                abandonVisibleAfterMs = abandonVisibleAfterMs,
            )
            return@Box
        }
        // A domicilio no hay ticket que mostrar: la compra se confirma en su página y se sigue en la del pedido.
        val delivery = order.summary.destination == OrderDestination.HOME_DELIVERY
        if (delivery && activeCelebration != null) {
            PurchaseSuccessScreen(
                order = order,
                kind = activeCelebration.kind,
                venue = venue,
                onFollowOrder = { onPurchaseCelebrationFinished(activeCelebration.orderId) },
                onViewOrders = {
                    onPurchaseCelebrationFinished(activeCelebration.orderId)
                    onViewTracking()
                },
            )
            return@Box
        }
        val qrPayload =
            confirmationTicketQrPayload(order).takeIf {
                order.summary.destination != OrderDestination.HOME_DELIVERY
            }
        // Abierto desde Pedidos se sigue en la página del dibujo de Doop (P1 a P13). Recién comprado se queda la
        // tienda con su celebración y su ticket.
        if ((!showTrackingButton || delivery) && activeCelebration == null) {
            OrderJourneyPage(
                order = order,
                qrPayload = qrPayload,
                onBack = onReturnToMenu,
                deliveryRequiresQr = deliveryRequiresQr,
                venue = venue,
                onAnnounceArrival = onAnnounceArrival?.let { announce -> { announce(order.summary.id) } },
                onCurrentWaiterCall = onCurrentWaiterCall,
                onCallWaiter = onCallWaiter,
                onCancelWaiter = onCancelWaiter,
                onOpenMenu = onOpenMenu,
                guestNotice = GUEST_ORDER_NOTICE.takeIf { guest },
            )
            return@Box
        }
        PickupJourneyScreen(
            order = order,
            // A domicilio no se recoge en barra: el código de entrega va en el texto del estado.
            qrPayload = qrPayload,
            onBack = onReturnToMenu,
            onViewTracking = onViewTracking,
            celebration = activeCelebration?.kind,
            onCelebrationFinished = { activeCelebration?.let { onPurchaseCelebrationFinished(it.orderId) } },
            deliveryRequiresQr = deliveryRequiresQr,
            showTrackingButton = showTrackingButton,
            onCurrentWaiterCall = onCurrentWaiterCall,
            onCallWaiter = onCallWaiter,
            onCancelWaiter = onCancelWaiter,
            guestNotice = GUEST_ORDER_NOTICE.takeIf { guest },
            venue = venue,
            onAnnounceArrival = onAnnounceArrival?.let { announce -> { announce(order.summary.id) } },
            // Las rentas de cancha no se cobran en la caja del mostrador: no imprimen ticket.
            // A domicilio tampoco: se paga al recibir o ya se pagó en línea.
            printsTicket =
                activeCelebration != null &&
                    order.reservation == null &&
                    order.summary.destination != OrderDestination.HOME_DELIVERY,
        )
    }
}

internal const val GUEST_ORDER_NOTICE =
    "Guarda este pedido: solo se ve en este teléfono."

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
