package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.domain.model.cashAmountDue
import com.vaiinilla.app.domain.model.deadlineLabel
import com.vaiinilla.app.domain.model.needsCashCollection
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
    /** El nombre del negocio, para la fila de arriba del seguimiento. */
    val name: String? = null,
)

/**
 * Si el cliente pide en una escuela. El pedido no trae el tipo de negocio (`tipo_establecimiento` aún no sale del
 * servidor) y el estado operativo puede no haber cargado al abrir Pedidos: el alumno veía su apartado como un
 * encargo cualquiera (P08). Vale el `modo_escuela` del estado, el de su membresía de cliente en ese negocio, o la
 * tutela; sin negocio a la vista, que todas sus membresías de cliente sean escuelas.
 */
internal fun inSchoolVenue(
    statusSchool: Boolean?,
    venueId: String?,
    modes: List<AuthorizedMode>,
    hasGuardian: Boolean = false,
): Boolean {
    if (statusSchool == true || hasGuardian) return true
    val clientModes = modes.filter { it.role == OperationalRole.CLIENT }
    if (venueId != null) return clientModes.any { it.establishmentId == venueId && it.isSchool }
    return statusSchool == null && clientModes.isNotEmpty() && clientModes.all { it.isSchool }
}

internal fun orderJourney(
    order: OrderDetail,
    venue: JourneyVenue = JourneyVenue(),
): OrderJourney {
    val summary = order.summary
    return when {
        order.reservation != null -> OrderJourney.COURT_RENTAL
        summary.destination == OrderDestination.HOME_DELIVERY -> OrderJourney.HOME_DELIVERY
        // «Pasa por él» (`recoge_en_barra`): se pide desde el lugar pero se entrega en la barra.
        summary.destination == OrderDestination.IN_SPACE && summary.pickUpAtBar -> OrderJourney.BAR
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

/** «pastel» si pidió una sola cosa; con varias, «pedido» o «encargo». */
private fun OrderDetail.itemWord(journey: OrderJourney): String {
    val fallback = if (journey == OrderJourney.COUNTER) "pedido" else "encargo"
    val live = items.filter { it.rejection == null }
    return live
        .singleOrNull()
        ?.productName
        ?.trim()
        ?.substringBefore(' ')
        ?.lowercase()
        ?.ifBlank { fallback } ?: fallback
}

/** Cómo se dice el lugar en una frase: «la Mesa 4», «tu asiento», «tu Cancha 2». */
private fun OrderDetail.placePhrase(journey: OrderJourney): String =
    when (journey) {
        OrderJourney.SEAT -> "tu asiento"
        OrderJourney.COURT_FOOD -> "tu ${place()}"
        OrderJourney.TABLE -> if (place().startsWith("Mesa")) "la ${place()}" else place()
        else -> place()
    }

/**
 * El renglón bajo lo pedido en un apartado o un encargo para recoger: cuándo se recoge (P08). Decía solo «Apartado ·
 * se recoge en el comedor»; sin fecha queda esa frase.
 */
internal fun scheduledPickupLine(
    order: OrderDetail,
    journey: OrderJourney,
): String {
    val schedule = journeyWhen(order)
    return if (journey == OrderJourney.SCHOOL) {
        schedule?.let { "Se recoge el $it" } ?: "Apartado · se recoge en el comedor"
    } else {
        schedule?.let { "Lo recoges el $it" } ?: "Encargo · lo recoges en el local"
    }
}

/**
 * El pedido es para un día que todavía no llega, en la hora del negocio. Con la fecha de entrega en el futuro, «Ya
 * está. Pasa por él» se decía un día antes (P08).
 */
internal fun OrderDetail.beforePickupDay(today: LocalDate): Boolean {
    val date = summary.deliveryDate?.take(10) ?: return false
    val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
    return day.isAfter(today)
}

/** «sáb 7 nov entre 11:00 y 13:00» para una frase; `null` si es para ahora. */
private fun journeyWhen(order: OrderDetail): String? {
    val schedule = journeySchedule(order) ?: return null
    val slot =
        order.summary.deliverySlot
            ?.split("-")
            ?.map { it.trim() }
            ?.takeIf { it.size == 2 } ?: return schedule
    return "${schedule.substringBefore(" · ")} entre ${slot[0]} y ${slot[1]}"
}

/** La sala, la fila y el asiento de un lugar llamado «Sala 5 · F12»; `null` si se llama de otra forma. */
internal data class JourneySeat(
    val room: String,
    val row: String,
    val seat: String,
)

internal fun journeySeat(order: OrderDetail): JourneySeat? =
    Regex("""Sala\s*(\w+)\D*?([A-Za-z])\s*(\d+)""")
        .find(
            order.summary.space
                ?.name
                .orEmpty(),
        )?.destructured
        ?.let { (room, row, seat) -> JourneySeat(room, row.uppercase(), seat) }

/** Los pasos de la barra de progreso; vacía cuando el tipo se cuenta de otra forma (la renta) o ya se cerró. */
internal fun journeySteps(
    venue: VenueContext,
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
        OrderJourney.COURT_FOOD -> listOf(first, "Preparando", "Listo", "En tu ${venue.vocabulary.court}")
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
    today: LocalDate = LocalDate.now(BusinessTime.zone),
): Int {
    // Solo importa cuántos pasos hay y si no hay ninguno: el vocabulario de cada paso no cambia eso.
    val steps = journeySteps(VenueContext.STANDARD, order, journey)
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
        // Apartado, Ese día, Listo, Recogido: hasta el día de recoger se queda en «Ese día», aunque ya esté listo.
        OrderJourney.SCHOOL ->
            when {
                state == OrderState.AWAITING_ACCEPTANCE || state == OrderState.PENDING_PAYMENT -> 0
                order.beforePickupDay(today) -> 1
                state == OrderState.READY || state == OrderState.ON_THE_WAY -> 3
                else -> 2
            }
        OrderJourney.DRIVE_THRU ->
            when {
                state == OrderState.PENDING_PAYMENT -> 0
                order.arrivedAt != null -> 2
                else -> 1
            }
        // Mesa y cancha: listo se queda en «Listo»; «En tu mesa» se marca hasta que el mesero lo entrega.
        OrderJourney.TABLE, OrderJourney.COURT_FOOD ->
            when (state) {
                OrderState.AWAITING_ACCEPTANCE, OrderState.PENDING_PAYMENT -> 0
                OrderState.READY, OrderState.ON_THE_WAY -> 2
                else -> 1
            }
        // Asiento: listo ya es «En camino». Los demás: listo cierra «Listo» y espera el último paso (Doop P2, P7).
        OrderJourney.SEAT ->
            when (state) {
                OrderState.AWAITING_ACCEPTANCE, OrderState.PENDING_PAYMENT -> 0
                OrderState.READY, OrderState.ON_THE_WAY -> 2
                else -> 1
            }
        else ->
            when (state) {
                OrderState.AWAITING_ACCEPTANCE, OrderState.PENDING_PAYMENT -> 0
                OrderState.READY, OrderState.ON_THE_WAY -> steps.size - 1
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
                state == OrderState.READY -> "Está listo. Te lo llevan a ${order.place()} sin interrumpir el juego."
                making -> "Te lo llevan a ${order.place()} sin interrumpir el juego."
                else -> null
            }
        OrderJourney.SCHOOL ->
            when {
                state == OrderState.READY && order.beforePickupDay(today) ->
                    journeySchedule(order)?.let { "Ya está apartado. Se recoge el $it en el comedor con tu código." }
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
                // Sin franja, un encargo que nace listo decía «Pasa por él» un día antes (P08): se dice qué día.
                pickup && state == OrderState.READY && order.beforePickupDay(today) ->
                    journeySchedule(order)?.let { "Ya está listo. Pasa por él el $it y muestra tu código." }
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
                    add(JourneyFact("Te lo llevan a", it))
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
            // Cocina puede empezarlo antes del cobro: sin pagar no se dice «Pagaste» (G07).
            summary.needsCashCollection ->
                add(JourneyFact("Total a pagar en Caja", moneyLabel(summary.cashAmountDue), FactTone.DUE))
            delivery?.deposit.positive() != null &&
                state in setOf(OrderState.AWAITING_ACCEPTANCE, OrderState.PENDING_PAYMENT) -> {
                add(
                    JourneyFact(
                        if (state ==
                            OrderState.AWAITING_ACCEPTANCE
                        ) {
                            "Anticipo, al aceptarlo"
                        } else {
                            "Anticipo, a pagar ahora"
                        },
                        moneyLabel(delivery!!.deposit!!),
                        FactTone.GOOD,
                    ),
                )
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
            state == OrderState.CANCELED && order.neverCharged() -> add(JourneyFact("Total", moneyLabel(summary.total)))
            order.rejectedAmount().signum() > 0 && state != OrderState.CANCELED && order.paidBeforeRejection() -> {
                add(
                    JourneyFact(
                        "Pagaste",
                        moneyLabel(
                            summary.total
                                .toBigDecimalOrNull()
                                ?.add(order.rejectedAmount())
                                ?.toPlainString() ?: summary.total,
                        ),
                    ),
                )
            }
            // Efectivo al recibir sin anticipo: aún no se paga nada. Decía «Pagaste en efectivo» antes de pagar (S01).
            summary.paymentMethod == PaymentMethod.CASH &&
                delivery != null &&
                delivery.hasAmountDue &&
                delivery.deposit.positive() == null -> {
                val label = if (journey == OrderJourney.PREORDER_PICKUP) "Pagas al recoger" else "Pagas al recibir"
                add(JourneyFact(label, moneyLabel(delivery.dueOnArrival ?: summary.total), FactTone.DUE))
            }
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
        val rejected = order.rejectedAmount()
        if (rejected.signum() > 0 && state != OrderState.CANCELED) {
            // Sin pago no hay nada que regresar: solo baja lo que se cobra (G07).
            if (order.paidBeforeRejection()) {
                add(JourneyFact("Te regresamos", moneyLabel(rejected), FactTone.GOOD))
            } else {
                add(JourneyFact("Ya no se cobra", moneyLabel(rejected), FactTone.GOOD))
            }
            if (!summary.payAtEnd) add(JourneyFact("Tu pedido ahora", moneyLabel(summary.total)))
        }
        if (state == OrderState.NOT_PICKED_UP) add(JourneyFact("Devolución", "La decide el negocio"))
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
        state == OrderState.AWAITING_ACCEPTANCE && order.homeDelivery?.deposit.positive() != null ->
            "Todavía no te cobramos nada. Cuando lo acepte tienes 24 horas para pagar tu anticipo de " +
                "${moneyLabel(
                    order.homeDelivery!!.deposit!!,
                )}, y empiezan tu ${order.itemWord(journey)} cuando lo pagues."
        state == OrderState.AWAITING_ACCEPTANCE && order.answerBy != null ->
            "Si no contesta a tiempo, se cancela solo y no pagas nada."
        state == OrderState.PENDING_PAYMENT && order.payBy != null ->
            "Si no lo pagas antes de esa hora, el pedido se cancela solo y no te cobramos nada."
        order.items.any {
            it.rejection != null
        } -> "Si ya no quieres lo demás, avísale al negocio antes de que lo terminen."
        journey == OrderJourney.DRIVE_THRU && state != OrderState.READY ->
            "Si llegas y aún no está listo, espera cerca de la ventanilla: te lo dan en cuanto salga."
        journey == OrderJourney.BAR && !order.summary.pickUpAtBar -> "Si te mueves a una mesa, dile al mesero tu folio."
        journey == OrderJourney.SEAT -> "Si te cambias de lugar, díselo a quien te lo lleve."
        journey == OrderJourney.COURT_FOOD -> "Esta compra va aparte de tu renta. La renta se paga en su propio pedido."
        journey == OrderJourney.SCHOOL && order.summary.paymentMethod == PaymentMethod.BALANCE ->
            "Si se cancela, el dinero regresa a su cartera."
        journey == OrderJourney.PREORDER_PICKUP && order.homeDelivery?.hasAmountDue == true ->
            "Al llegar muestra tu código. El resto lo pagas ahí, en efectivo o con tarjeta."
        else -> null
    }
}

/** El nombre corto de cada tipo, para la fila de arriba: «Pedido #27 · en la barra». */
internal fun journeyKindLabel(
    venue: VenueContext,
    journey: OrderJourney,
): String =
    when (journey) {
        OrderJourney.COUNTER -> "para llevar"
        OrderJourney.DRIVE_THRU -> "drive-thru"
        OrderJourney.TABLE -> "en tu mesa"
        OrderJourney.BAR -> "en la barra"
        OrderJourney.SEAT -> "a tu asiento"
        OrderJourney.COURT_FOOD -> "a tu ${venue.vocabulary.court}"
        OrderJourney.COURT_RENTAL -> "renta"
        OrderJourney.SCHOOL -> "comedor"
        OrderJourney.PREORDER_PICKUP -> "para recoger"
        OrderJourney.HOME_DELIVERY -> "a domicilio"
    }

/** «Pedido», «Apartado» o «Encargo», como lo llama el dibujo. */
internal fun journeyNoun(journey: OrderJourney): String =
    when (journey) {
        OrderJourney.SCHOOL -> "Apartado"
        OrderJourney.PREORDER_PICKUP -> "Encargo"
        else -> "Pedido"
    }

/** De qué color va la etiqueta de arriba: espera (yema), avanza bien (lima) o algo salió mal (coral). */
internal enum class CaptionTone { WAITING, DONE, PROBLEM }

internal data class JourneyCaption(
    val text: String,
    val tone: CaptionTone,
)

private fun OrderDetail.rejectedItems() = items.filter { it.rejection != null }

private const val REASON_IN_TITLE = 18

private fun OrderDetail.rejectedAmount(): BigDecimal =
    items.mapNotNull { it.rejection?.amount?.toBigDecimalOrNull() }.fold(BigDecimal.ZERO, BigDecimal::add)

/**
 * Quitar un artículo regresa dinero solo si ya se había pagado (G07). Por cobrar o a la cuenta sin cobrar, el servidor
 * solo baja el total (`docs/rechazo-articulo.md`). `monto_pagado` ya viene neto de lo devuelto; sin él, se juzga
 * por el saldo pendiente.
 */
private fun OrderDetail.paidBeforeRejection(): Boolean {
    if (summary.state == OrderState.PENDING_PAYMENT || summary.paymentPending) return false
    if (summary.payAtEnd && summary.amountPaid == null) return false
    val paid = summary.amountPaid?.toBigDecimalOrNull() ?: return !summary.needsCashCollection
    return paid.signum() > 0
}

private fun OrderDetail.depositPaid(): Boolean =
    homeDelivery?.deposit.positive() != null &&
        homeDelivery?.hasAmountDue == true &&
        summary.state in setOf(OrderState.PAID, OrderState.PREPARING, OrderState.READY, OrderState.ON_THE_WAY)

/** La etiqueta en mayúsculas sobre el título: en qué va y, si cabe, dónde. */
internal fun journeyCaption(
    venue: VenueContext,
    order: OrderDetail,
    journey: OrderJourney,
    today: LocalDate = LocalDate.now(BusinessTime.zone),
): JourneyCaption {
    val state = order.summary.state
    val waiting = CaptionTone.WAITING
    val done = CaptionTone.DONE
    return when {
        state == OrderState.CANCELED && order.depositDeadlineMissed() ->
            JourneyCaption("Venció el plazo de pago", CaptionTone.PROBLEM)
        state == OrderState.CANCELED && order.neverCharged() ->
            JourneyCaption(
                "No pudieron aceptarlo",
                CaptionTone.PROBLEM,
            )
        state == OrderState.CANCELED ->
            JourneyCaption(
                if (order.summary.canceledByRole in setOf("cocina", "cajero", "admin")) {
                    "Cancelado por el negocio"
                } else {
                    "Cancelado"
                },
                CaptionTone.PROBLEM,
            )
        state == OrderState.NOT_PICKED_UP -> JourneyCaption("No recogido", CaptionTone.PROBLEM)
        state == OrderState.EXPIRED -> JourneyCaption("Expirado", CaptionTone.PROBLEM)
        state == OrderState.DELIVERED ->
            JourneyCaption(journeySteps(venue, order, journey).lastOrNull() ?: "Entregado", done)
        order.cashDue() -> JourneyCaption("Por pagar · en Caja", waiting)
        state == OrderState.AWAITING_ACCEPTANCE ->
            JourneyCaption(
                if (journey ==
                    OrderJourney.HOME_DELIVERY
                ) {
                    "Por aceptar"
                } else {
                    "Por aceptar · no te cobramos nada"
                },
                waiting,
            )
        state == OrderState.PENDING_PAYMENT && order.payBy != null ->
            JourneyCaption(
                if (order.homeDelivery?.deposit.positive() !=
                    null
                ) {
                    "Aceptado · falta tu anticipo"
                } else {
                    "Aceptado · falta tu pago"
                },
                waiting,
            )
        state == OrderState.PENDING_PAYMENT -> JourneyCaption("Confirmando tu pago", waiting)
        order.rejectedItems().isNotEmpty() && state != OrderState.READY ->
            JourneyCaption("Preparando · un producto no se pudo", waiting)
        state == OrderState.ON_THE_WAY -> JourneyCaption("En camino", done)
        state == OrderState.READY && journey == OrderJourney.SCHOOL && order.beforePickupDay(today) ->
            JourneyCaption(journeySchedule(order)?.let { "Apartado · para el $it" } ?: "Apartado", done)
        state == OrderState.READY ->
            JourneyCaption(
                when (journey) {
                    OrderJourney.DRIVE_THRU -> "Listo · en la ventanilla"
                    OrderJourney.TABLE, OrderJourney.COURT_FOOD -> "Listo · te lo llevan"
                    OrderJourney.SEAT -> "Listo · va en camino"
                    OrderJourney.SCHOOL -> "Listo · en el comedor"
                    OrderJourney.PREORDER_PICKUP -> "Listo · pasa por él"
                    OrderJourney.HOME_DELIVERY -> "Listo · sale pronto"
                    else -> "Listo · en la barra"
                },
                done,
            )
        else ->
            when (journey) {
                OrderJourney.DRIVE_THRU ->
                    JourneyCaption(
                        if (order.arrivedAt != null) "Preparando · ya llegaste" else "Preparando · llegas en tu auto",
                        waiting,
                    )
                OrderJourney.TABLE -> JourneyCaption("Preparando · te lo llevan", waiting)
                OrderJourney.BAR -> JourneyCaption("Preparando · en la barra", waiting)
                OrderJourney.SEAT -> JourneyCaption("Preparando · a tu asiento", waiting)
                OrderJourney.COURT_FOOD -> JourneyCaption("Preparando · a tu ${venue.vocabulary.court}", waiting)
                OrderJourney.SCHOOL ->
                    JourneyCaption(journeySchedule(order)?.let { "Apartado · para el $it" } ?: "Apartado", done)
                OrderJourney.PREORDER_PICKUP, OrderJourney.HOME_DELIVERY ->
                    JourneyCaption(
                        if (order.depositPaid()) "Anticipo pagado · se está haciendo" else "Aceptado · se está haciendo",
                        done,
                    )
                else -> JourneyCaption("Preparando", waiting)
            }
    }
}

/** El título grande: lo que pasa con tu pedido, en una frase. */
internal fun journeyHeadline(
    order: OrderDetail,
    journey: OrderJourney,
    now: Instant = Instant.now(),
    zone: ZoneId = BusinessTime.zone,
): String {
    val state = order.summary.state
    val place = order.placePhrase(journey)
    val today = now.atZone(zone).toLocalDate()
    val rejected = order.rejectedItems()
    val word = order.itemWord(journey)
    return when {
        state == OrderState.CANCELED && order.depositDeadlineMissed() -> "Se pasó el plazo para pagar tu $word."
        state == OrderState.CANCELED && order.neverCharged() -> "El negocio no pudo aceptar tu $word."
        state == OrderState.CANCELED ->
            if (order.summary.canceledByRole in setOf("cocina", "cajero", "admin")) {
                if (journeyMoneyBack(order) !=
                    null
                ) {
                    "Cancelaron tu pedido. Te regresan todo."
                } else {
                    "Cancelaron tu pedido."
                }
            } else {
                "Tu pedido se canceló."
            }
        state == OrderState.NOT_PICKED_UP -> "Tu pedido no se recogió a tiempo."
        state == OrderState.EXPIRED -> "Este pedido expiró."
        state == OrderState.DELIVERED ->
            when (journey) {
                OrderJourney.TABLE, OrderJourney.SEAT, OrderJourney.COURT_FOOD -> "Ya te lo entregaron."
                OrderJourney.HOME_DELIVERY -> "¡Que lo disfruten!"
                else -> "Ya lo recogiste."
            }
        order.cashDue() -> "Paga en Caja y empiezan tu pedido."
        state == OrderState.AWAITING_ACCEPTANCE && journey == OrderJourney.HOME_DELIVERY ->
            "El negocio todavía no confirma tu $word."
        state == OrderState.AWAITING_ACCEPTANCE ->
            order.answerBy?.let { "El negocio te contesta ${deadlineLabel(it, zone, today)}." }
                ?: "El negocio revisa tu encargo."
        state == OrderState.PENDING_PAYMENT && order.payBy != null ->
            if (order.homeDelivery?.deposit.positive() != null) {
                "¡Aceptaron tu $word! Paga el anticipo para que lo empiecen."
            } else {
                "¡Aceptaron tu $word! Págalo para que lo empiecen."
            }
        state == OrderState.PENDING_PAYMENT -> "Estamos confirmando tu pago."
        rejected.isNotEmpty() && state != OrderState.READY ->
            if (rejected.size == 1) {
                val live = order.items.filter { it.rejection == null }
                val first = rejected.first()
                val what =
                    first.rejection
                        ?.reason
                        ?.takeIf { it.length <= REASON_IN_TITLE }
                        ?.lowercase() ?: "no se pudo"
                val rest = if (live.size == 1) "${live.first().productName} sigue." else "Lo demás sigue."
                "${first.productName} $what. $rest"
            } else {
                "${rejected.size} productos no se pudieron. Lo demás sigue."
            }
        state == OrderState.ON_THE_WAY ->
            if (journey == OrderJourney.HOME_DELIVERY) "Va en camino a tu casa." else "Va en camino a $place."
        state == OrderState.READY && order.beforePickupDay(today) && journeyWhen(order) != null ->
            when (journey) {
                OrderJourney.SCHOOL -> "Ya está apartado. Se recoge el ${journeyWhen(order)}."
                OrderJourney.HOME_DELIVERY -> "Ya está listo. Llega el ${journeyWhen(order)}."
                else -> "Ya está listo. Pasa por él el ${journeyWhen(order)}."
            }
        state == OrderState.READY ->
            when (journey) {
                OrderJourney.DRIVE_THRU -> "Ya está. Te lo dan en la ventanilla."
                // Listo no es entregado: el mesero aún tiene que recogerlo y llevarlo.
                OrderJourney.TABLE, OrderJourney.COURT_FOOD -> "Está listo. Te lo llevan a $place."
                OrderJourney.SEAT -> "Ya va a $place."
                OrderJourney.SCHOOL -> "Ya está. Recógelo en el comedor."
                OrderJourney.PREORDER_PICKUP -> "Ya está. Pasa por él."
                OrderJourney.HOME_DELIVERY -> "Está listo. Pronto sale."
                OrderJourney.BAR -> "Ya está. Te lo dan en la barra."
                else -> "Ya está. Pasa a la barra por él."
            }
        else ->
            when (journey) {
                OrderJourney.DRIVE_THRU ->
                    if (order.arrivedAt != null) "Ya saben que llegaste." else "Avísanos cuando entres al carril."
                OrderJourney.TABLE, OrderJourney.SEAT, OrderJourney.COURT_FOOD -> "Te lo llevan a $place."
                OrderJourney.BAR -> "Te lo entregan en la barra."
                OrderJourney.SCHOOL -> journeyWhen(order)?.let { "Se recoge el $it." } ?: "Ya está apartado."
                OrderJourney.PREORDER_PICKUP -> journeyWhen(order)?.let { "Pasa por él el $it." } ?: "Se está haciendo."
                OrderJourney.HOME_DELIVERY ->
                    journeyWhen(order)?.let { "Tu $word llega el $it." }
                        ?: "Se está haciendo."
                else -> "Tu $word se está preparando."
            }
    }
}

/** El texto bajo el título: qué hacer ahora. `null` cuando el título ya lo dice todo. */
internal fun journeyLead(
    order: OrderDetail,
    journey: OrderJourney,
    now: Instant = Instant.now(),
    zone: ZoneId = BusinessTime.zone,
): String? {
    val state = order.summary.state
    val folio = order.summary.folio
    val hasToken = !order.pickupToken.isNullOrBlank()
    val today = now.atZone(zone).toLocalDate()
    return when {
        state == OrderState.NOT_PICKED_UP -> "El negocio ya no lo puede entregar."
        state !in OPEN_STATES -> null
        order.cashDue() ->
            if (hasToken) {
                "Di tu folio o muestra este código. Cocina empieza en cuanto te cobren."
            } else {
                "Di tu folio $folio en Caja. Cocina empieza en cuanto te cobren."
            }
        state == OrderState.AWAITING_ACCEPTANCE && journey == OrderJourney.HOME_DELIVERY ->
            order.answerBy?.let {
                "Te contesta ${deadlineLabel(
                    it,
                    zone,
                    today,
                )}. Si no contesta a tiempo, se cancela solo y no pagas nada."
            } ?: "Antes de que lo acepte no se te cobra nada."
        state == OrderState.AWAITING_ACCEPTANCE && order.homeDelivery?.deposit.positive() != null ->
            "Si lo acepta, tienes 24 horas para pagar el anticipo. Si no contesta a tiempo, se cancela solo."
        state == OrderState.AWAITING_ACCEPTANCE ->
            if (order.summary.paymentMethod == PaymentMethod.STRIPE) {
                "Si lo acepta, te pedimos el pago con tarjeta. Antes no se te cobra nada."
            } else {
                "Si lo acepta, te avisamos aquí. Antes no se te cobra nada."
            }
        state == OrderState.PENDING_PAYMENT && order.payBy != null ->
            "Tienes hasta ${deadlineLabel(
                order.payBy,
                zone,
                today,
            )}. Empiezan tu ${order.itemWord(journey)} en cuanto entre tu pago."
        state == OrderState.PENDING_PAYMENT -> null
        order.rejectedItems().isNotEmpty() && state != OrderState.READY -> {
            val back = order.rejectedAmount()
            val to =
                when (order.summary.paymentMethod) {
                    PaymentMethod.BALANCE -> "a tu cartera Vaiinilla"
                    PaymentMethod.STRIPE -> "a tu tarjeta"
                    PaymentMethod.CASH -> null
                }
            if (to != null && back.signum() > 0 && order.paidBeforeRejection()) {
                "Te regresamos ${moneyLabel(back)} $to. No tienes que hacer nada."
            } else {
                "Lo que no se pudo ya no cuenta en tu total."
            }
        }
        state == OrderState.ON_THE_WAY ->
            when {
                order.homeDelivery?.hasAmountDue == true ->
                    "Ten listos ${moneyLabel(order.homeDelivery.dueOnArrival!!)} y dale el código a quien llegue."
                journeyDeliveryCode(order) != null -> "Dale el código a quien llegue."
                else -> null
            }
        state == OrderState.READY ->
            when (journey) {
                OrderJourney.COUNTER ->
                    if (hasToken) "Di tu folio o muestra este código. No tienes que formarte." else "Di tu folio $folio en la barra."
                OrderJourney.BAR -> "Di tu folio $folio en la barra."
                OrderJourney.DRIVE_THRU -> "Di tu folio $folio en la ventanilla."
                OrderJourney.TABLE -> "Tu mesero te lo lleva."
                OrderJourney.SEAT -> "Te lo llevan a tu lugar."
                OrderJourney.COURT_FOOD -> "Te lo llevan sin interrumpir el juego."
                OrderJourney.SCHOOL, OrderJourney.PREORDER_PICKUP ->
                    if (hasToken) "Muestra este código al recogerlo." else "Di tu folio $folio al recogerlo."
                else -> null
            }
        else ->
            when (journey) {
                OrderJourney.COUNTER -> journeyHint(order, journey, now, zone)
                OrderJourney.DRIVE_THRU ->
                    if (order.arrivedAt != null) {
                        "Lo terminan y te lo dan en la ventanilla."
                    } else {
                        "Así lo terminan para cuando llegues a la ventanilla."
                    }
                OrderJourney.TABLE -> if (order.summary.payAtEnd) "Pagas todo junto al final." else null
                OrderJourney.BAR -> "Te llaman por tu folio $folio. Si te lo piden, muestra tu código."
                OrderJourney.SEAT -> "No salgas de la sala. Te lo llevan a tu lugar."
                OrderJourney.COURT_FOOD -> "Lo dejan sin interrumpir el juego."
                OrderJourney.SCHOOL -> "Se recoge en el comedor con tu código."
                OrderJourney.HOME_DELIVERY -> "Si cambia la hora, te avisamos en ese momento y te decimos por qué."
                OrderJourney.PREORDER_PICKUP -> "Si algo cambia, te avisan en ese momento."
                else -> null
            }
    }
}

