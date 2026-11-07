package com.vaiinilla.app.domain.hotel

/** Lo que el dueño configura del hotel (`hotel_config` del establecimiento); null en el modelo = servidor sin hotel. */
data class HotelConfig(
    /** `false` en un negocio que no es hotel: la app no muestra nada de hotel. */
    val active: Boolean = true,
    /** El huésped puede cargar el room service a su habitación. */
    val chargeToRoom: Boolean = true,
    /** El huésped puede pagar con tarjeta al pedir. */
    val cardAtOrderEnabled: Boolean = false,
    /**
     * Preferencia guardada en el servidor, que no la exige: la entrega a una habitación nunca pide código (decisión de
     * Naev). La app la conserva solo para no pisar lo que ya había al guardar la configuración.
     */
    val deliveryCode: Boolean = false,
    /** «HH:mm» desde y hasta cuándo la cocina recibe pedidos a las habitaciones; null = sin horario propio. */
    val kitchenFrom: String? = null,
    val kitchenTo: String? = null,
    /** El huésped puede pedir toallas, limpieza y demás desde la app. */
    val requestsEnabled: Boolean = true,
    /** El huésped puede poner «No molestar». */
    val doNotDisturbEnabled: Boolean = true,
    /** «HH:mm» hasta cuándo se puede pedir salida tardía; null = no se ofrece. */
    val lateCheckoutTime: String? = null,
    /** Precio de la salida tardía, como texto con punto decimal. */
    val lateCheckoutPrice: String? = null,
    /** «HH:mm» de la salida normal. */
    val checkoutTime: String? = null,
)

enum class StayStatus(
    val wire: String,
    val label: String,
) {
    RESERVED("reservada", "Por llegar"),
    IN_HOUSE("en_casa", "En casa"),
    CHECKED_OUT("salio", "Salió"),
    CANCELED("cancelada", "Cancelada"),
    ;

    companion object {
        fun fromWire(value: String?): StayStatus = entries.firstOrNull { it.wire == value } ?: IN_HOUSE
    }
}

enum class ChargeCategory(
    val wire: String,
    val label: String,
) {
    MINIBAR("minibar", "Minibar"),
    SPA("spa", "Spa"),
    LATE_CHECKOUT("salida_tardia", "Salida tardía"),
    OTHER("otro", "Otro"),
    ;

    companion object {
        fun fromWire(value: String?): ChargeCategory = entries.firstOrNull { it.wire == value } ?: OTHER
    }
}

/** Cómo pagó el huésped al salir. «Tarjeta» es el registro de una terminal externa, no un cobro de la app. */
enum class CheckoutMethod(
    val wire: String,
    val label: String,
) {
    CARD("tarjeta", "Tarjeta"),
    CASH("efectivo", "Efectivo"),

    /** El servidor todavía no lo acepta (`metodo_pago` es efectivo o tarjeta): no ofrecerlo. */
    TRANSFER("transferencia", "Transferencia"),
}

enum class RequestType(
    val wire: String,
    val label: String,
) {
    TOWELS("toallas", "Toallas"),
    PILLOWS("almohadas", "Almohadas"),
    CLEANING("limpieza", "Limpieza"),
    CHARGER("cargador", "Cargador"),
    LATE_CHECKOUT("salida_tardia", "Salida tardía"),
    OTHER("otro", "Otra cosa"),
    ;

    companion object {
        fun fromWire(value: String?): RequestType = entries.firstOrNull { it.wire == value } ?: OTHER
    }
}

enum class RequestStatus(
    val wire: String,
    val label: String,
) {
    PENDING("pendiente", "Pendiente"),
    TAKEN("tomada", "En camino"),
    RESOLVED("resuelta", "Resuelta"),
    CANCELED("cancelada", "Cancelada"),
    ;

    val isOpen: Boolean get() = this == PENDING || this == TAKEN

    companion object {
        fun fromWire(value: String?): RequestStatus = entries.firstOrNull { it.wire == value } ?: PENDING
    }
}

