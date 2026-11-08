package com.vaiinilla.app.data.assistant

import com.vaiinilla.app.core.network.ApiJson
import com.vaiinilla.app.domain.assistant.AssistantCourtCard
import com.vaiinilla.app.domain.assistant.AssistantOrderLine
import com.vaiinilla.app.domain.assistant.AssistantOrderProposal
import com.vaiinilla.app.domain.assistant.AssistantProductCard
import com.vaiinilla.app.domain.assistant.AssistantWaiterOffer
import com.vaiinilla.app.domain.assistant.CustomerAssistantReply
import com.vaiinilla.app.domain.assistant.CustomerAssistantTurn
import com.vaiinilla.app.domain.repository.CallReason
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** `POST /asistente-cliente`: la plática de ida y la respuesta de vuelta. */
class CustomerAssistantJson(
    private val json: Json = ApiJson.plain,
) {
    fun encodeTurns(
        turns: List<CustomerAssistantTurn>,
        establishmentId: String? = null,
    ): String =
        buildJsonObject {
            establishmentId?.takeIf(String::isNotBlank)?.let { put("establecimiento_id", it) }
            putJsonArray("mensajes") {
                turns.takeLast(MAX_MESSAGES).forEach { turn ->
                    addJsonObject {
                        put("rol", if (turn.fromCustomer) "cliente" else "asistente")
                        put("texto", turn.text.take(MAX_LETTERS))
                    }
                }
            }
        }.toString()

    fun parseReply(body: String): CustomerAssistantReply {
        val root = json.parseToJsonElement(body).jsonObject
        val data = (root["data"] as? JsonObject) ?: root
        return CustomerAssistantReply(
            message = data.string("mensaje").orEmpty().trim(),
            products = (data["productos"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(::card) },
            order = (data["pedido"] as? JsonObject)?.let(::order),
            waiter = (data["llamar_mesero"] as? JsonObject)?.let(::waiter),
            options =
                (data["opciones"] as? JsonArray)
                    .orEmpty()
                    .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) },
            remainingMessages = (data["quedan_mensajes"] as? JsonPrimitive)?.intOrNull ?: MAX_MESSAGES,
            courts =
                (data["pistas"] as? JsonArray)
                    .orEmpty()
                    .mapNotNull { (it as? JsonObject)?.let(::court) }
                    .distinctBy { it.courtId }
                    .take(MAX_COURTS),
        )
    }

    /**
     * Contrato 6: `{espacio_id, nombre, libre_ahora, libre_hasta, ocupada_hasta, precio_hora, capacidad}`, con
     * `precio_hora = {efectivo_saldo, tarjeta}` (un texto suelto también se acepta como precio único).
     */
    private fun court(obj: JsonObject): AssistantCourtCard? {
        val id = (obj["espacio_id"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: return null
        val price = obj["precio_hora"]
        val cashPrice =
            (price as? JsonObject)?.string("efectivo_saldo")
                ?: (price as? JsonPrimitive)?.contentOrNull
        val cardPrice = (price as? JsonObject)?.string("tarjeta")
        return AssistantCourtCard(
            courtId = id,
            name = obj.string("nombre")?.trim()?.takeIf(String::isNotEmpty) ?: "Pista",
            freeNow = (obj["libre_ahora"] as? JsonPrimitive)?.booleanOrNull ?: false,
            freeUntil = obj.string("libre_hasta")?.takeIf(String::isNotBlank),
            busyUntil = obj.string("ocupada_hasta")?.takeIf(String::isNotBlank),
            pricePerHour = cashPrice?.takeIf(String::isNotBlank) ?: cardPrice?.takeIf(String::isNotBlank),
            cardPricePerHour = cardPrice?.takeIf(String::isNotBlank),
            capacity = (obj["capacidad"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 },
        )
    }

    private fun card(obj: JsonObject): AssistantProductCard? {
        val id = (obj["producto_id"] as? JsonPrimitive)?.intOrNull ?: return null
        return AssistantProductCard(
            productId = id,
            name = obj.string("nombre").orEmpty(),
            price = obj.string("precio") ?: "0",
            imageUrl = obj.string("imagen_url")?.takeIf(String::isNotBlank),
            available = (obj["disponible"] as? JsonPrimitive)?.booleanOrNull ?: true,
            needsOptions = (obj["requiere_opciones"] as? JsonPrimitive)?.booleanOrNull ?: false,
            minutes = (obj["minutos"] as? JsonPrimitive)?.intOrNull,
        )
    }

    private fun order(obj: JsonObject): AssistantOrderProposal? {
        val lines =
            (obj["lineas"] as? JsonArray).orEmpty().mapNotNull { element ->
                val line = element as? JsonObject ?: return@mapNotNull null
                val product = card(line) ?: return@mapNotNull null
                AssistantOrderLine(
                    product = product,
                    quantity = ((line["cantidad"] as? JsonPrimitive)?.intOrNull ?: 1).coerceIn(1, MAX_QUANTITY),
                    note = line.string("nota")?.trim()?.takeIf(String::isNotEmpty),
                )
            }
        val unavailable =
            (obj["no_disponibles"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        val toChoose =
            (obj["por_elegir"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.string("nombre") }
        if (lines.isEmpty() && unavailable.isEmpty() && toChoose.isEmpty()) return null
        return AssistantOrderProposal(lines, obj.string("total") ?: "0", unavailable, toChoose)
    }

    private fun waiter(obj: JsonObject): AssistantWaiterOffer? {
        val reason = CallReason.entries.firstOrNull { it.wireValue == obj.string("motivo") } ?: return null
        return AssistantWaiterOffer(reason, obj.string("detalle")?.trim()?.takeIf(String::isNotEmpty))
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    companion object {
        /** Lo que acepta el servidor. */
        const val MAX_MESSAGES = 40
        const val MAX_LETTERS = 600
        const val MAX_QUANTITY = 20
        const val MAX_COURTS = 4
    }
}