/** A dónde regresa el dinero de un pedido cancelado, en grande; `null` si el servidor no lo deja ver. */
internal fun journeyMoneyBack(order: OrderDetail): String? {
    if (order.summary.state != OrderState.CANCELED) return null
    val total = moneyLabel(order.summary.total)
    return when {
        order.summary.payAtEnd -> "No se te cobra nada"
        order.summary.paymentMethod == PaymentMethod.BALANCE -> "$total a tu saldo Vaiinilla"
        order.summary.paymentMethod == PaymentMethod.STRIPE &&
            cancellationMoneyNote(
                order,
            ) != null -> "$total a tu tarjeta"
        order.neverCharged() -> "No te cobramos nada"
        else -> null
    }
}

/** El renglón bajo [journeyMoneyBack]. */
internal fun journeyMoneyBackNote(order: OrderDetail): String? =
    cancellationMoneyNote(order)
        ?: when {
            order.summary.state == OrderState.CANCELED && order.depositDeadlineMissed() ->
                "El negocio lo aceptó, pero el anticipo no se pagó a tiempo. Tu tarjeta no tuvo ningún cargo."
            order.summary.state == OrderState.CANCELED &&
                order.neverCharged() &&
                order.summary.paymentMethod == PaymentMethod.CASH ->
                "Ibas a pagar al recibirlo. Como no se aceptó, no pagas nada."
            order.summary.state == OrderState.CANCELED && order.neverCharged() ->
                "El anticipo se paga cuando el negocio acepta. " +
                    "Como no se llegó a pagar, tu tarjeta no tuvo ningún cargo."
            else -> null
        }

/** El negocio aceptó el encargo, pero el cliente no pagó el anticipo a tiempo (R29): no fue un rechazo. */
private fun OrderDetail.depositDeadlineMissed(): Boolean = summary.cancelReasonCode == "anticipo_sin_pagar"

/**
 * Un encargo que se canceló sin cobrarse nada: con tarjeta, antes de pagarse (EQUIPO-53); en efectivo al recibir, el
 * que pasó por «Por aceptar» y nunca se cobró en la puerta (S04). Decía «Pagaste en efectivo» sin haber pagado.
 */
private fun OrderDetail.neverCharged(): Boolean =
    when (summary.paymentMethod) {
        PaymentMethod.STRIPE -> payment == null && (answerBy != null || payBy != null)
        PaymentMethod.CASH -> !summary.payAtEnd && answerBy != null && homeDelivery?.collectedAtDoorMethod == null
        PaymentMethod.BALANCE -> false
    }

/** Los 4 dígitos que el cliente dicta al recibir un pedido a domicilio, cuando ya va en camino. */
internal fun journeyDeliveryCode(order: OrderDetail): String? =
    order.homeDelivery?.code?.takeIf { it.isNotBlank() && order.summary.state == OrderState.ON_THE_WAY }