/** `POST solicitudes-hotel/:id/transiciones`: tomar, resolver o aprobar (la salida tardía suma un cargo). */
enum class RequestAction(
    val wire: String,
) {
    TAKE("tomar"),
    RESOLVE("resolver"),
    APPROVE("aprobar"),
}

enum class RoomCleaning(
    val wire: String,
    val label: String,
) {
    READY("lista", "Lista"),
    DIRTY("por_limpiar", "Por limpiar"),
    ;

    companion object {
        fun fromWire(value: String?): RoomCleaning = entries.firstOrNull { it.wire == value } ?: READY
    }
}

/** Dinero siempre como texto con punto decimal, como lo manda el servidor. */
data class StayCharge(
    val id: String,
    val concept: String,
    val amount: String,
    val category: ChargeCategory,
    val createdAt: String? = null,
)

/** Un pedido de room service que va a la cuenta de la estancia. */
data class StayOrder(
    val orderId: String,
    /** El folio como lo manda el servidor («A-014»). */
    val folio: String?,
    val total: String,
    val state: String,
    val createdAt: String? = null,
    val itemsSummary: String? = null,
    /** Todavía no se cobra: se cobra en el check-out. */
    val pendingPayment: Boolean = false,
)

/** La cuenta de la estancia: noches, cargos y room service. */
data class StayAccount(
    val nightsTotal: String? = null,
    val chargesTotal: String? = null,
    val ordersTotal: String? = null,
    val total: String? = null,
    val pendingOrders: Int = 0,
)

data class Stay(
    val id: String,
    val guestName: String,
    val contact: String? = null,
    val userId: String? = null,
    /** Código de 6 dígitos que recepción le da al huésped. */
    val code: String? = null,
    val roomId: Int? = null,
    val roomName: String? = null,
    val floor: Int? = null,
    /** Fechas ISO (`yyyy-MM-dd` o instante). */
    val checkIn: String,
    val checkOut: String,
    val nightlyRate: String? = null,
    val status: StayStatus = StayStatus.IN_HOUSE,
    val nights: Int = 1,
    val charges: List<StayCharge> = emptyList(),
    val orders: List<StayOrder> = emptyList(),
    /** Cuenta completa (noches, cargos y room service); null si el servidor no la calcula. */
    val total: String? = null,
    val paymentMethod: String? = null,
    val paidAt: String? = null,
    val doNotDisturb: Boolean = false,
    val category: String? = null,
    /** Para `version_esperada` al cambiar la estancia (check-in, cancelar, check-out). */
    val version: Int = 1,
    /** Instante de la salida prevista (ISO). */
    val expectedDeparture: String? = null,
    val linkedUsers: Int = 0,
    val account: StayAccount? = null,
    val totalCharged: String? = null,
    /** Solo al huésped: los ajustes del hotel (`ajustes`, la misma forma que `hotel/configuracion`). */
    val settings: HotelConfig? = null,
)

data class NewStay(
    val guestName: String,
    val contact: String?,
    val roomId: Int,
    /** `null` = llega hoy: el servidor usa el día del hotel, no el del teléfono, y la estancia queda en casa. */
    val checkIn: String?,
    val checkOut: String,
    val nightlyRate: String?,
)

data class HotelRequest(
    val id: String,
    val stayId: String? = null,
    val roomId: Int? = null,
    val roomName: String? = null,
    val floor: Int? = null,
    val type: RequestType,
    val quantity: Int = 1,
    val note: String? = null,
    val status: RequestStatus = RequestStatus.PENDING,
    /** Quién la tomó (personal); el huésped no ve el nombre. */
    val takenBy: String? = null,
    val createdAt: String? = null,
    /** Para `version_esperada` en las transiciones. */
    val version: Int = 1,
    val guestName: String? = null,
)

