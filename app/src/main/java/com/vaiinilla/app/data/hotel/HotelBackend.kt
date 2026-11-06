package com.vaiinilla.app.data.hotel

/**
 * Qué repositorio de hotel usa la app. Mientras la APK apunte a un backend sin el modo Hotel, usa
 * [FakeHotelRepository]; una APK apuntada a un backend de hotel (su URL lleva `hotel`, por ejemplo
 * `hotel.169-58-43-188.sslip.io`) habla con el servidor real sin cambiar código.
 */
object HotelBackend {
    /** Pone `true` o `false` para forzarlo (pruebas); `null` lo decide la URL. */
    var forceFake: Boolean? = null

    fun useFake(apiBaseUrl: String): Boolean = forceFake ?: !apiBaseUrl.contains("hotel", ignoreCase = true)
}
