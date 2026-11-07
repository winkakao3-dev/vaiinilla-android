package com.vaiinilla.app.domain.model

/**
 * Tipo de negocio que se está mirando. El servidor manda `tipo` en el negocio; lo que la app no distingue
 * (restaurante, escuela, club, pastelería…) cae en [STANDARD] y conserva el texto y el comportamiento de siempre.
 */
enum class VenueKind(
    val wire: String?,
) {
    STANDARD(null),
    BOWLING("boliche"),
    HOTEL("hotel"),
    ;

    companion object {
        fun fromWire(type: String?): VenueKind = entries.firstOrNull { it.wire != null && it.wire == type } ?: STANDARD
    }
}

/**
 * Cómo se llama lo que se renta: en un boliche las canchas del servidor (`tipo: cancha`) son pistas y en un hotel
 * las habitaciones son espacios `mesa` que la app llama «habitación».
 */
data class VenueVocabulary(
    val court: String,
    val courts: String,
    val courtExampleSport: String,
    val courtExampleSurface: String,
    /** Etiqueta del tipo «cancha» al dar de alta un espacio; null usa la del tipo. */
    val courtTypeChip: String?,
    /** Aviso del admin al crear un producto que no es una renta (el negocio renta ese espacio desde Mesas). */
    val menuRentalAdvice: String,
) {
    val courtTitle: String get() = court.replaceFirstChar(Char::titlecase)
    val courtsTitle: String get() = courts.replaceFirstChar(Char::titlecase)

    val room: String get() = "habitación"
    val roomTitle: String get() = "Habitación"
    val rooms: String get() = "habitaciones"
    val roomsTitle: String get() = "Habitaciones"
}

/** Lo que el negocio hace distinto según su tipo, para no repetir `if (esBoliche)` en cada pantalla. */
data class VenueCapabilities(
    /** Se pregunta cuántos juegan antes de apartar y se topa con la capacidad de la pista. */
    val asksPlayerCount: Boolean,
    /** Lo rentado se dibuja como pista de bolos y no como cancha de pádel. */
    val drawsLanes: Boolean,
    /** «Rentar ahora»: apartar de una vez el hueco que empieza ya. */
    val rentsByTheMoment: Boolean,
    /** Una renta de cancha entra como producto con solo llamarlo «renta»: aquí se avisa que se cobra desde Mesas. */
    val flagsRentalWordsInMenu: Boolean,
    /** Las mesas del servidor son habitaciones (salvo las que el servidor dijo que son del restaurante). */
    val tablesAreRooms: Boolean,
    /** «Llamar al mesero» es llamar a recepción. */
    val waiterIsReception: Boolean,
    /** La barra del carrito dice «Ver pedido» y no «Pedir ya»: el pedido se carga a la habitación. */
    val cartBarViewsOrder: Boolean,
    /** El personal trabaja habitaciones: tablero de room service en Cocina, habitaciones en Mesero y recepción en Caja. */
    val staffWorksRooms: Boolean,
)

/**
 * Una sola respuesta a «qué tipo de negocio es y qué cambia por eso». Se inyecta
 * (ver [com.vaiinilla.app.core.venue.VenueContextStore]) en lugar de leerse de un estado global.
 *
 * [hotelTables] son los espacios que el servidor dijo que NO son habitación en un hotel (`es_habitacion: false`):
 * las mesas de su restaurante. El pedido no trae ese dato, así que se recuerda por id del espacio.
 */
data class VenueContext(
    val kind: VenueKind = VenueKind.STANDARD,
    val hotelTables: Set<Int> = emptySet(),
) {
    val isBowling: Boolean get() = kind == VenueKind.BOWLING

    val isHotel: Boolean get() = kind == VenueKind.HOTEL

    fun isHotelTable(id: Int?): Boolean = id != null && id in hotelTables

    val vocabulary: VenueVocabulary =
        when (kind) {
            VenueKind.BOWLING ->
                VenueVocabulary(
                    court = "pista",
                    courts = "pistas",
                    courtExampleSport = "Boliche",
                    courtExampleSurface = "Madera sintética",
                    courtTypeChip = null,
                    menuRentalAdvice =
                        "La pista por hora se renta desde Mesas y espacios. " +
                            "Juegos por persona y renta de zapatos sí van aquí como producto.",
                )
            VenueKind.STANDARD, VenueKind.HOTEL ->
                VenueVocabulary(
                    court = "cancha",
                    courts = "canchas",
                    courtExampleSport = "Pádel",
                    courtExampleSurface = "Pasto sintético",
                    courtTypeChip = "Cancha o pista",
                    menuRentalAdvice = "Este negocio renta canchas desde Mesas y espacios. No crees la renta como producto.",
                )
        }

    val capabilities: VenueCapabilities =
        VenueCapabilities(
            asksPlayerCount = isBowling,
            drawsLanes = isBowling,
            rentsByTheMoment = isBowling,
            flagsRentalWordsInMenu = !isBowling,
            tablesAreRooms = isHotel,
            waiterIsReception = isHotel,
            cartBarViewsOrder = isHotel,
            staffWorksRooms = isHotel,
        )

    /** Aviso de los últimos minutos de una renta. */
    fun lastMinutesNotice(minutes: Int): String =
        if (isBowling) "Quedan $minutes min de pista." else "Quedan $minutes min: los reflectores ya bajaron."

    companion object {
        val STANDARD = VenueContext()

        fun of(type: String?): VenueContext = VenueContext(VenueKind.fromWire(type))
    }
}
