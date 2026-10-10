package com.vaiinilla.app.domain.model

data class PublicEstablishment(
    val id: String,
    val name: String,
    val slug: String,
    val clientIdLabel: String,
    val clientIdRequired: Boolean,
    val localVideos: List<PublicVideo> = emptyList(),
)

/** Ubicación aproximada del dispositivo para ordenar el descubrimiento por cercanía. */
data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
)

data class PublicVideo(
    val url: String,
    val title: String,
)

data class PublicSpace(
    val id: Int,
    val name: String,
    val type: String,
    /** Código del QR escaneado: con él el cliente se une a la mesa compartida. */
    val qrToken: String? = null,
)

data class GuestVenueContext(
    val establishment: PublicEstablishment,
    val space: PublicSpace? = null,
)

data class SpaceResolveResult(
    val establishment: PublicEstablishment,
    val space: PublicSpace?,
)
