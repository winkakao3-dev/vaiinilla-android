package com.vaiinilla.app.domain.guardianship

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

/**
 * Fechas de la escuela en `YYYY-MM-DD`. El "hoy" lo manda el servidor en la zona del negocio
 * (`programacion.hoy`), así que aquí solo se cuenta a partir de él, sin usar la zona del teléfono.
 */
object SchoolCalendar {
    private val shortNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    private val longNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    fun date(iso: String): LocalDate? =
        try {
            LocalDate.parse(iso.take(10))
        } catch (_: DateTimeParseException) {
            null
        }

    /** De hoy a hoy + `daysAhead`, en orden. */
    fun dates(
        today: String,
        daysAhead: Int,
    ): List<String> {
        val start = date(today) ?: return emptyList()
        return (0..maxOf(0, daysAhead)).map { start.plusDays(it.toLong()).toString() }
    }

    fun weekdayShort(iso: String): String = date(iso)?.let { shortNames[it.dayOfWeek.value - 1] }.orEmpty()

    fun dayNumber(iso: String): String = date(iso)?.dayOfMonth?.toString().orEmpty()

    fun isWeekend(iso: String): Boolean =
        date(iso)?.dayOfWeek.let { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY }

    /** "Hoy", "Mañana" o "Jueves 24". */
    fun longLabel(
        iso: String,
        today: String,
    ): String {
        if (iso == today) return "Hoy"
        val day = date(iso) ?: return iso
        if (date(today)?.plusDays(1) == day) return "Mañana"
        return "${longNames[day.dayOfWeek.value - 1]} ${day.dayOfMonth}"
    }

    /** "08:30" → minutos del día; inválido, `null`. */
    fun minutes(hour: String?): Int? =
        hour?.let {
            try {
                LocalTime.parse(it.take(5)).let { time -> time.hour * 60 + time.minute }
            } catch (_: DateTimeParseException) {
                null
            }
        }
}

/** Las reglas puras del planificador de la semana. */
object WeekPlannerRules {
    /**
     * Los días que se pueden apartar: el fin de semana solo si la escuela le puso menú; hoy, hasta la
     * hora de corte. `nowMinutes` es la hora actual en minutos (del teléfono, como en iOS: el servidor
     * vuelve a revisar el corte con la hora de la escuela).
     */
    fun dates(
        calendar: MenuCalendar,
        nowMinutes: Int,
    ): List<String> {
        val schedule = calendar.schedule
        val cutoff = SchoolCalendar.minutes(schedule.orderCutoff)
        return SchoolCalendar
            .dates(schedule.today, schedule.daysAhead)
            .filter { !SchoolCalendar.isWeekend(it) || calendar.menu(it) != null }
            .filter { it != schedule.today || cutoff == null || nowMinutes < cutoff }
    }

    /** El primer día abierto, o el primero si todos están cerrados. */
    fun firstSelectable(
        dates: List<String>,
        calendar: MenuCalendar,
    ): String? = dates.firstOrNull { calendar.closure(it) == null } ?: dates.firstOrNull()

    /** Lo que se puede apartar ese día: su menú si tiene; si no, todo lo disponible. Un día cerrado, nada. */
    fun productIds(
        calendar: MenuCalendar,
        date: String,
        availableIds: List<Int>,
        allIds: List<Int>,
    ): List<Int> {
        if (calendar.closure(date) != null) return emptyList()
        val menu = calendar.menu(date) ?: return availableIds
        // Un producto del menú del día se aparta aunque hoy esté marcado como no disponible.
        return allIds.filter { it in menu }
    }
}