data class HotelRoom(
    val id: Int,
    val name: String,
    val floor: Int? = null,
    /** «doble», «suite»… tal como la escribe el dueño. */
    val category: String? = null,
    val cleaning: RoomCleaning = RoomCleaning.READY,
    val occupied: Boolean = false,
    val doNotDisturb: Boolean = false,
    val openRequests: Int = 0,
    val stayId: String? = null,
    val guestName: String? = null,
    /** El QR permanente de la puerta: el token y la URL que se imprime o se comparte; null si el servidor no los manda. */
    val qrToken: String? = null,
    val qrUrl: String? = null,
    /** Salida prevista de la estancia en curso (ISO). */
    val stayDeparture: String? = null,
    val ordersInProgress: Int = 0,
)

/** Lo que dejó `POST habitaciones/lote`: cuántas se crearon y cuáles se omitieron (ya existían). */
data class RoomBatchResult(
    val created: Int,
    val omitted: List<String> = emptyList(),
    /** Mesas que ya existían con ese nombre y se convirtieron en habitación. */
    val converted: Int = 0,
)

data class HotelTopItem(
    val name: String,
    val quantity: Int,
    val total: String? = null,
)

/** `GET hotel/resumen` para el inicio del admin (A03). */
data class HotelSummary(
    val occupied: Int,
    val totalRooms: Int,
    val roomServiceToday: String? = null,
    val roomOrdersToday: Int = 0,
    val inKitchenNow: Int = 0,
    val openRequests: Int = 0,
    val oldestRequestMinutes: Int? = null,
    val topItems: List<HotelTopItem> = emptyList(),
    val departuresToday: Int = 0,
    val dirtyRooms: Int = 0,
    val doNotDisturbRooms: Int = 0,
    val ordersToRooms: Int = 0,
    val arrivalsToday: Int = 0,
    val departures: List<HotelDeparture> = emptyList(),
)

data class HotelDeparture(
    val stayId: String,
    val guestName: String,
    val roomName: String,
    val expectedDeparture: String? = null,
)

/** Códigos de error del servidor que las pantallas del hotel distinguen. */
object HotelErrors {
    /** Pedir a una habitación sin estancia vigente ligada a la cuenta (`403`). */
    const val ROOM_REQUIRES_STAY = "ROOM_REQUIRES_STAY"

    /** El código no existe, venció o no es de esa habitación (el servidor no distingue). */
    const val STAY_NOT_FOUND = "STAY_NOT_FOUND"
    const val STAY_INVALID_STATE = "STAY_INVALID_STATE"
    const val ROOM_OCCUPIED = "ROOM_OCCUPIED"
    const val SPACE_NOT_ROOM = "SPACE_NOT_ROOM"

    /** La cuenta cambió desde que recepción la vio: el error trae el total real. */
    const val ACCOUNT_TOTAL_CHANGED = "ACCOUNT_TOTAL_CHANGED"

    /** Cerrar una cuenta con pedidos por cobrar exige la caja abierta. */
    const val ESTABLISHMENT_NOT_RECEIVING = "ESTABLISHMENT_NOT_RECEIVING"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val STAY_GUEST_LIMIT = "STAY_GUEST_LIMIT"
    const val HOTEL_TYPE_LOCKED = "HOTEL_TYPE_LOCKED"
    const val ROOM_CHARGE_DISABLED = "ROOM_CHARGE_DISABLED"
    const val CARD_AT_ORDER_DISABLED = "CARD_AT_ORDER_DISABLED"
    const val OUTSIDE_ORDER_HOURS = "OUTSIDE_ORDER_HOURS"
    const val REQUESTS_DISABLED = "REQUESTS_DISABLED"
    const val DND_DISABLED = "DND_DISABLED"
    const val LATE_CHECKOUT_UNAVAILABLE = "LATE_CHECKOUT_UNAVAILABLE"
    const val REQUEST_VERSION_CONFLICT = "REQUEST_VERSION_CONFLICT"
    const val REQUEST_INVALID_STATE = "REQUEST_INVALID_STATE"
}

class HotelException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
