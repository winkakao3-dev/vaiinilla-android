package com.vaiinilla.app.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * La hora que ve la gente es la del negocio, no la del teléfono (EQUIPO-23): la cocina, la caja y el papá ven
 * la misma hora del mismo pedido aunque un teléfono esté en otra zona. Solo cambia cómo se muestra; las fechas
 * se guardan y se mandan al servidor igual que antes.
 *
 * El servidor todavía no manda la zona del establecimiento (solo `zona_horaria` en el día de reservas), así que
 * aquí vive el único valor por defecto.
 */
object BusinessTime {
    val DEFAULT_ZONE: ZoneId = ZoneId.of("America/Chihuahua")

    val zone: ZoneId get() = DEFAULT_ZONE

    /** La zona que mandó el servidor si se puede leer; si no, la del negocio. */
    fun zoneOr(serverZone: String?): ZoneId = serverZone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: zone

    fun at(
        instant: Instant,
        zone: ZoneId = this.zone,
    ): ZonedDateTime = instant.atZone(zone)

    fun format(
        instant: Instant,
        formatter: DateTimeFormatter,
        zone: ZoneId = this.zone,
    ): String = formatter.format(at(instant, zone))

    fun today(zone: ZoneId = this.zone): LocalDate = LocalDate.now(zone)
}
