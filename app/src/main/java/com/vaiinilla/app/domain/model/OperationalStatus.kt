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
    /** «Te lo llevamos», «Pasa por él» o ambas, para pedidos en mesa o lugar. */
    val spaceDelivery: SpaceDelivery = SpaceDelivery.TABLE,
    /** Si es true, el dueño activó la tarjeta en su panel y Stripe ya le permite cobrar. */
    val acceptsCard: Boolean = false,
    /** Si es false, el dueño apagó los pagos con saldo: el servidor rechaza `metodo_pago: saldo`. */
    val allowsBalance: Boolean = true,
    /** Si es false, el cliente no puede elegir «Pago en caja»: POST /pedidos responde CASH_AT_REGISTER_NOT_ALLOWED. */
    val allowsCashAtRegister: Boolean = true,
    /** Lo que el dueño dice que es el negocio (`drive_thru`, `restaurante`, `padel`…). */
    val businessType: String = "cafeteria",
    /** Franjas en las que se reciben pedidos; vacía = sin límite. */
    val orderWindows: List<OrderWindow> = emptyList(),
    /** `false` cuando el negocio tiene franjas y ahora está fuera de ellas. */
    val withinOrderWindow: Boolean = true,
    /** Solo una escuela muestra Mis hijos, Alumnos y Escuela; es presentación, las rutas no lo revisan. */
    val isSchool: Boolean = false,
    /** El cliente ya dio el código de la escuela: puede agregar o vincular hijos. */
    val isParentUnlocked: Boolean = true,
    /** Si es false, Cocina no puede empezar un pedido por cobrar: el servidor lo rechaza hasta que Caja cobre. */
    val kitchenPreparesUnpaid: Boolean = true,
    /**
     * Para la administración: Caja o Cocina latieron en los últimos 2 min. `cashierOnline` y `kitchenOnline`
     * miran 15 s y deciden la operación degradada; con el teléfono bloqueado salían «Sin conexión» enseguida.
     */
    val cashierConnected: Boolean? = null,
    val kitchenConnected: Boolean? = null,
    /** `null`: el negocio no lleva a domicilio o el servidor no lo conoce. */
    val homeDelivery: HomeDeliveryOffer? = null,
    /** `null`: no toma encargos para otro día. */
    val preorders: PreorderOffer? = null,
) {
    /** Lo que el resumen del admin dice de Caja: la ventana de 2 min si el servidor la manda. */
    val cashierPresent: Boolean get() = cashierConnected ?: cashierOnline

    val kitchenPresent: Boolean get() = kitchenConnected ?: kitchenOnline

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

/** Lo que el cliente ve de la entrega a domicilio del negocio (CASA-05). */
data class HomeDeliveryOffer(
    val fee: String,
    val minimum: String?,
    val radiusKm: String,
    val payOnArrival: Boolean,
    /** Banco, CLABE y titular, para pagar por transferencia al recibir. */
    val transferDetails: String?,
)

/** Encargos para otro día (CASA-06). */
data class PreorderOffer(
    /** Hasta cuántos días adelante se puede pedir. */
    val daysAhead: Int,
    /** "18:00": después de esa hora el día siguiente ya no cuenta como día de margen. */
    val cutoff: String?,
    val requiresAcceptance: Boolean,
    val depositPercent: Int?,
    /** En cuánto contesta el negocio un encargo; null si no puso plazo. */
    val answerMinutes: Int? = null,
)

/** "30 minutos", "1 hora", "3 horas" o "1 día": el plazo que el negocio se pone para contestar. */
fun answerTimeLabel(minutes: Int): String =
    when {
        minutes < 60 -> "$minutes minutos"
        minutes % 1440 == 0 -> (minutes / 1440).let { if (it == 1) "1 día" else "$it días" }
        minutes % 60 == 0 -> (minutes / 60).let { if (it == 1) "1 hora" else "$it horas" }
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
