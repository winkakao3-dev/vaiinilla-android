package com.vaiinilla.app.ui.order

import com.vaiinilla.app.core.text.normalizeForSearch
import com.vaiinilla.app.domain.admin.CashbackPreview
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.HomeCheckoutDraft
import com.vaiinilla.app.domain.model.HomeCheckoutRules
import com.vaiinilla.app.domain.model.HomeDeliveryOffer
import com.vaiinilla.app.domain.model.HomePayment
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreorderOffer
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.SpaceDelivery
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.domain.model.isStripePaymentConfirmedByBackend
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.assistant.AssistantChatMessage
import com.vaiinilla.app.ui.components.moneyLabel
import java.time.ZonedDateTime

data class OrderFlowUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val catalog: Catalog? = null,
    val operationalStatus: OperationalStatus? = null,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategoryId: Int? = null,
    val selectedProductId: Int? = null,
    val selectedOptionIds: Set<Int> = emptySet(),
    val selectedQuantity: Int = 1,
    /** Nota para cocina del producto que se está agregando; viaja con la línea. */
    val selectedNotes: String = "",
    val cartLines: List<CartLine> = emptyList(),
    val kitchenNotes: String = "",
    val checkoutDestination: OrderDestination = OrderDestination.TAKE_AWAY,
    val selectedSpaceId: Int = 0,
    /** Cancha rentada y en curso del cliente en este negocio: puede recibir ahí su comida. */
    val rentedCourt: PublicSpace? = null,
    val checkoutPayment: PaymentMethod = PaymentMethod.CASH,
    /** Pagar al final: el pedido va a la cuenta del espacio y se paga al irse (en efectivo). */
    val checkoutPayAtEnd: Boolean = false,
    /** «Pasa por él»: lo que eligió el cliente cuando el negocio ofrece las dos formas. */
    val checkoutPickUpAtBar: Boolean = false,
    /**
     * El servidor pidió confirmar «Para llevar» porque el cliente tiene lugar en una mesa o pista
     * (409 PEDIDO_PARA_LLEVAR_CON_SILLA_ACTIVA): con texto se muestra la pregunta.
     */
    val takeAwayWithSeatPrompt: String? = null,
    /** El cliente ya confirmó «Para llevar» con su silla activa: el pedido se reenvía con la confirmación. */
    val takeAwayConfirmed: Boolean = false,
    val creatingOrder: Boolean = false,
    val createOrderError: String? = null,
    /** Líneas del carrito que el menú vigente ya no permite pedir (agotadas o con opciones que cambiaron). */
    val unavailableCartKeys: Set<String> = emptySet(),
    /** Aviso de lo que cambió en el menú mientras el carrito esperaba: precios o productos quitados. */
    val cartNotice: String? = null,
    /** El menú salió de la memoria porque la recarga falló: precios y disponibilidad pueden haber cambiado. */
    val catalogStale: Boolean = false,
    val createdOrder: OrderDetail? = null,
    val stripeObservedOrder: OrderDetail? = null,
    val stripePendingOrderId: String? = null,
    val resolvingPendingStripePayment: Boolean = false,
    val stripePaymentSession: StripePaymentSession? = null,
    val stripePresentationKey: String? = null,
    val stripePaymentPhase: StripePaymentPhase = StripePaymentPhase.IDLE,
    val stripePaymentMessage: String? = null,
    val retryingStripePayment: Boolean = false,
    val purchaseCelebration: PurchaseCelebration? = null,
    val guestVenue: GuestVenueContext? = null,
    val guestVenueSuspended: Boolean = false,
    /** Compra sin cuenta: falta el nombre para dar de alta al invitado antes del primer pedido. */
    val guestEntryRequired: Boolean = false,
    val guestName: String = "",
    val assistantChatMessages: List<AssistantChatMessage> = emptyList(),
    /** A domicilio y encargos (CASA-05 a 08): dirección, cotización, día y forma de pago. */
    val homeCheckout: HomeCheckoutDraft = HomeCheckoutDraft(),
    val quotingDelivery: Boolean = false,
    val deliveryQuoteError: String? = null,
    /** Lo que el hotel ofrece al pedir (cargo a la habitación, tarjeta); `null` hasta que se lee o fuera de un hotel. */
    val hotelConfig: HotelConfig? = null,
    /** El tipo de negocio que se mira: la VM lo mantiene al día; de él dependen los textos y el pedido a habitación. */
    val venue: VenueContext = VenueContext.STANDARD,
    /** El servidor pidió la estancia (ROOM_REQUIRES_STAY): se abre el código de estancia dentro del carrito. */
    val needsStayCode: Boolean = false,
    val stayCodeError: String? = null,
    /** El código de estancia se está ligando: la hoja no deja mandarlo dos veces. */
    val linkingStay: Boolean = false,
    /** La regla de cashback activa del negocio; `null` si está apagado o no se pudo leer (24). */
    val cashbackRule: CashbackRule? = null,
)

