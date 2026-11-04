package com.vaiinilla.app.core.network

import java.util.UUID

/**
 * Llaves de idempotencia para operaciones sin versión (abrir caja, apartar cancha, cobrar una reserva…).
 *
 * Si una respuesta se pierde y la persona vuelve a tocar el botón, el reintento debe mandar la MISMA llave
 * para que el servidor devuelva lo que ya hizo en vez de repetirlo (doble cobro, doble caja, doble apartado).
 * La llave se guarda por firma de la operación (qué + con qué datos) y se olvida en cuanto el servidor
 * confirma: la siguiente operación igual, ya legítima, recibe una llave nueva.
 */
class RetryKeys {
    private val keys = mutableMapOf<String, String>()

    fun keyFor(vararg signature: Any?): String =
        synchronized(keys) { keys.getOrPut(signature.joinToString("|")) { UUID.randomUUID().toString() } }

    fun forget(vararg signature: Any?) {
        synchronized(keys) { keys.remove(signature.joinToString("|")) }
    }

    /** Corre [call] con la llave de [signature] y la olvida solo si el servidor confirmó. */
    inline fun <T> withKey(
        vararg signature: Any?,
        call: (String) -> Result<T>,
    ): Result<T> = call(keyFor(*signature)).onSuccess { forget(*signature) }
}
