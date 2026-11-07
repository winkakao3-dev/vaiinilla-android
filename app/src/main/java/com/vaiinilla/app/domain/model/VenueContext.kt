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
    ;

    companion object {
        fun fromWire(type: String?): VenueKind = entries.firstOrNull { it.wire != null && it.wire == type } ?: STANDARD
    }
}

/** Cómo se llama lo que se renta: en un boliche las canchas del servidor (`tipo: cancha`) son pistas. */
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
)

/**
 * Una sola respuesta a «qué tipo de negocio es y qué cambia por eso». Se inyecta (ver [com.vaiinilla.app.core.venue.VenueContextStore])
 * en lugar de leerse de un estado global.
 */
data class VenueContext(
    val kind: VenueKind = VenueKind.STANDARD,
) {
    val isBowling: Boolean get() = kind == VenueKind.BOWLING

    val vocabulary: VenueVocabulary =
        when (kind) {
            VenueKind.BOWLING ->
                VenueVocabulary(
                    "pista",
                    "pistas",
                    "Boliche",
                    "Madera sintética",
                    null,
                    "La pista por hora se renta desde Mesas y espacios. Juegos por persona y renta de zapatos sí van aquí como producto.",
                )
            VenueKind.STANDARD ->
                VenueVocabulary(
                    "cancha",
                    "canchas",
                    "Pádel",
                    "Pasto sintético",
                    "Cancha o pista",
                    "Este negocio renta canchas desde Mesas y espacios. No crees la renta como producto.",
                )
        }

    val capabilities: VenueCapabilities =
        VenueCapabilities(
            asksPlayerCount = isBowling,
            drawsLanes = isBowling,
            rentsByTheMoment = isBowling,
            flagsRentalWordsInMenu = !isBowling,
        )

    /** Aviso de los últimos minutos de una renta. */
    fun lastMinutesNotice(minutes: Int): String =
        if (isBowling) "Quedan $minutes min de pista." else "Quedan $minutes min: los reflectores ya bajaron."

    companion object {
        val STANDARD = VenueContext()

        fun of(type: String?): VenueContext = VenueContext(VenueKind.fromWire(type))
    }
}
