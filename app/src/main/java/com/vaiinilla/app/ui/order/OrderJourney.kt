package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.deadlineLabel
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.moneyLabel
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Cómo se sigue un pedido: cada tipo tiene sus pasos, su "qué sigue" y sus datos (diseño P1 a P13 de Doop). */
internal enum class OrderJourney {
    COUNTER,
    DRIVE_THRU,
    TABLE,
    BAR,
    SEAT,
    COURT_FOOD,
    COURT_RENTAL,
    SCHOOL,
    PREORDER_PICKUP,
    HOME_DELIVERY,
}

/** Lo que la app sabe del negocio donde está el cliente; el pedido no trae su tipo ni su WhatsApp. */
data class JourneyVenue(
    val driveThru: Boolean = false,
    val school: Boolean = false,
    val whatsappUrl: String? = null,
)

internal fun orderJourney(
    order: OrderDetail,
    venue: JourneyVenue = JourneyVenue(),
): OrderJourney {
    val summary = order.summary
    return when {
        order.reservation != null -> OrderJourney.COURT_RENTAL
        summary.destination == OrderDestination.HOME_DELIVERY -> OrderJourney.HOME_DELIVERY
        summary.destination == OrderDestination.IN_SPACE ->
            when (summary.space?.type) {
                "barra" -> OrderJourney.BAR
                "asiento" -> OrderJourney.SEAT
                "cancha" -> OrderJourney.COURT_FOOD
                "drive_thru" -> OrderJourney.DRIVE_THRU
                else -> OrderJourney.TABLE
            }
        venue.driveThru -> OrderJourney.DRIVE_THRU
        summary.deliveryDate != null -> if (venue.school) OrderJourney.SCHOOL else OrderJourney.PREORDER_PICKUP
        else -> OrderJourney.COUNTER
    }
}

private val OPEN_STATES =
    setOf(
        OrderState.AWAITING_ACCEPTANCE,
        OrderState.PENDING_PAYMENT,
        OrderState.PAID,
        OrderState.PREPARING,
        OrderState.READY,
        OrderState.ON_THE_WAY,
    )

private fun OrderDetail.cashDue(): Boolean =
    summary.state == OrderState.PENDING_PAYMENT && summary.paymentMethod == PaymentMethod.CASH && !summary.payAtEnd

private fun OrderDetail.place(): String = summary.space?.name?.takeIf { it.isNotBlank() } ?: "tu lugar"

/** Los pasos de la barra de progreso; vacía cuando el tipo se cuenta de otra forma (la renta) o ya se cerró. */
internal fun journeySteps(
    order: OrderDetail,
    journey: OrderJourney,
): List<String> {
    if (journey == OrderJourney.COURT_RENTAL || order.summary.state !in OPEN_STATES + OrderState.DELIVERED) {
        return emptyList()
    }
    val first =
        when {
            order.cashDue() -> "Por pagar"
            order.summary.payAtEnd -> "Pedido"
            else -> "Pagado"
        }
    return when (journey) {
        OrderJourney.COUNTER -> listOf(first, "Preparando", "Listo", "Recogido")
        OrderJourney.DRIVE_THRU -> listOf(first, "Preparando", "Llegaste", "En tu auto")
        OrderJourney.TABLE -> listOf(first, "Preparando", "Listo", "En tu mesa")
        OrderJourney.BAR -> listOf(first, "Preparando", "Listo", "Entregado")
        OrderJourney.SEAT -> listOf(first, "Preparando", "En camino", "Entregado")
        OrderJourney.COURT_FOOD -> listOf(first, "Preparando", "Listo", "En tu cancha")
        OrderJourney.SCHOOL -> listOf("Apartado", "Ese día", "Listo", "Recogido")
        OrderJourney.PREORDER_PICKUP -> listOf("Enviado", "Aceptado", "Anticipo", "Haciéndose", "Recogido")
        OrderJourney.HOME_DELIVERY -> listOf("Enviado", "Aceptado", "Haciéndose", "En camino", "Entregado")
        OrderJourney.COURT_RENTAL -> emptyList()
    }
}