/** A one-shot visual confirmation for the order currently being created. */
data class PurchaseCelebration(
    val orderId: String,
    val kind: PurchaseCelebrationKind,
)

enum class PurchaseCelebrationKind {
    PAYMENT_CONFIRMED,
    ORDER_RECEIVED,
}

/** Lo que dice la tarjeta de "¡Pedido recibido!": un pedido a la cuenta no se paga en Caja ahora. */
internal fun celebrationMessage(
    venue: VenueContext,
    order: OrderDetail,
    kind: PurchaseCelebrationKind,
): String =
    when {
        kind == PurchaseCelebrationKind.PAYMENT_CONFIRMED -> "Tu pago fue autorizado y tu pedido ya está en marcha."
        order.summary.payAtEnd && SpaceCopy.isRoomOrder(venue, order) ->
            "Se carga a tu ${venue.vocabulary.room}: la pagas al salir."
        order.summary.payAtEnd -> "Va a tu cuenta: la pagas al final."
        order.summary.destination == OrderDestination.HOME_DELIVERY ->
            if (order.homeDelivery?.hasAmountDue == true) {
                "Lo pagas al recibirlo. Te avisamos cuando el negocio lo acepte."
            } else {
                "Te avisamos cuando el negocio lo acepte."
            }
        else -> "Paga en Caja para continuar con tu pedido."
    }

internal fun purchaseCelebrationFor(order: OrderDetail): PurchaseCelebration? =
    when (order.summary.paymentMethod) {
        PaymentMethod.CASH ->
            PurchaseCelebration(
                orderId = order.summary.id,
                kind = PurchaseCelebrationKind.ORDER_RECEIVED,
            )
        PaymentMethod.BALANCE ->
            order
                .takeIf { it.summary.state.isPaidOrLater }
                ?.let {
                    PurchaseCelebration(
                        orderId = it.summary.id,
                        kind = PurchaseCelebrationKind.PAYMENT_CONFIRMED,
                    )
                }
        PaymentMethod.STRIPE ->
            order.takeIf { it.isStripePaymentConfirmedByBackend() }?.let {
                PurchaseCelebration(
                    orderId = it.summary.id,
                    kind = PurchaseCelebrationKind.PAYMENT_CONFIRMED,
                )
            }
    }

val OrderFlowUiState.selectedProduct: Product?
    get() = catalog?.products?.firstOrNull { it.id == selectedProductId }

val OrderFlowUiState.filteredProducts: List<Product>
    get() {
        // Lo agotado se queda en el menú con "Agotado" y sin Agregar (B10), al final de su categoría y no de todo el
        // menú: allá abajo parecía que había desaparecido (en vivo, #23). El orden va por categoría y por id, no por
        // el del servidor, que cambia con cada edición (marcar agotado lo mandaba al final).
        val categoryOrder =
            catalog?.categories.orEmpty().sortedBy { it.order }.withIndex().associate { (index, category) ->
                category.id to index
            }
        val products =
            catalog?.products.orEmpty().sortedWith(
                compareBy({ categoryOrder[it.categoryId] ?: Int.MAX_VALUE }, { !it.available }, { it.id }),
            )
        val categoryFiltered =
            selectedCategoryId
                ?.takeIf { id -> catalog?.categories.orEmpty().any { it.id == id } }
                ?.let { categoryId -> products.filter { it.categoryId == categoryId } }
                ?: products
        val query = searchQuery.normalizeForSearch()
        return if (query.isEmpty()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { product ->
                product.name.normalizeForSearch().contains(query) ||
                    product.description.normalizeForSearch().contains(query)
            }
        }
    }

val OrderFlowUiState.cartItemCount: Int
    get() = cartLines.sumOf(CartLine::quantity)

val OrderFlowUiState.cartPreviewTotal: String
    get() = Money.cartPreview(cartLines)

/**
 * Lo que se cobra por cada método: la tarjeta paga el precio digital del menú y efectivo o saldo el de mostrador
 * más extras, que difieren cuando el negocio pasa la comisión de la tarjeta al cliente (COMPRA-01, PAGO-10).
 * El servidor confirma el total; la tarjeta además calcula la cuota sobre el carrito completo, así que ahí
 * pueden sobrar o faltar centavos (se resuelve en el backend).
 */
