package com.vaiinilla.app.data.admin

import com.vaiinilla.app.domain.admin.AssistantCategory
import com.vaiinilla.app.domain.admin.AssistantChange
import com.vaiinilla.app.domain.admin.AssistantInvite
import com.vaiinilla.app.domain.admin.AssistantLabels
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.domain.admin.AssistantProduct
import com.vaiinilla.app.domain.admin.AssistantProposal
import com.vaiinilla.app.domain.admin.AssistantReply
import com.vaiinilla.app.domain.admin.AssistantSpaceGroup
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
    fun encodeMessages(
        messages: List<AssistantMessage>,
        photoIds: List<String> = emptyList(),
    ): String =
        buildJsonObject {
            putJsonArray("mensajes") {
                messages.forEach { message ->
                    addJsonObject {
                        put("rol", if (message.fromOwner) "dueno" else "asistente")
                        put("texto", message.text)
                    }
                }
            }
            if (photoIds.isNotEmpty()) putJsonArray("fotos") { photoIds.forEach { add(JsonPrimitive(it)) } }
        }.toString()

    /** `POST /catalogo/categorias`. */
    fun encodeCategory(
        name: String,
        order: Int,
    ): String =
        buildJsonObject {
            put("nombre", name)
            put("orden", order)
        }.toString()

    /** `POST /catalogo/productos`: lo mínimo; tamaños, extras y alérgenos se ajustan después en Menú. */
    fun encodeProduct(
        categoryId: Int,
        product: AssistantProduct,
    ): String =
        buildJsonObject {
            put("categoria_id", categoryId)
            put("estacion_preparacion", product.station)
            put("nombre", product.name)
            put("descripcion", product.description?.let(::JsonPrimitive) ?: JsonNull)
            put("ingredientes", JsonNull)
            put("alergenos", JsonNull)
            put("tiempo_estimado_min", if (product.station == "ninguna") 0 else DEFAULT_PREP_MINUTES)
            put("precio_mostrador", product.price)
            put("disponible", true)
            putJsonArray("grupos_opcion") {}
        }.toString()

    /** El `id` del recurso que regresó un POST (`{"data":{"id":…}}`). */
    fun parseId(body: String): Int {
        val root = json.parseToJsonElement(body).jsonObject
        val data = (root["data"] as? JsonObject) ?: root
        return requireNotNull((data["id"] as? JsonPrimitive)?.intOrNull) { "La respuesta no trae id." }
    }

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
        val changes = proposal["cambios"] as? JsonObject ?: JsonObject(emptyMap())
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
        val spaces =
            (proposal["espacios"] as? JsonArray).orEmpty().mapNotNull { item ->
                val group = item as? JsonObject ?: return@mapNotNull null
                AssistantSpaceGroup(
                    type = group.text("tipo") ?: return@mapNotNull null,
                    prefix = group.text("prefijo") ?: return@mapNotNull null,
                    count = (group["cantidad"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null,
                )
            }
        val menu =
            (proposal["menu"] as? JsonArray).orEmpty().mapNotNull { item ->
                val category = item as? JsonObject ?: return@mapNotNull null
                val products =
                    (category["productos"] as? JsonArray).orEmpty().mapNotNull { raw ->
                        val product = raw as? JsonObject ?: return@mapNotNull null
                        AssistantProduct(
                            name = product.text("nombre") ?: return@mapNotNull null,
                            price = product.text("precio") ?: return@mapNotNull null,
                            description = product.text("descripcion"),
                            station = product.text("prepara") ?: "cocina",
                            photoId = product.text("foto"),
                        )
                    }
                if (products.isEmpty()) return@mapNotNull null
                AssistantCategory(
                    name = category.text("categoria") ?: return@mapNotNull null,
                    existingId = (category["categoria_id"] as? JsonPrimitive)?.intOrNull,
                    products = products,
                )
            }
        val invites =
            (proposal["personal"] as? JsonArray).orEmpty().mapNotNull { item ->
                val invite = item as? JsonObject ?: return@mapNotNull null
                AssistantInvite(
                    email = invite.text("email") ?: return@mapNotNull null,
                    role = invite.text("rol") ?: return@mapNotNull null,
                )
            }
        val storePhotos =
            (proposal["fotos"] as? JsonObject)
                ?.mapNotNull { (kind, id) -> (id as? JsonPrimitive)?.contentOrNull?.let { kind to it } }
                ?.toMap()
                .orEmpty()
        val sectionReasons =
            SECTIONS.mapNotNull { section -> reasons?.text(section)?.let { section to it } }.toMap()
        if (items.isEmpty() && spaces.isEmpty() && menu.isEmpty() && invites.isEmpty() && storePhotos.isEmpty()) {
            return null
        }
        return AssistantProposal(
            patchBody = changes.takeIf { it.isNotEmpty() }?.toString(),
            changes = items,
            spaces = spaces,
            menu = menu,
            invites = invites,
            storePhotos = storePhotos,
            sectionReasons = sectionReasons,
        )
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.contentOrNull
            ?.trim()
            ?.takeIf(String::isNotEmpty)

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

    private companion object {
        val SECTIONS = listOf("espacios", "menu", "personal", "fotos")
        const val DEFAULT_PREP_MINUTES = 10
    }
}