/** El paso en el que va el pedido: los anteriores ya pasaron. Con el pedido entregado, todos. */
internal fun journeyStepIndex(
    order: OrderDetail,
    journey: OrderJourney,
): Int {
    val steps = journeySteps(order, journey)
    if (steps.isEmpty()) return -1
    val state = order.summary.state
    if (state == OrderState.DELIVERED) return steps.size
    return when (journey) {
        OrderJourney.PREORDER_PICKUP ->
            when (state) {
                OrderState.AWAITING_ACCEPTANCE -> 0
                OrderState.PENDING_PAYMENT -> 2
                else -> 3
            }
        OrderJourney.HOME_DELIVERY ->
            when (state) {
                OrderState.AWAITING_ACCEPTANCE -> 0
                OrderState.READY, OrderState.ON_THE_WAY -> 3
                else -> 2
            }
        OrderJourney.DRIVE_THRU ->
            when {
                state == OrderState.PENDING_PAYMENT -> 0
                order.arrivedAt != null -> 2
                else -> 1
            }
        else ->
            when (state) {
                OrderState.AWAITING_ACCEPTANCE, OrderState.PENDING_PAYMENT -> 0
                OrderState.READY, OrderState.ON_THE_WAY -> 2
                else -> 1
            }
    }
}

private val SCHEDULE_DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-MX"))

/** "lun 9 nov · 12:00-13:00" para un pedido apartado; `null` si es para ahora. */
internal fun journeySchedule(order: OrderDetail): String? {
    val date = order.summary.deliveryDate ?: return null
    val day =
        runCatching { LocalDate.parse(date.take(10)).format(SCHEDULE_DAY).replace(".", "") }.getOrNull() ?: return null
    return listOfNotNull(day, order.summary.deliverySlot?.takeIf { it.isNotBlank() }).joinToString(" · ")
}

/** La línea de "qué sigue" de cada tipo; `null` deja el texto de siempre ([pickupHint]). */
internal fun journeyHint(
    order: OrderDetail,
    journey: OrderJourney,
    now: Instant = Instant.now(),
    zone: ZoneId = BusinessTime.zone,
): String? {
    val state = order.summary.state
    if (state !in OPEN_STATES || order.cashDue()) return null
    val today = now.atZone(zone).toLocalDate()
    val making = state == OrderState.PAID || state == OrderState.PREPARING
    val folio = order.summary.folio
    return when (journey) {
        OrderJourney.COUNTER ->
            if (making) "Te avisamos cuando esté en la barra. No haces fila: muestras tu código y te lo dan." else null
        OrderJourney.DRIVE_THRU ->
            when {
                state == OrderState.READY && order.arrivedAt != null ->
                    "Ya está. Te lo dan en la ventanilla con tu folio $folio."
                state == OrderState.READY -> "Está listo. Di tu folio $folio en la ventanilla."
                order.arrivedAt != null -> "Ya saben que llegaste. Lo terminan y te lo dan en la ventanilla."
                making -> "Avisa con «Ya llegué» cuando entres al carril y lo terminan para tu llegada."
                else -> null
            }
        OrderJourney.TABLE ->
            when {
                order.summary.payAtEnd && making -> "Te lo llevan a ${order.place()}. Pagas todo junto al final."
                else -> null
            }
        OrderJourney.BAR ->
            when {
                state == OrderState.READY -> "Está listo en la barra. Di tu folio $folio."
                making -> "Te lo entregan en la barra. Te llaman por tu folio $folio."
                else -> null
            }
        OrderJourney.SEAT ->
            when {
                state == OrderState.READY -> "Ya va en camino a ${order.place()}."
                making -> "Te lo llevan a ${order.place()}. No tienes que salir de la sala."
                else -> null
            }
        OrderJourney.COURT_FOOD ->
            when {
                state == OrderState.READY -> "Ya va a ${order.place()}. Lo dejan sin interrumpir el juego."
                making -> "Te lo llevan a ${order.place()} sin interrumpir el juego."
                else -> null
            }
        OrderJourney.SCHOOL ->
            when {
                state == OrderState.READY -> "Está listo. Recógelo en el comedor con tu código."
                making -> journeySchedule(order)?.let { "Apartado para el $it. Se recoge en el comedor con tu código." }
                else -> null
            }
        OrderJourney.PREORDER_PICKUP, OrderJourney.HOME_DELIVERY -> {
            val pickup = journey == OrderJourney.PREORDER_PICKUP
            when {
                state == OrderState.AWAITING_ACCEPTANCE ->
                    order.answerBy?.let {
                        "El negocio te contesta ${deadlineLabel(
                            it,
                            zone,
                            today,
                        )}. Aún no se te cobra nada."
                    }
                        ?: "El negocio revisa tu encargo y te confirma. Aún no se te cobra nada."
                state == OrderState.PENDING_PAYMENT && order.summary.paymentMethod == PaymentMethod.STRIPE ->
                    order.payBy?.let {
                        "Aceptaron tu encargo. Paga el anticipo ${deadlineLabel(
                            it,
                            zone,
                            today,
                        )}; empiezan cuando lo pagues."
                    }
                        ?: "Aceptaron tu encargo. Empiezan cuando pagues el anticipo."
                pickup && making ->
                    journeySchedule(order)?.let { "Se está haciendo. Pasa por él el $it." }
                        ?: "Se está haciendo."
                pickup && state == OrderState.READY -> "Está listo. Pasa por él y muestra tu código."
                else -> null
            }
        }
        OrderJourney.COURT_RENTAL -> null
    }
}

