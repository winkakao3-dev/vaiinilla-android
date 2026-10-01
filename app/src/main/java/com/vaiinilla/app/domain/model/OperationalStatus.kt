package com.vaiinilla.app.domain.model

/** Una franja en la que el negocio recibe pedidos (comedor de empresa, eventos): hora local del negocio. */
data class OrderWindow(
    val from: String,
    val to: String,
)

data class OperationalStatus(
    val acceptingOrders: Boolean,
    val cashSessionOpen: Boolean,
    val cashierOnline: Boolean,
    val kitchenOnline: Boolean,
    val estimatedTimeMinutes: Int,
    val consultedAt: String,
    /** Si es false, el mesero entrega en un espacio sin escanear el QR del cliente. */
    val deliveryRequiresQr: Boolean = true,
    /** Si es true, el cliente puede pedir en un espacio y pagar al final. */
    val allowsPayAtEnd: Boolean = false,
    /** Lo que el dueño dice que es el negocio (`drive_thru`, `restaurante`, `padel`…). */
    val businessType: String = "cafeteria",
    /** Franjas en las que se reciben pedidos; vacía = sin límite. */
    val orderWindows: List<OrderWindow> = emptyList(),
    /** `false` cuando el negocio tiene franjas y ahora está fuera de ellas. */
    val withinOrderWindow: Boolean = true,
) {
    /** "12:00 a 15:00 y 18:00 a 20:00". */
    val orderHoursText: String
        get() = orderWindows.joinToString(" y ") { "${it.from} a ${it.to}" }

    /** Lo que se le dice al cliente fuera de las franjas; `null` si se reciben pedidos (o no hay franjas). */
    val outsideOrderHoursMessage: String?
        get() =
            when {
                withinOrderWindow -> null
                orderWindows.isEmpty() -> "Por ahora no se reciben pedidos."
                else -> "Por ahora no se reciben pedidos. Horario de pedidos: $orderHoursText."
            }

    /** En un drive-thru el cliente avisa desde la app que ya llegó por su pedido. */
    val isDriveThru: Boolean
        get() = businessType == "drive_thru"
}
