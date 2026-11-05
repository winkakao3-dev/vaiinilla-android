package com.vaiinilla.app.domain.model

data class PublicEstablishment(
    val id: String,
    val name: String,
    val slug: String,
    val clientIdLabel: String,
    val clientIdRequired: Boolean,
    val localVideos: List<PublicVideo> = emptyList(),
    /** Foto del lugar (fachada o salón); `null` si el negocio no subió una. */
    val photoUrl: String? = null,
    val logoUrl: String? = null,
    /** El enlace de WhatsApp que el negocio puso en sus redes, para quien pregunta antes de pedir (CASA-11). */
    val whatsappUrl: String? = null,
    /** El negocio deja pagar al final en una mesa; el invitado aún sin sesión no tiene el estado operativo. */
    val allowsPayAtEnd: Boolean = false,
    /** «Te lo llevamos», «Pasa por él» o ambas; el invitado sin sesión lo lee de aquí. */
    val spaceDelivery: SpaceDelivery = SpaceDelivery.TABLE,
    /** Tiene el asistente de pedidos para clientes («Pregúntame qué pedir»); el dueño o Vaiinilla lo apagan. */
    val assistantForCustomers: Boolean = true,
    /** `tipo` del negocio («boliche», «cafeteria»…); en un boliche las canchas se llaman pistas. */
    val businessType: String? = null,
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