internal enum class FactTone { PLAIN, GOOD, DUE }

internal data class JourneyFact(
    val label: String,
    val value: String,
    val tone: FactTone = FactTone.PLAIN,
)

private fun String?.positive(): BigDecimal? = this?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }

/** Dónde, cuándo y cuánto: las filas de la tarjeta del pedido. Solo lo que el pedido trae; nada se inventa. */
internal fun journeyFacts(
    order: OrderDetail,
    journey: OrderJourney,
): List<JourneyFact> {
    val summary = order.summary
    val state = summary.state
    val open = state in OPEN_STATES
    return buildList {
        when (journey) {
            OrderJourney.COUNTER -> if (open) add(JourneyFact("Recoges en", "Barra de entrega"))
            OrderJourney.DRIVE_THRU -> if (open) add(JourneyFact("En la ventanilla di", "Folio ${summary.folio}"))
            OrderJourney.BAR -> if (open) add(JourneyFact("Tu folio", "${summary.folio}"))
            OrderJourney.TABLE, OrderJourney.SEAT, OrderJourney.COURT_FOOD ->
                if (open) add(JourneyFact("Te lo llevan a", order.place()))
            OrderJourney.SCHOOL -> journeySchedule(order)?.let { add(JourneyFact("Se recoge el", it)) }
            OrderJourney.PREORDER_PICKUP -> journeySchedule(order)?.let { add(JourneyFact("Lo recoges", it)) }
            OrderJourney.HOME_DELIVERY -> {
                order.homeDelivery?.address?.address?.takeIf { it.isNotBlank() }?.let {
                    add(
                        JourneyFact("Entrega en", it),
                    )
                }
                journeySchedule(order)?.let { add(JourneyFact("Llega", it)) }
            }
            OrderJourney.COURT_RENTAL -> Unit
        }
        val delivery = order.homeDelivery
        when {
            summary.payAtEnd -> {
                add(JourneyFact("Este pedido", moneyLabel(summary.total)))
                summary.amountPaid.positive()?.let { add(JourneyFact("Pagado en la mesa", moneyLabel(it))) }
                summary.balanceDue.positive()?.let { add(JourneyFact("Falta pagar", moneyLabel(it), FactTone.DUE)) }
            }
            order.cashDue() -> add(JourneyFact("Total a pagar en Caja", moneyLabel(summary.total), FactTone.DUE))
            delivery?.deposit.positive() != null &&
                state in setOf(OrderState.AWAITING_ACCEPTANCE, OrderState.PENDING_PAYMENT) -> {
                add(JourneyFact("Anticipo", moneyLabel(delivery!!.deposit!!), FactTone.DUE))
                delivery.dueOnArrival.positive()?.let {
                    add(
                        JourneyFact(
                            if (journey ==
                                OrderJourney.PREORDER_PICKUP
                            ) {
                                "Al recoger"
                            } else {
                                "Al recibir"
                            },
                            moneyLabel(it),
                        ),
                    )
                }
            }
            state == OrderState.AWAITING_ACCEPTANCE -> add(JourneyFact("Total", moneyLabel(summary.total)))
            state == OrderState.PENDING_PAYMENT -> add(JourneyFact("Total", moneyLabel(summary.total), FactTone.DUE))
            else -> {
                val how =
                    when (summary.paymentMethod) {
                        PaymentMethod.BALANCE -> "Pagado con saldo"
                        PaymentMethod.STRIPE -> "Pagaste con tarjeta"
                        PaymentMethod.CASH -> "Pagaste en efectivo"
                    }
                val paid = delivery?.deposit.positive()?.takeIf { delivery?.hasAmountDue == true }
                add(
                    JourneyFact(
                        if (paid !=
                            null
                        ) {
                            "Pagaste el anticipo"
                        } else {
                            how
                        },
                        paid?.let { moneyLabel(it) } ?: moneyLabel(summary.total),
                    ),
                )
                if (paid != null && open) {
                    delivery?.dueOnArrival.positive()?.let {
                        val label =
                            if (journey ==
                                OrderJourney.PREORDER_PICKUP
                            ) {
                                "Pagas al recoger"
                            } else {
                                "Pagas al recibir"
                            }
                        add(JourneyFact(label, moneyLabel(it), FactTone.DUE))
                    }
                }
            }
        }
        val rejected =
            order.items
                .mapNotNull { it.rejection?.amount?.toBigDecimalOrNull() }
                .fold(BigDecimal.ZERO, BigDecimal::add)
        if (rejected.signum() > 0 && state != OrderState.CANCELED) {
            add(JourneyFact("Te regresamos", moneyLabel(rejected), FactTone.GOOD))
        }
        summary.combinedSavings.positive()?.let { add(JourneyFact("Ahorraste", moneyLabel(it), FactTone.GOOD)) }
        if (state != OrderState.CANCELED) {
            summary.cashbackAwarded.positive()?.let {
                add(
                    JourneyFact("Ganaste de cashback", moneyLabel(it), FactTone.GOOD),
                )
            }
        }
    }
}

