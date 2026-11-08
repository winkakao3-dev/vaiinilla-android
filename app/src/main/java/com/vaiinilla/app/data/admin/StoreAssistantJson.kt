package com.vaiinilla.app.data.admin

import com.vaiinilla.app.core.network.ApiJson
import com.vaiinilla.app.domain.admin.AssistantBooking
import com.vaiinilla.app.domain.admin.AssistantCategory
import com.vaiinilla.app.domain.admin.AssistantChange
import com.vaiinilla.app.domain.admin.AssistantCourtHours
import com.vaiinilla.app.domain.admin.AssistantInvite
import com.vaiinilla.app.domain.admin.AssistantLabels
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.domain.admin.AssistantMovement
import com.vaiinilla.app.domain.admin.AssistantProduct
import com.vaiinilla.app.domain.admin.AssistantProductEdit
import com.vaiinilla.app.domain.admin.AssistantProductPhoto
import com.vaiinilla.app.domain.admin.AssistantProposal
import com.vaiinilla.app.domain.admin.AssistantRecipe
import com.vaiinilla.app.domain.admin.AssistantRecipeLine
import com.vaiinilla.app.domain.admin.AssistantReply
import com.vaiinilla.app.domain.admin.AssistantRoleChange
import com.vaiinilla.app.domain.admin.AssistantSpaceEdit
import com.vaiinilla.app.domain.admin.AssistantSpaceGroup
import com.vaiinilla.app.domain.admin.AssistantSupply
import com.vaiinilla.app.domain.admin.AssistantSupplyEdit
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
    private val json: Json = ApiJson.plain,
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

    /** `POST /reservas`: la renta para alguien que llamó o llegó; queda por cobrar en Caja. */
    fun encodeBooking(booking: AssistantBooking): String =
        buildJsonObject {
            put("espacio_id", booking.courtId)
            put("inicio", booking.start)
            put("duracion_min", booking.durationMinutes)
            booking.customerName?.let { put("nombre_cliente", it) }
            booking.players?.let { put("jugadores", it) }
        }.toString()

    /** `POST /reservas/{id}/pago` en efectivo sin monto: el pedido de renta queda por cobrar en Caja. */
    fun encodeCashLater(): String = buildJsonObject { put("metodo_pago", "efectivo") }.toString()

    /** El `id` en texto (UUID) que regresó un POST. */
    fun parseTextId(body: String): String {
        val root = json.parseToJsonElement(body).jsonObject
        val data = (root["data"] as? JsonObject) ?: root
        return requireNotNull((data["id"] as? JsonPrimitive)?.contentOrNull) { "La respuesta no trae id." }
    }

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
            proposal =
                (data["propuesta"] as? JsonObject)?.let {
                    parseProposal(it, continues = (data["sigue"] as? JsonPrimitive)?.booleanOrNull == true)
                },
            remainingMessages = (data["quedan_mensajes"] as? JsonPrimitive)?.intOrNull ?: Int.MAX_VALUE,
        )
    }

    private fun parseProposal(
        proposal: JsonObject,
        continues: Boolean,
    ): AssistantProposal? {
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
                    hourlyPrice = group.text("precio_hora"),
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
        // Fotos para productos que ya existen: [{producto_id, nombre, foto}] (algunas traen tipo 'foto_producto').
        val productPhotos =
            (proposal["fotos_productos"] as? JsonArray).orEmpty().mapNotNull { item ->
                val photo = item as? JsonObject ?: return@mapNotNull null
                val type = photo.text("tipo")
                if (type != null && type != "foto_producto") return@mapNotNull null
                AssistantProductPhoto(
                    productId = (photo["producto_id"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null,
                    name = photo.text("nombre") ?: "Producto",
                    photoId = photo.text("foto") ?: return@mapNotNull null,
                )
            }
        val supplies =
            (proposal["insumos"] as? JsonArray).orEmpty().mapNotNull { item ->
                val supply = item as? JsonObject ?: return@mapNotNull null
                AssistantSupply(
                    name = supply.text("nombre") ?: return@mapNotNull null,
                    unit = supply.text("unidad") ?: return@mapNotNull null,
                    initialStock = supply.number("existencia_inicial"),
                    minimum = supply.number("minimo"),
                    unitCost = supply.number("costo_unitario"),
                )
            }
        val recipes =
            (proposal["recetas"] as? JsonArray).orEmpty().mapNotNull { item ->
                val recipe = item as? JsonObject ?: return@mapNotNull null
                val lines =
                    (recipe["lineas"] as? JsonArray).orEmpty().mapNotNull { raw ->
                        val line = raw as? JsonObject ?: return@mapNotNull null
                        AssistantRecipeLine(
                            supplyId = (line["insumo_id"] as? JsonPrimitive)?.longOrNull,
                            supplyName = line.text("insumo") ?: return@mapNotNull null,
                            unit = line.text("unidad") ?: return@mapNotNull null,
                            quantity = line.number("cantidad")?.takeIf { it > 0 } ?: return@mapNotNull null,
                        )
                    }
                if (lines.isEmpty()) return@mapNotNull null
                AssistantRecipe(
                    productId = (recipe["producto_id"] as? JsonPrimitive)?.intOrNull,
                    productName = recipe.text("producto") ?: return@mapNotNull null,
                    lines = lines,
                )
            }
        val courtHours =
            (proposal["horario_pistas"] as? JsonObject)?.let { hours ->
                val opening = hours.text("apertura")
                val closing = hours.text("cierre")
                if (opening == null || closing == null) {
                    null
                } else {
                    AssistantCourtHours(
                        opening = opening,
                        closing = closing,
                        weekDays =
                            (hours["dias_semana"] as? JsonArray)?.mapNotNull {
                                (it as? JsonPrimitive)?.intOrNull
                            },
                        durations =
                            (hours["duraciones_min"] as? JsonArray)?.mapNotNull {
                                (it as? JsonPrimitive)
                                    ?.intOrNull
                            },
                    )
                }
            }
        val movements =
            (proposal["movimientos"] as? JsonArray).orEmpty().mapNotNull { item ->
                val movement = item as? JsonObject ?: return@mapNotNull null
                AssistantMovement(
                    supplyId = (movement["insumo_id"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null,
                    supplyName = movement.text("insumo") ?: return@mapNotNull null,
                    unit = movement.text("unidad") ?: return@mapNotNull null,
                    type = movement.text("tipo")?.takeIf { it in MOVEMENT_TYPES } ?: return@mapNotNull null,
                    quantity = movement.number("cantidad")?.takeIf { it >= 0 } ?: return@mapNotNull null,
                    unitCost = movement.number("costo_unitario"),
                    note = movement.text("nota"),
                )
            }
        val productEdits =
            (proposal["cambios_productos"] as? JsonArray).orEmpty().mapNotNull { item ->
                val edit = item as? JsonObject ?: return@mapNotNull null
                AssistantProductEdit(
                    productId = (edit["producto_id"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null,
                    name = edit.text("producto") ?: return@mapNotNull null,
                    price = edit.text("precio"),
                    description = edit.text("descripcion"),
                    setsDescription = edit.containsKey("descripcion"),
                    available = (edit["disponible"] as? JsonPrimitive)?.booleanOrNull,
                    archive = (edit["archivar"] as? JsonPrimitive)?.booleanOrNull == true,
                )
            }
        val supplyEdits =
            (proposal["cambios_insumos"] as? JsonArray).orEmpty().mapNotNull { item ->
                val edit = item as? JsonObject ?: return@mapNotNull null
                AssistantSupplyEdit(
                    supplyId = (edit["insumo_id"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null,
                    name = edit.text("insumo") ?: return@mapNotNull null,
                    minimum = edit.number("minimo"),
                    unitCost = edit.number("costo_unitario"),
                    // Presente en null lo borra (""); sin la clave no se toca (null).
                    supplierName = if (edit.containsKey("proveedor_nombre")) edit.text("proveedor_nombre").orEmpty() else null,
                    supplierPhone =
                        if (edit.containsKey("proveedor_telefono")) edit.text("proveedor_telefono").orEmpty() else null,
                )
            }
        val spaceEdits =
            (proposal["cambios_espacios"] as? JsonArray).orEmpty().mapNotNull { item ->
                val edit = item as? JsonObject ?: return@mapNotNull null
                AssistantSpaceEdit(
                    spaceId = (edit["espacio_id"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null,
                    name = edit.text("espacio") ?: return@mapNotNull null,
                    hourlyPrice = edit.text("precio_hora"),
                    setsHourlyPrice = edit.containsKey("precio_hora"),
                    active = (edit["activo"] as? JsonPrimitive)?.booleanOrNull,
                )
            }
        val roleChanges =
            (proposal["cambios_personal"] as? JsonArray).orEmpty().mapNotNull { item ->
                val change = item as? JsonObject ?: return@mapNotNull null
                AssistantRoleChange(
                    membershipId = change.text("membresia_id") ?: return@mapNotNull null,
                    person = change.text("persona") ?: "Alguien del equipo",
                    currentRole = change.text("rol_actual") ?: return@mapNotNull null,
                    role = change.text("rol") ?: return@mapNotNull null,
                )
            }
        val bookings =
            (proposal["reservas"] as? JsonArray).orEmpty().mapNotNull { item ->
                val booking = item as? JsonObject ?: return@mapNotNull null
                AssistantBooking(
                    courtId = (booking["espacio_id"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null,
                    court = booking.text("espacio") ?: return@mapNotNull null,
                    start = booking.text("inicio") ?: return@mapNotNull null,
                    localStart = booking.text("inicio_local") ?: booking.text("inicio")!!,
                    durationMinutes = (booking["duracion_min"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null,
                    customerName = booking.text("nombre_cliente"),
                    players = (booking["jugadores"] as? JsonPrimitive)?.intOrNull,
                )
            }
        val sectionReasons =            SECTIONS.mapNotNull { section -> reasons?.text(section)?.let { section to it } }.toMap()
        val nothing =
            items.isEmpty() && spaces.isEmpty() && menu.isEmpty() && invites.isEmpty() && storePhotos.isEmpty()
        val dayToDay =
            movements.isEmpty() && productEdits.isEmpty() && supplyEdits.isEmpty() && spaceEdits.isEmpty() &&
                roleChanges.isEmpty() && bookings.isEmpty()
        if (nothing && productPhotos.isEmpty() && supplies.isEmpty() && recipes.isEmpty() && courtHours == null &&
            dayToDay
        ) {
            return null
        }
        return AssistantProposal(
            patchBody = changes.takeIf { it.isNotEmpty() }?.toString(),
            changes = items,
            spaces = spaces,
            menu = menu,
            invites = invites,
            storePhotos = storePhotos,
            productPhotos = productPhotos,
            sectionReasons = sectionReasons,
            supplies = supplies,
            recipes = recipes,
            courtHours = courtHours,
            movements = movements,
            productEdits = productEdits,
            supplyEdits = supplyEdits,
            spaceEdits = spaceEdits,
            roleChanges = roleChanges,
            bookings = bookings,
            continues = continues,
        )
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.contentOrNull
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    private fun JsonObject.number(key: String): Double? = (this[key] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()

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
        val SECTIONS =
            listOf(
                "espacios",
                "horario_pistas",
                "menu",
                "personal",
                "fotos",
                "fotos_productos",
                "insumos",
                "recetas",
                "movimientos",
                "cambios_productos",
                "cambios_insumos",
                "cambios_espacios",
                "cambios_personal",
                "reservas",
            )
        val MOVEMENT_TYPES = setOf("entrada", "merma", "conteo")
        const val DEFAULT_PREP_MINUTES = 10
    }
}
