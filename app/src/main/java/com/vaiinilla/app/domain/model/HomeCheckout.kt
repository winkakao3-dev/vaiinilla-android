package com.vaiinilla.app.domain.model

import com.vaiinilla.app.domain.admin.HomeBusinessRules
import com.vaiinilla.app.domain.admin.StoreRules
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Cómo paga el cliente un pedido a domicilio (CASA-07). */
enum class HomePayment {
    CARD,

    /** Con tarjeta solo el anticipo; el resto en la puerta. */
    DEPOSIT,

    /** Todo en la puerta, en efectivo o por transferencia. */
    ON_ARRIVAL,
}

/** Lo que el cliente llena para que le lleven el pedido y le confirmen el día. */
data class HomeCheckoutDraft(
    val address: DeliveryAddress = DeliveryAddress(address = "", phone = ""),
    /** La respuesta del negocio para la dirección escrita; se borra al cambiarla. */
    val quote: HomeDeliveryQuote? = null,
    /** `AAAA-MM-DD`; null: lo antes posible. */
    val deliveryDate: String? = null,
    val payment: HomePayment = HomePayment.CARD,
)

/** Las reglas del carrito a domicilio y de los encargos, iguales a `HomeCheckoutRules` de iOS. */
object HomeCheckoutRules {
    /** Las formas de pago que ofrece el negocio, en el orden en que se muestran. */
    fun payments(
        delivery: HomeDeliveryOffer,
        preorders: PreorderOffer?,
        acceptsCard: Boolean,
    ): List<HomePayment> =
        buildList {
            if (acceptsCard) add(HomePayment.CARD)
            if (acceptsCard && preorders?.depositPercent != null) add(HomePayment.DEPOSIT)
            if (delivery.payOnArrival) add(HomePayment.ON_ARRIVAL)
        }

    /** Los días de margen que pide el carrito: el producto que más tarda manda. */
    fun preparationDays(products: List<Product>): Int = products.maxOfOrNull { it.preparationDays ?: 0 } ?: 0

    /** ¿Hay que elegir día? Solo si el negocio toma encargos y el carrito no se entrega hoy, o deja pedir para otro día. */
    fun asksForDate(
        preorders: PreorderOffer?,
        preparationDays: Int,
    ): Boolean = preorders != null && (preparationDays > 0 || preorders.daysAhead > 0)

    /** Los días que se pueden elegir. Pasada la hora de corte, hoy ya no cuenta como día de margen. */
    fun dates(
        preorders: PreorderOffer,
        preparationDays: Int,
        now: ZonedDateTime,
    ): List<String> {
        var offset = preparationDays
        val cutoff = preorders.cutoff?.let(StoreRules::time)
        if (cutoff != null && !now.toLocalTime().isBefore(cutoff)) offset += 1
        val last = minOf(maxOf(preorders.daysAhead, offset), HomeBusinessRules.MAX_DAYS_AHEAD + 1)
        if (offset > last) return emptyList()
        val today = now.toLocalDate()
        return (offset..last).map { today.plusDays(it.toLong()).toString() }
    }

    /** "Hoy", "Mañana" o "sáb 7 nov". */
    fun dayLabel(
        wire: String,
        today: LocalDate,
    ): String {
        val date = runCatching { LocalDate.parse(wire) }.getOrNull() ?: return wire
        return when (date) {
            today -> "Hoy"
            today.plusDays(1) -> "Mañana"
            else -> DAY.format(date).replace(".", "")
        }
    }

    /** Lo que falta para pedir; null si ya se puede. */
    fun problem(
        draft: HomeCheckoutDraft,
        delivery: HomeDeliveryOffer,
        subtotal: String,
        needsDate: Boolean,
    ): String? {
        draft.address.problem?.let { return it }
        val quote = draft.quote ?: return "Revisa la dirección para calcular el envío."
        if (!quote.reaches) return "Esta dirección queda fuera de la zona de entrega."
        val minimum = (quote.minimum ?: delivery.minimum)?.toBigDecimalOrNull()
        val total = subtotal.toBigDecimalOrNull() ?: BigDecimal.ZERO
        if (minimum != null && minimum.signum() > 0 && total < minimum) {
            return "El pedido mínimo a domicilio es de ${compact(minimum)}."
        }
        if (needsDate && draft.deliveryDate == null) return "Elige para qué día lo quieres."
        return null
    }

    /** El costo del envío que se le enseña al cliente: el de la cotización, o el fijo del negocio mientras no hay. */
    fun fee(
        draft: HomeCheckoutDraft,
        delivery: HomeDeliveryOffer,
    ): String = draft.quote?.fee ?: delivery.fee

    private fun compact(amount: BigDecimal): String =
        "$" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().removeSuffix(".00")

    private val DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-MX"))
}

/** Lo que el negocio puede hacer con un pedido a domicilio o un encargo, según su estado (CASA-09). */
enum class HomeOrderStep {
    DECIDE,
    SEND_OUT,
    DELIVER,
    COLLECT,
}

object HomeOrderBoard {
    fun step(order: OrderDetail): HomeOrderStep? =
        when (order.summary.state) {
            OrderState.AWAITING_ACCEPTANCE -> HomeOrderStep.DECIDE
            // Quien trabaja sola no pasa por Cocina: el servidor deja salir un pedido a domicilio desde cobrado.
            OrderState.PAID, OrderState.PREPARING, OrderState.READY ->
                HomeOrderStep.SEND_OUT.takeIf { order.summary.destination == OrderDestination.HOME_DELIVERY }
            OrderState.ON_THE_WAY -> HomeOrderStep.DELIVER
            OrderState.DELIVERED -> HomeOrderStep.COLLECT.takeIf { order.homeDelivery?.hasAmountDue == true }
            else -> null
        }

    /** Entregados que falta cobrar, sin repetir el mismo pedido. */
    fun owed(orders: List<OrderDetail>): List<OrderDetail> =
        orders.filter { step(it) == HomeOrderStep.COLLECT }.distinctBy { it.summary.id }

    fun total(orders: List<OrderDetail>): String =
        orders
            .sumOf { it.homeDelivery?.dueOnArrival?.toBigDecimalOrNull() ?: BigDecimal.ZERO }
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .toPlainString()
}
