package com.vaiinilla.app.domain.counter

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Lo que se le dice a la cajera cuando el servidor no cobra o no entrega. Le habla a ella, no al alumno. */
object StudentCounterMessages {
    private val hour = DateTimeFormatter.ofPattern("H:mm")

    fun chargeFailure(
        error: Throwable,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val api = generateSequence(error) { it.cause }.filterIsInstance<ApiClientException>().firstOrNull()
        when (api?.code) {
            "PIN_INCORRECT" -> {
                val left = detail(api, "intentos_restantes")?.intOrNull
                if (left != null) {
                    return if (left == 1) {
                        "El PIN no coincide; queda 1 intento."
                    } else {
                        "El PIN no coincide; quedan $left intentos."
                    }
                }
            }
            "PIN_LOCKED" -> {
                val until =
                    detail(api, "bloqueado_hasta")
                        ?.content
                        ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                if (until != null) {
                    return "Se equivocaron 5 veces con su PIN. Se puede volver a intentar a las " +
                        "${hour.format(until.atZone(zone))} o llama a su tutor."
                }
            }
        }
        return error.toUserFacingMessage("No se cobró. Intenta otra vez.")
    }

    fun deliveryFailure(error: Throwable): String = error.toUserFacingMessage("No se entregó. Intenta otra vez.")

    private fun detail(
        error: ApiClientException,
        key: String,
    ): JsonPrimitive? {
        val first =
            when (val details = error.details) {
                is JsonArray -> details.firstOrNull()
                is JsonObject -> details
                else -> null
            } as? JsonObject ?: return null
        return first[key] as? JsonPrimitive
    }
}
