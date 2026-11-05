package com.vaiinilla.app.domain.model

/**
 * Vocabulario según el tipo de espacio del pedido (`mesa`, `barra`, `cancha`, `drive_thru`, `asiento`).
 * Sin tipo o con `mesa` se conserva el texto de siempre.
 */
object SpaceCopy {
    /** Nombre corto del tipo para el personal: "Mesa", "Cancha"... */
    fun kindLabel(type: String?): String =
        when (type) {
            "cancha" -> Venue.courtTitle
            "barra" -> "Barra"
            "drive_thru" -> "Carril"
            "asiento" -> "Asiento"
            else -> "Mesa"
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
            null, "mesa" -> "tu mesa"
            else -> "tu espacio"
        }

    /** Título de la opción de destino en el carrito (antes siempre "Comer aquí"). */
    fun destinationTitle(type: String?): String =
        when (type) {
            "cancha" -> "En la ${Venue.court}"
            "barra" -> "En la barra"
            "drive_thru" -> "En tu auto"
            "asiento" -> "En tu asiento"
            else -> "Comer aquí"
        }

    /** Carrito en efectivo con destino a un espacio. En la barra se entrega ahí; no "se lleva". */
    fun payAtCashierHint(type: String?): String =
        if (type == "barra") {
            "Pagas en caja y te lo entregamos en la barra"
        } else {
            "Pagas en caja y te lo llevamos a ${yourPlace(type)}"
        }

    /** Aviso de pedido listo con destino a un espacio. */
    fun readyInSpace(type: String?): String =
        if (type == "barra") {
            "Tu pedido está listo. Te lo entregamos en la barra."
        } else {
            "Tu pedido está listo. En un momento te lo llevan a ${yourPlace(type)}."
        }

    /** Aviso de pedido entregado en un espacio: el mesero ya lo dejó en el lugar del cliente. */
    fun deliveredInSpace(type: String?): String =
        if (type == "barra") {
            "Pedido entregado. Buen provecho."
        } else {
            "Ya está en ${yourPlace(type)}. Buen provecho."
        }

    /** Subtítulo de la opción de destino en el carrito. Si el negocio solo entrega en barra, no se lleva al lugar. */
    fun destinationSubtitle(
        type: String?,
        delivery: SpaceDelivery = SpaceDelivery.TABLE,
    ): String =
        when {
            delivery == SpaceDelivery.BAR -> "Pides desde tu lugar y pasas por él a la barra"
            type == null -> "Te lo llevamos a tu espacio"
            else -> "Te lo llevamos a ${yourPlace(type)}"
        }

    fun pickPrompt(type: String?): String =
        when (type) {
            "cancha" -> "Selecciona tu ${Venue.court}"
            "barra" -> "Selecciona tu lugar en la barra"
            "drive_thru" -> "Selecciona tu carril"
            "asiento" -> "Selecciona tu asiento"
            else -> "Selecciona tu mesa"
        }

    /**
     * Para cocina y caja: adónde va el pedido, con el nombre del espacio ("Comer aquí · Mesa 1"). Un «Pasa por
     * él» (`recoge_en_barra`) se entrega en la barra aunque se pidió desde el lugar:
     * «Pasa por él a la barra · Pista 1».
     */
    fun staffDestination(
        destination: OrderDestination,
        space: OrderSpace?,
        pickUpAtBar: Boolean = false,
    ): String {
        when (destination) {
            OrderDestination.TAKE_AWAY -> return "Para llevar"
            OrderDestination.HOME_DELIVERY -> return "A domicilio"
            OrderDestination.IN_SPACE -> Unit
        }
        val title = if (pickUpAtBar) "Pasa por él a la barra" else destinationTitle(space?.type)
        val name = space?.name?.takeIf { it.isNotBlank() } ?: return title
        return "$title · $name"
    }
}
