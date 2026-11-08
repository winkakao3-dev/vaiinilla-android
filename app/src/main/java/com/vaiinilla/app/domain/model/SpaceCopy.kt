package com.vaiinilla.app.domain.model

/**
 * Vocabulario según el tipo de espacio del pedido (`mesa`, `barra`, `cancha`, `drive_thru`, `asiento`).
 * Sin tipo o con `mesa` se conserva el texto de siempre, salvo en un hotel ([VenueContext.isHotel]): ahí la mesa del
 * servidor es una habitación y la app dice «habitación».
 */
object SpaceCopy {
    /** En un hotel las habitaciones son espacios `mesa` (o sin tipo): el servidor no tiene un tipo propio. */
    fun isRoomType(type: String?): Boolean = type == null || type == "mesa"

    fun isRoom(
        venue: VenueContext,
        type: String?,
    ): Boolean = venue.capabilities.tablesAreRooms && isRoomType(type)

    /**
     * ¿Este espacio es una habitación? Con el dato del servidor (`es_habitacion`) manda: una mesa del restaurante del
     * hotel no lo es. Sin el dato, en un hotel toda mesa lo es, como antes. Sin espacio, no.
     */
    fun isRoom(
        venue: VenueContext,
        space: PublicSpace?,
    ): Boolean = space != null && isRoom(venue, space.type) && space.isRoom != false

    /**
     * ¿El pedido va a una habitación? Un pedido ligado a una estancia ([OrderDetail.stayId]) lo es. Los demás, con un
     * servidor que no manda ese dato, cuentan como habitación en un hotel salvo que el servidor haya dicho que ese
     * espacio es una mesa del restaurante ([com.vaiinilla.app.core.venue.VenueContextStore.markSpace]).
     */
    fun isRoomOrder(
        venue: VenueContext,
        order: OrderDetail,
    ): Boolean {
        if (order.stayId != null) return true
        val space = order.summary.space ?: return false
        return venue.capabilities.tablesAreRooms && isRoomType(space.type) && !venue.isHotelTable(space.id)
    }

    /**
     * El espacio como lo dice el personal en una etiqueta corta: «Hab. 304» para una habitación (el servidor la
     * guarda como «Habitación 304»), el nombre tal cual para los demás.
     */
    fun short(
        venue: VenueContext,
        type: String?,
        name: String,
    ): String =
        if (isRoom(venue, type)) {
            name.trim().replace(Regex("^Habitaci[óo]n\\s*", RegexOption.IGNORE_CASE), "Hab. ")
        } else {
            name
        }

    /** Nombre corto del tipo para el personal: "Mesa", "Cancha"... */
    fun kindLabel(
        venue: VenueContext,
        type: String?,
    ): String =
        when (type) {
            "cancha" -> venue.vocabulary.courtTitle
            "barra" -> "Barra"
            "drive_thru" -> "Carril"
            "asiento" -> "Asiento"
            else -> if (isRoom(venue, type)) venue.vocabulary.roomTitle else "Mesa"
        }

    /** El nombre que le puso el negocio con su artículo: «la Mesa 4», «el Asiento 3». */
    fun named(
        type: String?,
        name: String,
    ): String = (if (type == "asiento" || type == "drive_thru") "el " else "la ") + name

    /** Cómo nombra el cliente su lugar: "tu mesa", "tu cancha"... */
    fun yourPlace(
        venue: VenueContext,
        type: String?,
    ): String =
        when (type) {
            "cancha" -> "tu ${venue.vocabulary.court}"
            "barra" -> "la barra"
            "drive_thru" -> "tu auto"
            "asiento" -> "tu asiento"
            null, "mesa" -> if (isRoom(venue, type)) "tu ${venue.vocabulary.room}" else "tu mesa"
            else -> "tu espacio"
        }

