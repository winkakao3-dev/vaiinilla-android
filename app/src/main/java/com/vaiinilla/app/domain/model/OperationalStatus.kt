package com.vaiinilla.app.domain.model

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
) {
    /** En un drive-thru el cliente avisa desde la app que ya llegó por su pedido. */
    val isDriveThru: Boolean
        get() = businessType == "drive_thru"
}
