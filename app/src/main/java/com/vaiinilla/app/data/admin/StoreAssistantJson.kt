package com.vaiinilla.app.data.admin

import com.vaiinilla.app.domain.admin.AssistantChange
import com.vaiinilla.app.domain.admin.AssistantLabels
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.domain.admin.AssistantProposal
import com.vaiinilla.app.domain.admin.AssistantReply
import com.vaiinilla.app.domain.model.OrderWindow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** `POST /establecimiento/asistente`: la plática de ida y la respuesta de vuelta. */
class StoreAssistantJson(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun encodeMessages(messages: List<AssistantMessage>): String =
        buildJsonObject {
            putJsonArray("mensajes") {
                messages.forEach { message ->
                    addJsonObject {
                        put("rol", if (message.fromOwner) "dueno" else "asistente")
                        put("texto", message.text)
                    }
                }
            }
        }.toString()

    fun parseReply(body: String): AssistantReply {
        val root = json.parseToJsonElement(body).jsonObject
        val data = (root["data"] as? JsonObject) ?: root
        val message = (data["mensaje"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        require(message.isNotEmpty()) { "El asistente no mandó mensaje." }
        val options =
            (data["opciones"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
                .orEmpty()
        return AssistantReply(
            message = message,
            options = options,
            proposal = (data["propuesta"] as? JsonObject)?.let(::parseProposal),
            remainingMessages = (data["quedan_mensajes"] as? JsonPrimitive)?.intOrNull ?: Int.MAX_VALUE,
        )
    }

    private fun parseProposal(proposal: JsonObject): AssistantProposal? {
        val changes = proposal["cambios"] as? JsonObject ?: return null
        if (changes.isEmpty()) return null
        val reasons = proposal["motivos"] as? JsonObject
        val items =
            changes.map { (field, value) ->
                AssistantChange(
                    field = field,
                    title = AssistantLabels.title(field),
                    value = valueLabel(field, value),
                    reason = (reasons?.get(field) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() },
                )
            }
        return AssistantProposal(patchBody = changes.toString(), changes = items)
    }

    private fun valueLabel(
        field: String,
        value: JsonElement,
    ): String =
        when {
            value is JsonNull -> "Sin definir"
            value is JsonArray && field == "franjas_pedido" ->
                AssistantLabels.windows(
                    value.mapNotNull { item ->
                        val window = item as? JsonObject ?: return@mapNotNull null
                        val from = (window["desde"] as? JsonPrimitive)?.contentOrNull
                        val to = (window["hasta"] as? JsonPrimitive)?.contentOrNull
                        if (from != null && to != null) OrderWindow(from, to) else null
                    },
                )
            value is JsonPrimitive && value.isString ->
                when (field) {
                    "fondo_sugerido", "reparto_costo", "reparto_minimo", "recarga_minima", "recarga_maxima" ->
                        AssistantLabels.money(value.content)
                    else -> AssistantLabels.choice(field, value.content)
                }
            value is JsonPrimitive && value.booleanOrNull != null -> AssistantLabels.yesNo(field, value.booleanOrNull!!)
            value is JsonPrimitive && value.longOrNull != null -> AssistantLabels.number(field, value.longOrNull!!)
            else -> value.toString()
        }
}
