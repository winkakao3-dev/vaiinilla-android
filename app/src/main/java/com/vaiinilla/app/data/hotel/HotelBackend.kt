package com.vaiinilla.app.data.hotel

/**
 * Qué repositorio de hotel usa la app. El servidor real es siempre el valor por omisión: [FakeHotelRepository]
 * inventa estancias y cargos, así que solo se usa en una compilación que no es de producción y solo si alguien
 * pone [forceFake] en `true` a propósito (una demo sin servidor de hotel). La URL ya no lo decide: un backend de
 * pruebas que adopte las rutas del hotel no puede volver a servir datos falsos a quien lo prueba.
 */
object HotelBackend {
    /** `true` pide el repositorio en memoria (demo o pruebas); `null` o `false` usan el servidor. */
    var forceFake: Boolean? = null

    fun useFake(production: Boolean): Boolean = !production && forceFake == true
}
