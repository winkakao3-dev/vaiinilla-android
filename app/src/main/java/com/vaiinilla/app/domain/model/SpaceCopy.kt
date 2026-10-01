package com.vaiinilla.app.domain.model

/**
 * Vocabulario según el tipo de espacio del pedido (`mesa`, `barra`, `cancha`, `drive_thru`, `asiento`).
 * Sin tipo o con `mesa` se conserva el texto de siempre.
 */
object SpaceCopy {
    /** Nombre corto del tipo para el personal: "Mesa", "Cancha"... */
    fun kindLabel(type: String?): String =
        when (type) {
            "cancha" -> "Cancha"
            "barra" -> "Barra"
            "drive_thru" -> "Carril"
            "asiento" -> "Asiento"
            else -> "Mesa"
        }

    /** Cómo nombra el cliente su lugar: "tu mesa", "tu cancha"... */
    fun yourPlace(type: String?): String =
        when (type) {
            "cancha" -> "tu cancha"
            "barra" -> "la barra"
            "drive_thru" -> "tu auto"
            "asiento" -> "tu asiento"
            null, "mesa" -> "tu mesa"
            else -> "tu espacio"
        }

    /** Título de la opción de destino en el carrito (antes siempre "Comer aquí"). */
    fun destinationTitle(type: String?): String =
        when (type) {
            "cancha" -> "En la cancha"
            "barra" -> "En la barra"
            "drive_thru" -> "En tu auto"
            "asiento" -> "En tu asiento"
            else -> "Comer aquí"
        }

    fun destinationSubtitle(type: String?): String =
        if (type == null) "Te lo llevamos a tu espacio" else "Te lo llevamos a ${yourPlace(type)}"

    fun pickPrompt(type: String?): String =
        when (type) {
            "cancha" -> "Selecciona tu cancha"
            "barra" -> "Selecciona tu lugar en la barra"
            "drive_thru" -> "Selecciona tu carril"
            "asiento" -> "Selecciona tu asiento"
            else -> "Selecciona tu mesa"
        }

    /** Para cocina y caja: adónde va el pedido, con el nombre del espacio ("Comer aquí · Mesa 1"). */
    fun staffDestination(
        destination: OrderDestination,
        space: OrderSpace?,
    ): String {
        if (destination != OrderDestination.IN_SPACE) return "Para llevar"
        val title = destinationTitle(space?.type)
        val name = space?.name?.takeIf { it.isNotBlank() } ?: return title
        return "$title · $name"
    }
}
