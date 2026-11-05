package com.vaiinilla.app.domain.model

/**
 * Cómo se entrega un pedido en mesa o lugar (`entrega_en_espacio`).
 * TABLE: «Te lo llevamos», el mesero lo lleva. BAR: «Pasa por él», el cliente recoge en la barra
 * y Caja lo entrega. BOTH: el cliente elige al pagar; en Caja lo marca el cajero.
 */
enum class SpaceDelivery(
    val wire: String,
) {
    TABLE("mesa"),
    BAR("barra"),
    BOTH("ambas"),
    ;

    /** Si el cliente tiene que elegir entre las dos. */
    val asksCustomer: Boolean get() = this == BOTH

    /** Lo que se recoge en barra cuando el cliente no elige. */
    fun pickUpAtBar(chosen: Boolean): Boolean =
        when (this) {
            TABLE -> false
            BAR -> true
            BOTH -> chosen
        }

    companion object {
        /** Un valor desconocido o ausente (servidor anterior) es lo de siempre: lo lleva el mesero. */
        fun fromWire(value: String?): SpaceDelivery = entries.firstOrNull { it.wire == value } ?: TABLE
    }
}