fun OrderFlowUiState.cartTotalFor(method: PaymentMethod): String =
    if (method == PaymentMethod.STRIPE) Money.cartPreview(cartLines) else Money.counterPreview(cartLines)

/** El método con el que se cobra ahora: a domicilio manda el de la entrega. */
val OrderFlowUiState.chargedPaymentMethod: PaymentMethod
    get() = if (isHomeDelivery) homePaymentMethod else checkoutPayment

/** Lo que cuesta una línea con el método elegido, para que las líneas sumen el total que se ve. */
fun OrderFlowUiState.cartLineTotal(line: CartLine): String =
    if (chargedPaymentMethod == PaymentMethod.STRIPE) Money.cartLinePreview(line) else Money.counterLinePreview(line)

/** El total del método de pago que está elegido ahora, más el envío si va a domicilio. */
val OrderFlowUiState.checkoutTotal: String
    get() {
        val cart = cartTotalFor(chargedPaymentMethod)
        val fee = homeDeliveryFee ?: return cart
        return Money.format(Money.parse(cart) + Money.parse(fee))
    }

/** «Ganas $4.60 de cashback (1%)» para el carrito y la hoja de pago; `null` si este pedido no gana nada (24). */
fun OrderFlowUiState.cashbackLine(now: ZonedDateTime = ZonedDateTime.now(BusinessTime.zone)): String? {
    val percent = CashbackPreview.percentAt(cashbackRule, now) ?: return null
    val earned = CashbackPreview.earned(cashbackRule, checkoutTotal, now) ?: return null
    val shown = percent.stripTrailingZeros().toPlainString()
    return "Ganas ${moneyLabel(earned)} de cashback ($shown%) cuando te lo entreguen."
}

val OrderFlowUiState.homeDeliveryOffer: HomeDeliveryOffer?
    get() = operationalStatus?.homeDelivery

val OrderFlowUiState.preorderOffer: PreorderOffer?
    get() = operationalStatus?.preorders

val OrderFlowUiState.canDeliverHome: Boolean
    get() = homeDeliveryOffer != null

val OrderFlowUiState.isHomeDelivery: Boolean
    get() = checkoutDestination == OrderDestination.HOME_DELIVERY && canDeliverHome

val OrderFlowUiState.cartPreparationDays: Int
    get() = HomeCheckoutRules.preparationDays(cartLines.map { it.product })

/** En una mesa se come ahora: el día solo se elige para llevar o a domicilio. */
val OrderFlowUiState.asksForDeliveryDate: Boolean
    get() =
        checkoutDestination != OrderDestination.IN_SPACE &&
            HomeCheckoutRules.asksForDate(preorderOffer, cartPreparationDays)

val OrderFlowUiState.deliveryDates: List<String>
    get() {
        val offer = preorderOffer ?: return emptyList()
        if (!asksForDeliveryDate) return emptyList()
        return HomeCheckoutRules.dates(offer, cartPreparationDays, java.time.ZonedDateTime.now(BusinessTime.zone))
    }

val OrderFlowUiState.homePayments: List<HomePayment>
    get() = homeDeliveryOffer?.let { HomeCheckoutRules.payments(it, preorderOffer, canPayWithCard) }.orEmpty()

val OrderFlowUiState.homeDeliveryFee: String?
    get() = homeDeliveryOffer?.takeIf { isHomeDelivery }?.let { HomeCheckoutRules.fee(homeCheckout, it) }

/**
 * La forma de pago a domicilio que vale: la elegida si el negocio la ofrece; si no (p. ej. sin tarjeta), la
 * primera que ofrece. Null solo si no ofrece ninguna, y entonces no se puede pedir (auditoría R23).
 */
val OrderFlowUiState.effectiveHomePayment: HomePayment?
    get() = homeCheckout.payment.takeIf { it in homePayments } ?: homePayments.firstOrNull()

/** A domicilio la forma de pago se elige en el carrito, no en la hoja de pago. */
val OrderFlowUiState.homePaymentMethod: PaymentMethod
    get() = if (effectiveHomePayment == HomePayment.ON_ARRIVAL) PaymentMethod.CASH else PaymentMethod.STRIPE

/** Lo que falta para poder pedir a domicilio o para otro día; null si nada. */
val OrderFlowUiState.checkoutScheduleProblem: String?
    get() {
        val offer = homeDeliveryOffer
        if (isHomeDelivery && offer != null) {
            if (homePayments.isEmpty()) return "Este negocio aún no tiene cómo cobrar a domicilio."
            return HomeCheckoutRules.problem(homeCheckout, offer, cartTotalFor(homePaymentMethod), asksForDeliveryDate)
        }
        if (asksForDeliveryDate && homeCheckout.deliveryDate == null) return "Elige para qué día lo quieres."
        return null
    }

