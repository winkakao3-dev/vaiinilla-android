package com.vaiinilla.app.ui.screens

import androidx.annotation.DrawableRes
import com.vaiinilla.app.R
import kotlin.random.Random

/**
 * Catálogo local de mascotas del ticket.
 *
 * La selección vive por completo en la app: el backend no asigna ni conoce la
 * mascota. Cada pedido recibe una mascota estable (mismo pedido => misma
 * mascota) derivada de su id, con probabilidad configurable por [Mascot.weight].
 */
object TicketMascotCatalog {
    data class Mascot(
        @DrawableRes val drawable: Int,
        val weight: Int = 1,
    )

    val mascots: List<Mascot> =
        listOf(
            Mascot(R.drawable.mascot_laptop),
            Mascot(R.drawable.mascot_play),
            Mascot(R.drawable.mascot_workshop),
            Mascot(R.drawable.mascot_karate),
            Mascot(R.drawable.mascot_travel),
        )

    init {
        require(mascots.all { it.weight > 0 }) { "Mascot weights must be positive" }
    }

    fun drawableFor(orderId: String): Int {
        val totalWeight = mascots.sumOf(Mascot::weight)
        var ticket = Random(orderId.hashCode()).nextInt(totalWeight)
        mascots.forEach { mascot ->
            ticket -= mascot.weight
            if (ticket < 0) return mascot.drawable
        }
        return mascots.last().drawable
    }
}
