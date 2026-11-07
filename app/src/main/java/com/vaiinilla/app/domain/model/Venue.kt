package com.vaiinilla.app.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cómo se llama lo que se renta en el negocio abierto. En un boliche (`tipo: boliche`) las
 * canchas del servidor son pistas: mismo contrato (`tipo: cancha`), otro nombre y otro dibujo.
 * En un hotel (`tipo: hotel`) las habitaciones son espacios `mesa` que la app llama «Habitación».
 * Se actualiza con cada estado operativo; es estado de Compose para que las pantallas cambien solas.
 */
object Venue {
    const val BOWLING = "boliche"
    const val HOTEL = "hotel"

    var businessType: String? by mutableStateOf(null)
        private set

    val isBowling: Boolean get() = businessType == BOWLING

    val isHotel: Boolean get() = businessType == HOTEL

    fun update(type: String?) {
        if (type != businessType) businessType = type
    }

    /**
     * Espacios que el servidor dijo que NO son habitación en un hotel (`es_habitacion: false`): las mesas de su
     * restaurante. El pedido no trae ese dato, así que se recuerda por id del espacio.
     */
    private var hotelTables: Set<Int> by mutableStateOf(emptySet())

    /** Anota lo que el servidor dijo de un espacio: `false` es una mesa del hotel; `true` o `null`, no lo es. */
    fun markSpace(
        id: Int,
        isRoom: Boolean?,
    ) {
        val isTable = isRoom == false
        if (isTable != (id in hotelTables)) hotelTables = if (isTable) hotelTables + id else hotelTables - id
    }

    fun isHotelTable(id: Int?): Boolean = id != null && id in hotelTables

    /** «cancha» o «pista». */
    val court: String get() = if (isBowling) "pista" else "cancha"

    /** «Cancha» o «Pista». */
    val courtTitle: String get() = if (isBowling) "Pista" else "Cancha"

    val courts: String get() = if (isBowling) "pistas" else "canchas"

    val courtsTitle: String get() = if (isBowling) "Pistas" else "Canchas"

    /** «habitación»: lo que se renta en un hotel. */
    val room: String get() = "habitación"

    val roomTitle: String get() = "Habitación"

    val rooms: String get() = "habitaciones"

    val roomsTitle: String get() = "Habitaciones"
}
