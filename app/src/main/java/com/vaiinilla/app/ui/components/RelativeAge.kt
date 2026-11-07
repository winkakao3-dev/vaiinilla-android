package com.vaiinilla.app.ui.components

import com.vaiinilla.app.ui.notices.NoticePanel
import java.time.Duration
import java.time.Instant

/**
 * «hace 5 min», «ahora»: cuánto lleva algo, dicho igual en Caja, Mesero, Cocina y el panel del dueño. Todo parte de
 * milisegundos ya transcurridos para que se pueda probar sin reloj.
 */
object RelativeAge {
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L

    /** Milisegundos desde [iso] hasta [now]; null si el texto no es una fecha. */
    fun elapsedMs(
        iso: String,
        now: Instant = Instant.now(),
    ): Long? = runCatching { Duration.between(Instant.parse(iso), now).toMillis() }.getOrNull()

    /** «ahora», «5 min» o «2 h»: sin «hace», para frases como «Llamando · 5 min». */
    fun short(ms: Long): String =
        when {
            ms < MINUTE_MS -> "ahora"
            ms < HOUR_MS -> "${ms / MINUTE_MS} min"
            else -> "${ms / HOUR_MS} h"
        }

    /** [short] de lo ocurrido en [iso]; sin fecha legible dice «ahora». */
    fun short(
        iso: String,
        now: Instant = Instant.now(),
    ): String = elapsedMs(iso, now)?.let { short(it) } ?: "ahora"

    /** «ahora» o «hace 5 min» para frases que ya no llevan otro verbo. */
    fun shortAgo(
        iso: String,
        now: Instant = Instant.now(),
    ): String = short(iso, now).let { if (it == "ahora") it else "hace $it" }

    /**
     * «ahora», «hace 5 min», «hace 2 h», «hace 3 d» y, pasada una semana, la fecha corta (nadie lee «hace 340 d»).
     * [nowMs] solo se usa para sacar esa fecha.
     */
    fun ago(
        ms: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): String =
        when {
            ms < MINUTE_MS -> "ahora"
            ms < HOUR_MS -> "hace ${ms / MINUTE_MS} min"
            ms < DAY_MS -> "hace ${ms / HOUR_MS} h"
            ms < 7 * DAY_MS -> "hace ${ms / DAY_MS} d"
            else -> NoticePanel.shortDate(nowMs - ms)
        }
}
