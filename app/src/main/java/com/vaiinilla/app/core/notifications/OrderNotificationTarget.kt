package com.vaiinilla.app.core.notifications

enum class OrderNotificationTarget(
    val wireValue: String,
) {
    CLIENT("client"),
    STAFF("staff"),

    /** Una mesa llamó al mesero: se abre su tablero, donde se ve el motivo (K02). */
    WAITER_CALL("waiter_call"),
    ;

    companion object {
        fun fromWireValue(value: String?): OrderNotificationTarget =
            entries.firstOrNull { it.wireValue == value } ?: CLIENT
    }
}
