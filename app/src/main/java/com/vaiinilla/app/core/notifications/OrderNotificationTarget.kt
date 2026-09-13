package com.vaiinilla.app.core.notifications

enum class OrderNotificationTarget(
    val wireValue: String,
) {
    CLIENT("client"),
    STAFF("staff"),
    ;

    companion object {
        fun fromWireValue(value: String?): OrderNotificationTarget =
            entries.firstOrNull { it.wireValue == value } ?: CLIENT
    }
}