    /** Título de la opción de destino en el carrito (antes siempre "Comer aquí"). */
    fun destinationTitle(
        venue: VenueContext,
        type: String?,
    ): String =
        when (type) {
            "cancha" -> "En la ${venue.vocabulary.court}"
            "barra" -> "En la barra"
            "drive_thru" -> "En tu auto"
            "asiento" -> "En tu asiento"
            else -> if (isRoom(venue, type)) "A tu ${venue.vocabulary.room}" else "Comer aquí"
        }

    /** Carrito en efectivo con destino a un espacio. En la barra se entrega ahí; no "se lleva". */
    fun payAtCashierHint(
        venue: VenueContext,
        type: String?,
    ): String =
        if (type == "barra") {
            "Pagas en caja y te lo entregamos en la barra"
        } else if (isRoom(venue, type)) {
            "Se carga a tu ${venue.vocabulary.room} y te lo llevamos a tu puerta"
        } else {
            "Pagas en caja y te lo llevamos a ${yourPlace(venue, type)}"
        }

    /** Aviso de pedido listo con destino a un espacio. */
    fun readyInSpace(
        venue: VenueContext,
        type: String?,
    ): String =
        if (type == "barra") {
            "Tu pedido está listo. Te lo entregamos en la barra."
        } else {
            "Tu pedido está listo. En un momento te lo llevan a ${yourPlace(venue, type)}."
        }

    /** Aviso de pedido entregado en un espacio: el mesero ya lo dejó en el lugar del cliente. */
    fun deliveredInSpace(
        venue: VenueContext,
        type: String?,
    ): String =
        if (type == "barra") {
            "Pedido entregado. Buen provecho."
        } else {
            "Ya está en ${yourPlace(venue, type)}. Buen provecho."
        }

    /** Subtítulo de la opción de destino en el carrito. Si el negocio solo entrega en barra, no se lleva al lugar. */
    fun destinationSubtitle(
        venue: VenueContext,
        type: String?,
        delivery: SpaceDelivery = SpaceDelivery.TABLE,
    ): String =
        when {
            delivery == SpaceDelivery.BAR -> "Pides desde tu lugar y pasas por él a la barra"
            type == null -> "Te lo llevamos a tu espacio"
            else -> "Te lo llevamos a ${yourPlace(venue, type)}"
        }

    fun pickPrompt(
        venue: VenueContext,
        type: String?,
    ): String =
        when (type) {
            "cancha" -> "Selecciona tu ${venue.vocabulary.court}"
            "barra" -> "Selecciona tu lugar en la barra"
            "drive_thru" -> "Selecciona tu carril"
            "asiento" -> "Selecciona tu asiento"
            else -> if (isRoom(venue, type)) "Selecciona tu ${venue.vocabulary.room}" else "Selecciona tu mesa"
        }

    /**
     * Para cocina y caja: adónde va el pedido, con el nombre del espacio ("Comer aquí · Mesa 1"). Un «Pasa por
     * él» (`recoge_en_barra`) se entrega en la barra aunque se pidió desde el lugar:
     * «Pasa por él a la barra · Pista 1». En una habitación el personal lee «A la habitación · Hab. 304».
     */
    fun staffDestination(
        venue: VenueContext,
        destination: OrderDestination,
        space: OrderSpace?,
        pickUpAtBar: Boolean = false,
    ): String {
        when (destination) {
            OrderDestination.TAKE_AWAY -> return "Para llevar"
            OrderDestination.HOME_DELIVERY -> return "A domicilio"
            OrderDestination.IN_SPACE -> Unit
        }
        val title =
            when {
                pickUpAtBar -> "Pasa por él a la barra"
                isRoom(venue, space?.type) -> "A la ${venue.vocabulary.room}"
                else -> destinationTitle(venue, space?.type)
            }
        val name = space?.name?.takeIf { it.isNotBlank() }?.let { short(venue, space.type, it) } ?: return title
        return "$title · $name"
    }
}