/** El aviso punteado de abajo: lo que conviene saber de este tipo en este momento. */
internal fun journeyNote(
    order: OrderDetail,
    journey: OrderJourney,
): String? {
    val state = order.summary.state
    return when {
        state == OrderState.CANCELED -> cancellationMoneyNote(order)
        state == OrderState.NOT_PICKED_UP ->
            "Si fue un error o tuviste un problema, escríbele al negocio: desde su Caja pueden devolverte el dinero."
        state !in OPEN_STATES -> null
        order.cashDue() -> "Hasta que pagues no se prepara nada y no se te cobra nada."
        state == OrderState.AWAITING_ACCEPTANCE && order.answerBy != null ->
            "Si no contesta a tiempo, se cancela solo y no pagas nada."
        order.items.any { it.rejection != null } -> "Lo demás de tu pedido sigue igual."
        journey == OrderJourney.DRIVE_THRU && state != OrderState.READY ->
            "Si llegas y aún no está listo, espera cerca de la ventanilla: te lo dan en cuanto salga."
        journey == OrderJourney.BAR -> "Si te mueves a una mesa, dile al mesero tu folio."
        journey == OrderJourney.SEAT -> "Si te cambias de lugar, díselo a quien te lo lleve."
        journey == OrderJourney.COURT_FOOD -> "Esta compra va aparte de tu renta."
        journey == OrderJourney.SCHOOL && order.summary.paymentMethod == PaymentMethod.BALANCE ->
            "Si se cancela, el dinero regresa a su cartera."
        journey == OrderJourney.PREORDER_PICKUP && order.homeDelivery?.hasAmountDue == true ->
            "Al llegar muestra tu código. Lo que falta lo pagas ahí."
        else -> null
    }
}
