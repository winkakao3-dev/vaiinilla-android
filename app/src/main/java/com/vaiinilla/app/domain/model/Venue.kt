package com.vaiinilla.app.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaiinilla.app.domain.hotel.HotelConfig

/**
 * Cómo se llama lo que se renta en el negocio abierto. En un boliche (`tipo: boliche`) las
 * canchas del servidor son pistas: mismo contrato (`tipo: cancha`), otro nombre y otro dibujo.
 * En un hotel (`tipo: hotel`) los espacios son habitaciones (`tipo: habitacion`).
 * Se actualiza con cada estado operativo; es estado de Compose para que las pantallas cambien solas.
 */
object Venue {
    const val BOWLING = "boliche"
    const val HOTEL = "hotel"

    var businessType: String? by mutableStateOf(null)
        private set

    /** Lo que el dueño configuró del hotel; null fuera de un hotel o si el servidor no lo manda. */
    var hotelConfig: HotelConfig? by mutableStateOf(null)
        private set

    val isBowling: Boolean get() = businessType == BOWLING

    val isHotel: Boolean get() = businessType == HOTEL

    fun update(
        type: String?,
        hotel: HotelConfig? = null,
    ) {
        if (type != businessType) businessType = type
        if (hotel != hotelConfig) hotelConfig = hotel
    }

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
