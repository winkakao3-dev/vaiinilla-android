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
    ): String {
        val original = Json.parseToJsonElement(body).jsonObject
        val capture =
            buildJsonObject {
                put("id", JsonPrimitive(orderId))
                put("capturado_en", JsonPrimitive(capturedAt.toString()))
                put("folio_local", JsonPrimitive(localFolio))
            }
        return JsonObject(original + ("sin_conexion" to capture)).toString()
    }
}