val OrderFlowUiState.selectedProductPreviewPrice: String
    get() = selectedProduct?.let { Money.productUnitPreview(it, selectedOptionIds) } ?: "0.00"

val OrderFlowUiState.selectedProductPreviewTotal: String
    get() = Money.format(Money.parse(selectedProductPreviewPrice) * selectedQuantity.toBigDecimal())

val OrderFlowUiState.isSelectedProductValid: Boolean
    get() =
        selectedProduct?.let { product ->
            runCatching { ContractRules.validateSelections(product, selectedOptionIds) }.isSuccess
        } ?: false

val OrderFlowUiState.isOperationallyReady: Boolean
    get() =
        // La app sigue la regla del backend: basta con que el negocio reciba pedidos. Si Caja
        // o Cocina están fuera de línea, el backend acepta el pedido y lo deja en su cola.
        operationalStatus?.acceptingOrders == true

val OrderFlowUiState.canSubmitCart: Boolean
    get() = cartLines.isNotEmpty() && !creatingOrder && !hasUnresolvedStripePayment && unavailableCartKeys.isEmpty()

val OrderFlowUiState.hasUnresolvedStripePayment: Boolean
    get() = !stripePendingOrderId.isNullOrBlank()

val OrderFlowUiState.requiresOperationalReady: Boolean
    get() =
        checkoutPayment == PaymentMethod.CASH ||
            checkoutPayment == PaymentMethod.BALANCE ||
            checkoutPayment == PaymentMethod.STRIPE

val OrderFlowUiState.canCreateOrder: Boolean
    get() = canSubmitCart && (!requiresOperationalReady || isOperationallyReady) && checkoutScheduleProblem == null

/** Espacios a los que se puede mandar el pedido: la cancha rentada (primero) y el del QR. */
val OrderFlowUiState.checkoutSpaces: List<PublicSpace>
    get() = listOfNotNull(rentedCourt, guestVenue?.space).distinctBy { it.id }

/**
 * Dónde está el cliente ahora. Con una cancha o pista rentada en curso, esa manda sobre la mesa del QR: quien
 * entró por «La Mesa 1» y rentó la Pista 3 ya está en la pista, y ahí deben llegar sus pedidos y el mesero.
 * La mesa del QR se queda en [GuestVenueContext.space] (mesa compartida, carrito guardado) por si la renta acaba.
 */
val OrderFlowUiState.currentSpace: PublicSpace?
    get() = rentedCourt ?: guestVenue?.space

/** El espacio elegido, si sigue entre los permitidos; si no, el primero (la cancha rentada va primero). */
val OrderFlowUiState.checkoutSpace: PublicSpace?
    get() = checkoutSpaces.firstOrNull { it.id == selectedSpaceId } ?: rentedCourt ?: guestVenue?.space

val OrderFlowUiState.checkoutSpaceId: Int?
    get() =
        if (checkoutDestination == OrderDestination.IN_SPACE) {
            checkoutSpace?.id
        } else {
            null
        }

val OrderFlowUiState.selectedSpaceName: String
    get() = checkoutSpace?.name ?: "Escanea el QR de tu mesa"

/**
 * Whether the known wallet balance can cover the cart. Unknown or unparsable
 * values stay affordable: the backend remains the authoritative check.
 */
fun OrderFlowUiState.isBalancePaymentAffordable(walletBalance: String?): Boolean {
    val balance = walletBalance?.let { runCatching { Money.parse(it) }.getOrNull() } ?: return true
    val total = runCatching { Money.parse(cartTotalFor(PaymentMethod.BALANCE)) }.getOrNull() ?: return true
    return balance >= total
}

/** Client-facing copy: never expose which staff devices are online. */
const val ESTABLISHMENT_CLOSED_MESSAGE =
    "El establecimiento no está abierto en este momento. Verifica que esté abierto y desliza hacia abajo para actualizar."

/** Fuera de las franjas que eligió el dueño se dice el horario; si no, el aviso genérico de cerrado. */
fun OperationalStatus.checkoutStaffBlocker(): String? =
    outsideOrderHoursMessage ?: if (acceptingOrders) null else ESTABLISHMENT_CLOSED_MESSAGE

val OrderFlowUiState.operationalBlockerMessage: String?
    get() {
        if (cartLines.isEmpty() || isOperationallyReady) return null
        val status = operationalStatus ?: return "No pudimos verificar si el establecimiento está recibiendo pedidos."
        return status.checkoutStaffBlocker()
    }

