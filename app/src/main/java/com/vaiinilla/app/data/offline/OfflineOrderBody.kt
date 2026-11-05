package com.vaiinilla.app.data.offline

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.time.Instant

/**
 * Le agrega `sin_conexion` al cuerpo de `POST /pedidos` que ya armó [com.vaiinilla.app.data.order.OrderContractJson]:
 * así el pedido guardado se manda exactamente como se hubiera mandado con red, más cuándo y con qué folio se tomó.
 */
object OfflineOrderBody {
    fun withOfflineCapture(
        body: String,
        orderId: String,
        capturedAt: Instant,
        localFolio: String,
        collectNow: Boolean = false,
    ): String {
        val original = Json.parseToJsonElement(body).jsonObject
        val capture =
            buildJsonObject {
                put("id", JsonPrimitive(orderId))
                put("capturado_en", JsonPrimitive(capturedAt.toString()))
                put("folio_local", JsonPrimitive(localFolio))
            }
        val withCapture = original + ("sin_conexion" to capture)
        // Caja ya recibió el efectivo: el pedido llega cobrado y entra al corte de su turno.
        val collected = if (collectNow) withCapture + ("cobrar_ahora" to JsonPrimitive(true)) else withCapture
        return JsonObject(collected).toString()
    }
}
