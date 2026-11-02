package com.vaiinilla.app.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * El último lugar donde se pidió desde este teléfono, para «Pediste aquí» en Dónde pedir. La lista de pedidos del
 * servidor no dice en qué negocio se hizo cada uno, así que se guarda al confirmar el pedido.
 */
data class LastOrderedPlace(
    val establishment: PublicEstablishment,
    /** El primer platillo del pedido y cuántos más llevaba. */
    val firstItemName: String,
    val otherItemsCount: Int,
    val orderedAt: Instant,
) {
    /** «Ayer · Pastel Michi con fresas y 2 más». */
    fun summary(
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val items = if (otherItemsCount > 0) "$firstItemName y $otherItemsCount más" else firstItemName
        return "${dayLabel(orderedAt, now, zone)} · $items"
    }

    companion object {
        /** `null` si el pedido no trae platillos (una renta de cancha se cobra como pedido sin artículos). */
        fun from(
            establishment: PublicEstablishment,
            order: OrderDetail,
            fallbackTime: Instant = Instant.now(),
        ): LastOrderedPlace? {
            val items = order.items.filter { it.rejection == null }
            val first = items.firstOrNull() ?: return null
            return LastOrderedPlace(
                establishment = establishment,
                firstItemName = first.productName,
                otherItemsCount = items.size - 1,
                orderedAt = runCatching { Instant.parse(order.summary.createdAt) }.getOrDefault(fallbackTime),
            )
        }

        fun dayLabel(
            date: Instant,
            now: Instant,
            zone: ZoneId,
        ): String {
            val day = date.atZone(zone).toLocalDate()
            val days = ChronoUnit.DAYS.between(day, now.atZone(zone).toLocalDate())
            return when {
                days <= 0 -> "Hoy"
                days == 1L -> "Ayer"
                days <= 6 -> "El ${WEEKDAYS.getValue(day.dayOfWeek)}"
                else -> "El ${day.dayOfMonth} ${MONTHS[day.monthValue - 1]}"
            }
        }

        // A mano y no con el formateador del sistema: según la versión de Android, «nov» sale con o sin punto.
        private val WEEKDAYS =
            mapOf(
                DayOfWeek.MONDAY to "lunes",
                DayOfWeek.TUESDAY to "martes",
                DayOfWeek.WEDNESDAY to "miércoles",
                DayOfWeek.THURSDAY to "jueves",
                DayOfWeek.FRIDAY to "viernes",
                DayOfWeek.SATURDAY to "sábado",
                DayOfWeek.SUNDAY to "domingo",
            )
        private val MONTHS =
            listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    }
}
