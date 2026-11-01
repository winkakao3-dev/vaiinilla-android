package com.vaiinilla.app.ui.notices

import com.vaiinilla.app.core.notifications.NoticeExplanation
import com.vaiinilla.app.core.notifications.NoticeLookup
import com.vaiinilla.app.core.notifications.ReceivedNotice
import com.vaiinilla.app.domain.time.BusinessTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Orden y agrupado del panel «Avisos recibidos» (AVISOS-02), igual que `ReceivedNoticesView` de iOS. */
internal object NoticePanel {
    private val SPANISH = Locale.forLanguageTag("es-MX")
    private val HOUR = DateTimeFormatter.ofPattern("HH:mm", SPANISH)
    private val DAY = DateTimeFormatter.ofPattern("d 'de' MMMM", SPANISH)
    private val DAY_YEAR = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", SPANISH)

    fun explanation(notice: ReceivedNotice): NoticeExplanation =
        NoticeExplanation.explain(notice.payload, notice.isRemote)

    fun isPending(notice: ReceivedNotice): Boolean = explanation(notice).isPending(notice.lookup)

    /**
     * El negocio del aviso: el del pedido leído, el que mande el push (`establecimientoNombre`, pedido a Saúl) o el
     * nombre que tiene en los modos de la cuenta.
     */
    fun business(notice: ReceivedNotice): String? {
        (notice.lookup as? NoticeLookup.Found)?.let { return it.order.establishmentName }
        notice.payload["establecimientoNombre"]?.takeIf { it.isNotBlank() }?.let { return it }
        val id = explanation(notice).establishmentId ?: return null
        return notice.phone
            ?.memberships
            ?.firstOrNull { it.establishmentId == id }
            ?.establishmentName
    }

    /** Quién hizo el pedido: el del pedido leído o el `clienteNombre` del push. */
    fun customer(notice: ReceivedNotice): String? =
        (notice.lookup as? NoticeLookup.Found)?.order?.customerName
            ?: notice.payload["clienteNombre"]?.takeIf { it.isNotBlank() }

    /** Pendientes primero y, dentro de cada grupo, el más nuevo arriba. */
    fun ordered(notices: List<ReceivedNotice>): List<ReceivedNotice> =
        notices.sortedWith(
            compareByDescending<ReceivedNotice> { isPending(it) }.thenByDescending { it.receivedAtMillis },
        )

    /** Un chip por negocio con cuántos avisos tiene, de más a menos. Los que no se sabe de qué negocio son, no. */
    fun chips(notices: List<ReceivedNotice>): List<Pair<String, Int>> =
        notices
            .mapNotNull { business(it) }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

    fun filter(
        notices: List<ReceivedNotice>,
        business: String?,
    ): List<ReceivedNotice> = if (business == null) notices else notices.filter { business(it) == business }

    /** «Hoy, 14:20», «Ayer, 09:05» o «28 de octubre, 20:21», en la hora del negocio. */
    fun date(
        millis: Long,
        zone: ZoneId = BusinessTime.zone,
        today: LocalDate = LocalDate.now(zone),
    ): String {
        val moment = Instant.ofEpochMilli(millis).atZone(zone)
        val day = moment.toLocalDate()
        val prefix =
            when {
                day == today -> "Hoy"
                day == today.minusDays(1) -> "Ayer"
                day.year == today.year -> DAY.format(moment)
                else -> DAY_YEAR.format(moment)
            }
        return "$prefix, ${HOUR.format(moment)}"
    }
}