/** Aviso de arriba del menú: el negocio no recibe pedidos ahora (antes de armar el carrito) o el menú es de memoria. */
val OrderFlowUiState.menuNotice: String?
    get() =
        operationalStatus?.takeUnless { it.acceptingOrders }?.checkoutStaffBlocker()
            ?: STALE_MENU_NOTICE.takeIf { catalogStale }

private const val STALE_MENU_NOTICE =
    "No pudimos actualizar el menú. Desliza hacia abajo para reintentar: los precios pueden haber cambiado."

/**
 * ¿Se ofrece "Pagar al final"? Solo en un espacio y si el establecimiento lo permite. El invitado que aún no da su
 * nombre no puede leer el estado operativo: mientras tanto vale lo que dice la ficha pública del negocio.
 */
val OrderFlowUiState.canPayAtEnd: Boolean
    get() =
        checkoutDestination == OrderDestination.IN_SPACE &&
            if (isHotelRoomOrder) {
                // En un hotel «pagar al final» es el cargo a la habitación y lo enciende el hotel, no la mesa.
                (hotelConfig ?: HotelConfig()).chargeToRoom
            } else {
                (operationalStatus?.allowsPayAtEnd ?: guestVenue?.establishment?.allowsPayAtEnd) == true
            }

/** ¿El pedido va a la habitación de un hotel? Ahí el pago y los textos cambian. */
val OrderFlowUiState.isHotelRoomOrder: Boolean
    get() =
        venue.capabilities.tablesAreRooms &&
            checkoutDestination == OrderDestination.IN_SPACE &&
            SpaceCopy.isRoom(venue, checkoutSpace)

/** Cómo entrega el negocio en mesa o lugar; el invitado sin sesión lo lee de la ficha pública. */
val OrderFlowUiState.spaceDelivery: SpaceDelivery
    get() = operationalStatus?.spaceDelivery ?: guestVenue?.establishment?.spaceDelivery ?: SpaceDelivery.TABLE

/** ¿Se pregunta «Te lo llevamos» o «Pasa por él»? Solo en un espacio de un negocio que ofrece las dos. */
val OrderFlowUiState.asksSpaceDelivery: Boolean
    get() = checkoutDestination == OrderDestination.IN_SPACE && spaceDelivery.asksCustomer

/** Lo que viaja en el pedido: en un espacio decide el negocio, o el cliente si se ofrecen las dos. */
val OrderFlowUiState.pickUpAtBarForOrder: Boolean
    get() = checkoutDestination == OrderDestination.IN_SPACE && spaceDelivery.pickUpAtBar(checkoutPickUpAtBar)

/** Después de pedir a la cuenta en una mesa, la siguiente ronda sigue a la cuenta. */
val OrderFlowUiState.payAtEndForNextRound: Boolean
    get() = checkoutPayAtEnd && checkoutDestination == OrderDestination.IN_SPACE

/** ¿Se ofrece pagar con tarjeta? Solo si el dueño la activó en su panel. */
val OrderFlowUiState.canPayWithCard: Boolean
    get() = operationalStatus?.acceptsCard == true && (!isHotelRoomOrder || hotelConfig?.cardAtOrderEnabled == true)

/** ¿Se ofrece pagar con saldo? No si el dueño lo apagó; sin estado operativo todavía, sí (el servidor valida). */
val OrderFlowUiState.canPayWithBalance: Boolean
    get() = operationalStatus?.allowsBalance != false

/**
 * ¿Se ofrece «Pago en caja»? No si el dueño lo apagó (`permite_pago_en_caja`); sin el dato (servidor anterior o
 * estado sin cargar), sí. Pagar al final, a domicilio, la renta y el personal no dependen de esto.
 */
val OrderFlowUiState.canPayAtRegister: Boolean
    get() = operationalStatus?.allowsCashAtRegister != false

enum class StripePaymentPhase {
    IDLE,
    READY,
    PRESENTING,
    PROCESSING_CONFIRMATION,
    PENDING,
    TIMED_OUT,
    CONFIRMED,
    FAILED,
    CANCELED,
    REFUNDING,
    REFUNDED,

    /** Encargo con tarjeta que el negocio revisa: todavía no hay nada que pagar. */
    AWAITING_ACCEPTANCE,

    /** El negocio aceptó el encargo y le toca pagar al cliente. */
    AWAITING_PAYMENT,
}

internal fun isEstablishmentSwitch(
    current: GuestVenueContext?,
    next: GuestVenueContext,
): Boolean = current?.establishment?.id?.let { it != next.establishment.id } == true
