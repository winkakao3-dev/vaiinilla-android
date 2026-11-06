package com.vaiinilla.app.domain.model

/**
 * Vocabulario según el tipo de espacio del pedido (`mesa`, `barra`, `cancha`, `drive_thru`, `asiento`).
 * Sin tipo o con `mesa` se conserva el texto de siempre, salvo en un hotel ([Venue.isHotel]): ahí la mesa del
 * servidor es una habitación y la app dice «habitación».
 */
object SpaceCopy {
    /** En un hotel las habitaciones son espacios `mesa` (o sin tipo): el servidor no tiene un tipo propio. */
    fun isRoomType(type: String?): Boolean = type == null || type == "mesa"

    fun isRoom(type: String?): Boolean = Venue.isHotel && isRoomType(type)

    /**
     * ¿Este espacio es una habitación? Con el dato del servidor (`es_habitacion`) manda: una mesa del restaurante del
     * hotel no lo es. Sin el dato, en un hotel toda mesa lo es, como antes. Sin espacio, no.
     */
    fun isRoom(space: PublicSpace?): Boolean = space != null && isRoom(space.type) && space.isRoom != false

    /** ¿El pedido va a una habitación? `estancia_id` solo lo tiene un pedido que se carga a una estancia. */
    fun isRoomOrder(order: OrderDetail): Boolean =
        Venue.isHotel && order.summary.space?.let { isRoomType(it.type) } == true && order.stayId != null

    /**
     * El espacio como lo dice el personal en una etiqueta corta: «Hab. 304» para una habitación (el servidor la
     * guarda como «Habitación 304»), el nombre tal cual para los demás.
     */
    fun short(
        type: String?,
        name: String,
    ): String =
        if (isRoom(type)) {
            name.trim().replace(Regex("^Habitaci[óo]n\\s*", RegexOption.IGNORE_CASE), "Hab. ")
        } else {
            name
        }

    /** Nombre corto del tipo para el personal: "Mesa", "Cancha"... */
    fun kindLabel(type: String?): String =
        when (type) {
            "cancha" -> Venue.courtTitle
            "barra" -> "Barra"
            "drive_thru" -> "Carril"
            "asiento" -> "Asiento"
            else -> if (isRoom(type)) Venue.roomTitle else "Mesa"
        }

    /** El nombre que le puso el negocio con su artículo: «la Mesa 4», «el Asiento 3». */
    fun named(
        type: String?,
        name: String,
    ): String = (if (type == "asiento" || type == "drive_thru") "el " else "la ") + name

    /** Cómo nombra el cliente su lugar: "tu mesa", "tu cancha"... */
    fun yourPlace(type: String?): String =
        when (type) {
            "cancha" -> "tu ${Venue.court}"
            "barra" -> "la barra"
            "drive_thru" -> "tu auto"
            "asiento" -> "tu asiento"
            null, "mesa" -> if (isRoom(type)) "tu ${Venue.room}" else "tu mesa"
            else -> "tu espacio"
        }

    /** Título de la opción de destino en el carrito (antes siempre "Comer aquí"). */
    fun destinationTitle(type: String?): String =
        when (type) {
            "cancha" -> "En la ${Venue.court}"
            "barra" -> "En la barra"
            "drive_thru" -> "En tu auto"
            "asiento" -> "En tu asiento"
            else -> if (isRoom(type)) "A tu ${Venue.room}" else "Comer aquí"
        }

    /** Carrito en efectivo con destino a un espacio. En la barra se entrega ahí; no "se lleva". */
    fun payAtCashierHint(type: String?): String =
        if (type == "barra") {
            "Pagas en caja y te lo entregamos en la barra"
        } else if (isRoom(type)) {
            "Se carga a tu ${Venue.room} y te lo llevamos a tu puerta"
        } else {
            "Pagas en caja y te lo llevamos a ${yourPlace(type)}"
        }

    /** Aviso de pedido listo con destino a un espacio. */
    fun readyInSpace(type: String?): String =
        if (type == "barra") {
            "Tu pedido está listo. Te lo entregamos en la barra."
        } else {
            "Tu pedido está listo. Ya va a ${yourPlace(type)}."
        }

    fun destinationSubtitle(type: String?): String =
        if (type == null) "Te lo llevamos a tu espacio" else "Te lo llevamos a ${yourPlace(type)}"

    fun pickPrompt(type: String?): String =
        when (type) {
            "cancha" -> "Selecciona tu ${Venue.court}"
            "barra" -> "Selecciona tu lugar en la barra"
            "drive_thru" -> "Selecciona tu carril"
            "asiento" -> "Selecciona tu asiento"
            else -> if (isRoom(type)) "Selecciona tu ${Venue.room}" else "Selecciona tu mesa"
        }

    /**
     * Para cocina y caja: adónde va el pedido, con el nombre del espacio ("Comer aquí · Mesa 1").
     * En una habitación el personal lee «A la habitación · Hab. 304», no «A tu habitación».
     */
    fun staffDestination(
        destination: OrderDestination,
        space: OrderSpace?,
    ): String {
        if (destination != OrderDestination.IN_SPACE) return "Para llevar"
        val title = if (isRoom(space?.type)) "A la ${Venue.room}" else destinationTitle(space?.type)
        val name = space?.name?.takeIf { it.isNotBlank() }?.let { short(space.type, it) } ?: return title
        return "$title · $name"
    }
}