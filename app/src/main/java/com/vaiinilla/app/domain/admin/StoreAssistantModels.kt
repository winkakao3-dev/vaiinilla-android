package com.vaiinilla.app.domain.admin

import com.vaiinilla.app.domain.model.OrderWindow

/** Un mensaje de la plática con el asistente de configuración (marco AS2). */
data class AssistantMessage(
    val fromOwner: Boolean,
    val text: String,
)

/** Un ajuste que propone el asistente, ya en palabras para el dueño (marco AS3). */
data class AssistantChange(
    val field: String,
    val title: String,
    val value: String,
    val reason: String?,
)

/** Mesas o lugares que el asistente propone crear: "[prefix] 1" a "[prefix] [count]", cada uno con su QR. */
data class AssistantSpaceGroup(
    val type: String,
    val prefix: String,
    val count: Int,
    /** Solo canchas y pistas: lo que se cobra por hora de renta ("350.00"). */
    val hourlyPrice: String? = null,
)

/** Un producto del menú propuesto. [photoId] es la foto del chat que lleva ("foto2"), si tiene. */
data class AssistantProduct(
    val name: String,
    val price: String,
    val description: String?,
    val station: String,
    val photoId: String?,
)

/** Una categoría del menú propuesto; [existingId] cuando la tienda ya la tiene y solo se agregan productos. */
data class AssistantCategory(
    val name: String,
    val existingId: Int?,
    val products: List<AssistantProduct>,
)

/**
 * Una foto del chat para un producto que la tienda ya tiene ([productId]); [name] es su nombre para
 * enseñarlo en la revisión. Se sube con `PUT catalogo/productos/{id}/imagen`.
 */
data class AssistantProductPhoto(
    val productId: Long,
    val name: String,
    val photoId: String,
)

/** Una invitación al personal. */
data class AssistantInvite(
    val email: String,
    val role: String,
)

/** Horario para rentar pistas o canchas; [weekDays] y [durations] en null dejan los que ya tiene. */
data class AssistantCourtHours(
    val opening: String,
    val closing: String,
    val weekDays: List<Int>?,
    val durations: List<Int>?,
)

/** Un insumo nuevo del inventario; [unit] es la unidad como la manda el servidor ("ml", "pieza"…). */
data class AssistantSupply(
    val name: String,
    val unit: String,
    val initialStock: Double?,
    val minimum: Double?,
    val unitCost: Double?,
)

/** Una línea de receta; [supplyId] es null cuando el insumo viene nuevo en la misma propuesta. */
data class AssistantRecipeLine(
    val supplyId: Long?,
    val supplyName: String,
    val unit: String,
    val quantity: Double,
)

/**
 * Lo que gasta una venta de un producto. [productId] es null si el producto viene nuevo en el menú de la misma
 * propuesta. Al guardar solo se agregan o cambian estas líneas; lo demás de la receta se queda.
 */
data class AssistantRecipe(
    val productId: Int?,
    val productName: String,
    val lines: List<AssistantRecipeLine>,
)

/** Una entrada, merma o conteo de un insumo que ya existe ([type]: "entrada", "merma" o "conteo"). */
data class AssistantMovement(
    val supplyId: Long,
    val supplyName: String,
    val unit: String,
    val type: String,
    val quantity: Double,
    val unitCost: Double?,
    val note: String?,
)

/**
 * Lo que cambia de un producto que ya existe; null no se toca. [archive] lo quita del menú y deja fuera lo demás.
 * [description] con [setsDescription] en true y null la borra.
 */
data class AssistantProductEdit(
    val productId: Int,
    val name: String,
    val price: String? = null,
    val description: String? = null,
    val setsDescription: Boolean = false,
    val available: Boolean? = null,
    val archive: Boolean = false,
)

/** Lo que cambia de un insumo que ya existe; null no se toca y "" borra el proveedor o su teléfono. */
data class AssistantSupplyEdit(
    val supplyId: Long,
    val name: String,
    val minimum: Double? = null,
    val unitCost: Double? = null,
    val supplierName: String? = null,
    val supplierPhone: String? = null,
)

/** Lo que cambia de una mesa o lugar; [hourlyPrice] con [setsHourlyPrice] y null lo deja sin renta. */
data class AssistantSpaceEdit(
    val spaceId: Int,
    val name: String,
    val hourlyPrice: String? = null,
    val setsHourlyPrice: Boolean = false,
    val active: Boolean? = null,
)

/** Cambiar el puesto de alguien del equipo; [person] es su nombre de pila. */
data class AssistantRoleChange(
    val membershipId: String,
    val person: String,
    val currentRole: String,
    val role: String,
)

/** Una renta de pista o cancha: [start] en UTC (ISO) y [localStart] como la dijo el dueño ("2026-11-09T18:00"). */
data class AssistantBooking(
    val courtId: Int,
    val court: String,
    val start: String,
    val localStart: String,
    val durationMinutes: Int,
    val customerName: String?,
    val players: Int?,
)

/**
 * Lo que el asistente propone. [patchBody] es el JSON tal cual para `PATCH /establecimiento/configuracion`
 * (null si no cambia ningún ajuste): el servidor ya lo validó y solo trae lo que cambia. Lo demás se crea con
 * las rutas de siempre al aceptar. [storePhotos] va de "logo" o "foto" al id de la foto del chat.
 */
data class AssistantProposal(
    val patchBody: String?,
    val changes: List<AssistantChange>,
    val spaces: List<AssistantSpaceGroup> = emptyList(),
    val menu: List<AssistantCategory> = emptyList(),
    val invites: List<AssistantInvite> = emptyList(),
    val storePhotos: Map<String, String> = emptyMap(),
    val productPhotos: List<AssistantProductPhoto> = emptyList(),
    val sectionReasons: Map<String, String> = emptyMap(),
    val supplies: List<AssistantSupply> = emptyList(),
    val recipes: List<AssistantRecipe> = emptyList(),
    val courtHours: AssistantCourtHours? = null,
    val movements: List<AssistantMovement> = emptyList(),
    val productEdits: List<AssistantProductEdit> = emptyList(),
    val supplyEdits: List<AssistantSupplyEdit> = emptyList(),
    val spaceEdits: List<AssistantSpaceEdit> = emptyList(),
    val roleChanges: List<AssistantRoleChange> = emptyList(),
    val bookings: List<AssistantBooking> = emptyList(),
    val orderCancels: List<AssistantOrderCancel> = emptyList(),
    /** El asistente todavía tiene algo más que proponer después de esta ("sigue"). */
    val continues: Boolean = false,
) {
    val productCount: Int get() = menu.sumOf { it.products.size }
}

/** Un Excel (.xlsx) que el dueño mandó en el chat («excel1»); va completo en cada mensaje, como las fotos. */
class AssistantSheet(
    val id: String,
    val name: String,
    val bytes: ByteArray,
)

/** Un pedido atorado que se cancela: por cobrar y sin pago, así que no se devuelve ni se cobra nada. */
data class AssistantOrderCancel(
    val orderId: String,
    val folio: Int,
    val version: Int,
    val reason: String,
)

/** Una foto que el dueño mandó en el chat, ya lista para subir como producto o como logo. */
class AssistantPhoto(
    val id: String,
    val image: AssistantUpload,
    val logo: AssistantUpload,
)

class AssistantUpload(
    val bytes: ByteArray,
    val filename: String,
    val mimeType: String,
)

/** Un paso al aplicar la propuesta; cada uno con su propia llave de idempotencia para poder reintentar. */
sealed interface AssistantStep {
    val label: String

    data class Settings(
        val patchBody: String,
        /** Nombre de cada campo para decir cuál no se pudo guardar. */
        val titles: Map<String, String> = emptyMap(),
    ) : AssistantStep {
        override val label = "Ajustes de la tienda"
    }

    data class Spaces(
        val group: AssistantSpaceGroup,
    ) : AssistantStep {
        override val label = "${group.prefix} 1 a ${group.prefix} ${group.count}"
    }

    data class Menu(
        val category: AssistantCategory,
    ) : AssistantStep {
        override val label = "Menú: ${category.name}"
    }

    data class StorePhoto(
        val kind: String,
        val photoId: String,
    ) : AssistantStep {
        override val label = if (kind == "logo") "Logo" else "Foto del local"
    }

    data class ProductPhoto(
        val photo: AssistantProductPhoto,
    ) : AssistantStep {
        override val label = "Foto de ${photo.name}"
    }

    data class Invite(
        val invite: AssistantInvite,
    ) : AssistantStep {
        override val label = "Invitar a ${invite.email}"
    }

    data class CourtHours(
        val hours: AssistantCourtHours,
    ) : AssistantStep {
        override val label = "Horario de las pistas"
    }

    data class Supply(
        val supply: AssistantSupply,
    ) : AssistantStep {
        override val label = "Insumo: ${supply.name}"
    }

    data class Recipe(
        val recipe: AssistantRecipe,
    ) : AssistantStep {
        override val label = "Receta de ${recipe.productName}"
    }

    data class Movement(
        val movement: AssistantMovement,
    ) : AssistantStep {
        override val label = "${movement.supplyName}: ${AssistantLabels.movementKind(movement.type)}"
    }

    data class ProductEdit(
        val edit: AssistantProductEdit,
    ) : AssistantStep {
        override val label = "Cambios a ${edit.name}"
    }

    data class SupplyEdit(
        val edit: AssistantSupplyEdit,
    ) : AssistantStep {
        override val label = "Cambios a ${edit.name}"
    }

    data class SpaceEdit(
        val edit: AssistantSpaceEdit,
    ) : AssistantStep {
        override val label = "Cambios a ${edit.name}"
    }

    data class RoleChange(
        val change: AssistantRoleChange,
    ) : AssistantStep {
        override val label = "Puesto de ${change.person}"
    }

    data class Booking(
        val booking: AssistantBooking,
    ) : AssistantStep {
        override val label = "Renta de ${booking.court}"
    }

    data class OrderCancel(
        val cancel: AssistantOrderCancel,
    ) : AssistantStep {
        override val label = "Cancelar el pedido #${cancel.folio}"
    }
}

/** Los pasos en el orden en que se aplican: primero ajustes y lugares, luego menú, fotos y al final el equipo. */
fun AssistantProposal.steps(): List<AssistantStep> =
    buildList {
        patchBody?.let { add(AssistantStep.Settings(it, changes.associate { c -> c.field to c.title })) }
        spaces.forEach { add(AssistantStep.Spaces(it)) }
        courtHours?.let { add(AssistantStep.CourtHours(it)) }
        menu.forEach { add(AssistantStep.Menu(it)) }
        // Las recetas van después del menú y de los insumos: pueden usar productos e insumos recién creados.
        supplies.forEach { add(AssistantStep.Supply(it)) }
        recipes.forEach { add(AssistantStep.Recipe(it)) }
        // Las entradas y conteos van después de dar de alta los insumos nuevos.
        movements.forEach { add(AssistantStep.Movement(it)) }
        supplyEdits.forEach { add(AssistantStep.SupplyEdit(it)) }
        productEdits.forEach { add(AssistantStep.ProductEdit(it)) }
        spaceEdits.forEach { add(AssistantStep.SpaceEdit(it)) }
        bookings.forEach { add(AssistantStep.Booking(it)) }
        roleChanges.forEach { add(AssistantStep.RoleChange(it)) }
        orderCancels.forEach { add(AssistantStep.OrderCancel(it)) }
        productPhotos.forEach { add(AssistantStep.ProductPhoto(it)) }
        storePhotos.forEach { (kind, id) -> add(AssistantStep.StorePhoto(kind, id)) }
        invites.forEach { add(AssistantStep.Invite(it)) }
    }

/** La respuesta del asistente: su mensaje, respuestas rápidas y, si ya la tiene, su propuesta. */
data class AssistantReply(
    val message: String,
    val options: List<String>,
    val proposal: AssistantProposal?,
    val remainingMessages: Int,
)

/** Cómo va una respuesta del asistente que el servidor arma en segundo plano. */
sealed interface AssistantJob {
    data object Thinking : AssistantJob

    data class Done(
        val reply: AssistantReply,
    ) : AssistantJob

    data class Failed(
        val code: String,
        val message: String,
    ) : AssistantJob
}

/** Cómo se lee cada ajuste en la revisión; los títulos son los de Flujo de mi tienda. */
object AssistantLabels {
    private val unitShort =
        mapOf(
            "pieza" to "pz",
            "botella" to "bot",
            "ml" to "ml",
            "l" to "L",
            "g" to "g",
            "kg" to "kg",
            "oz" to "oz",
            "porcion" to "porc",
        )
    private val unitLong =
        mapOf(
            "pieza" to "piezas",
            "botella" to "botellas",
            "ml" to "mililitros",
            "l" to "litros",
            "g" to "gramos",
            "kg" to "kilos",
            "oz" to "onzas",
            "porcion" to "porciones",
        )

    /** "120 ml", "1.5 kg", "3,000 ml". */
    fun amount(
        quantity: Double,
        unit: String,
    ): String {
        val number =
            if (quantity % 1.0 == 0.0) {
                "%,d".format(java.util.Locale.US, quantity.toLong())
            } else {
                "%,.3f".format(java.util.Locale.US, quantity).trimEnd('0').trimEnd('.')
            }
        return "$number ${unitShort[unit] ?: unit}"
    }

    fun supply(supply: AssistantSupply): String =
        listOfNotNull(
            "En ${unitLong[supply.unit] ?: supply.unit}",
            supply.initialStock?.let { "tienes ${amount(it, supply.unit)}" },
            supply.minimum?.takeIf { it > 0 }?.let { "te avisa con ${amount(it, supply.unit)}" },
            supply.unitCost?.takeIf { it > 0 }?.let { "cuesta ${money("%.2f".format(java.util.Locale.US, it))} c/u" },
        ).joinToString(" · ")

    /** "15:00 a 03:00 · todos los días · 1 h, 2 h". */
    fun courtHours(hours: AssistantCourtHours): String {
        val days =
            hours.weekDays?.let { list ->
                if (list.size ==
                    7
                ) {
                    "todos los días"
                } else {
                    list.joinToString(", ") { WEEK_DAYS.getOrElse(it - 1) { "$it" } }
                }
            }
        val durations =
            hours.durations?.joinToString(", ") { if (it % 60 == 0) "${it / 60} h" else "${it / 60} h ${it % 60} min" }
        return listOfNotNull("${hours.opening} a ${hours.closing}", days, durations).joinToString(" · ")
    }

    private val WEEK_DAYS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

    fun movementKind(type: String): String =
        when (type) {
            "entrada" -> "entrada"
            "merma" -> "merma"
            "conteo" -> "conteo"
            else -> type
        }

    /** "Llegaron 1,000 kg · a $28.00 c/u", "Se tiraron 3 pz", "Contaste 5 pz". */
    fun movement(movement: AssistantMovement): String {
        val amount = amount(movement.quantity, movement.unit)
        val what =
            when (movement.type) {
                "entrada" -> "Llegaron $amount"
                "merma" -> "Se tiraron $amount"
                "conteo" -> "Contaste $amount"
                else -> amount
            }
        return listOfNotNull(
            what,
            movement.unitCost?.takeIf { it > 0 }?.let { "a ${money("%.2f".format(java.util.Locale.US, it))} c/u" },
            movement.note,
        ).joinToString(" · ")
    }

    fun productEdit(edit: AssistantProductEdit): String {
        if (edit.archive) return "Se quita del menú; sus ventas se quedan"
        return listOfNotNull(
            edit.price?.let { "Precio ${money(it)}" },
            edit.available?.let { if (it) "Se vuelve a vender" else "Agotado" },
            if (edit.setsDescription) edit.description?.let { "Descripción: $it" } ?: "Sin descripción" else null,
        ).joinToString(" · ")
    }

    fun supplyEdit(
        edit: AssistantSupplyEdit,
        unit: String? = null,
    ): String =
        listOfNotNull(
            edit.minimum?.let { "Te avisa con ${if (unit != null) amount(it, unit) else formatNumber(it)}" },
            edit.unitCost?.let { "Cuesta ${money("%.2f".format(java.util.Locale.US, it))} c/u" },
            edit.supplierName?.let { if (it.isEmpty()) "Sin proveedor" else "Proveedor: $it" },
            edit.supplierPhone?.let { if (it.isEmpty()) "Sin teléfono" else "Tel. $it" },
        ).joinToString(" · ")

    fun spaceEdit(edit: AssistantSpaceEdit): String =
        listOfNotNull(
            if (edit.setsHourlyPrice) edit.hourlyPrice?.let { "${money(it)} la hora" } ?: "Ya no se renta" else null,
            edit.active?.let { if (it) "Se prende" else "Se apaga" },
        ).joinToString(" · ")

    fun roleChange(change: AssistantRoleChange): String = "${role(change.currentRole)} → ${role(change.role)}"

    /** "Lun 9 nov, 18:00 · 2 h · Familia Pérez · 6 jugadores". */
    fun booking(booking: AssistantBooking): String {
        val date =
            runCatching {
                val local = java.time.LocalDateTime.parse(booking.localStart)
                val day = WEEK_DAYS[local.dayOfWeek.value - 1].replaceFirstChar { it.uppercase() }
                "$day ${local.dayOfMonth} ${MONTHS[local.monthValue - 1]}, %02d:%02d".format(local.hour, local.minute)
            }.getOrDefault(booking.localStart)
        val minutes = booking.durationMinutes
        val duration = if (minutes % 60 == 0) "${minutes / 60} h" else "${minutes / 60} h ${minutes % 60} min"
        return listOfNotNull(
            date,
            duration,
            booking.customerName,
            booking.players?.let { if (it == 1) "1 jugador" else "$it jugadores" },
        ).joinToString(" · ")
    }

    private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) "%,d".format(java.util.Locale.US, value.toLong()) else "$value"

    fun recipe(recipe: AssistantRecipe): String =
        recipe.lines.joinToString(" · ") { "${it.supplyName} ${amount(it.quantity, it.unit)}" }

    fun title(field: String): String =
        when (field) {
            "tipo" -> "Tipo de negocio"
            "permite_pago_al_final" -> "¿Cuándo pagan?"
            "entrega_en_espacio" -> "En mesa o lugar, ¿cómo les llega?"
            "entrega_requiere_qr" -> "¿Cómo se entrega lo de llevar?"
            "permite_saldo" -> "Pagar con saldo"
            "permite_pago_en_caja" -> "Pago en caja"
            "barra_prepara" -> "La barra prepara pedidos"
            "cocina_prepara_sin_cobrar" -> "Cocina prepara antes de cobrar"
            "gracia_liberacion_min" -> "Tiempo de gracia"
            "franjas_pedido" -> "¿Cuándo se reciben pedidos?"
            "visible_en_directorio" -> "Aparecer en el directorio"
            "fondo_sugerido" -> "Fondo de caja sugerido"
            "modo_escuela" -> "Es una escuela"
            "dias_adelanto_pedido" -> "Días para apartar"
            "hora_corte_pedido" -> "Hora límite para apartar"
            "hora_corte_cancelacion" -> "Hora límite para cancelar"
            "union_requiere_codigo" -> "Pedir código para unirse"
            "tutores_requieren_aprobacion" -> "Aprobar a cada tutor"
            "recarga_minima" -> "Recarga mínima"
            "recarga_maxima" -> "Recarga máxima"
            "encargos_minutos_aceptar" -> "Tiempo para aceptar encargos"
            "reparto_activo" -> "Entrega a domicilio"
            "reparto_costo" -> "Costo del envío"
            "reparto_minimo" -> "Compra mínima a domicilio"
            "permite_contra_entrega" -> "Pagar al recibir"
            "encargos_requieren_aceptar" -> "Aceptar cada encargo"
            "anticipo_porcentaje" -> "Anticipo de encargos"
            else -> field
        }

    fun spaceType(type: String): String =
        when (type) {
            "mesa" -> "Mesas"
            "barra" -> "Lugares en barra"
            "cancha" -> "Canchas o pistas"
            "asiento" -> "Asientos"
            "drive_thru" -> "Carriles de autoservicio"
            else -> type
        }

    fun station(station: String): String =
        when (station) {
            "cocina" -> "Lo prepara cocina"
            "caja" -> "Lo prepara la barra"
            else -> "Ya está listo"
        }

    fun role(role: String): String =
        when (role) {
            "cajero" -> "Caja"
            "cocina" -> "Cocina"
            "mesero" -> "Mesero"
            "admin" -> "Administrador"
            else -> role
        }

    fun yesNo(
        field: String,
        value: Boolean,
    ): String =
        when (field) {
            "permite_pago_al_final" -> if (value) "Al final, con cuenta abierta" else "Al pedir"
            "entrega_requiere_qr" -> if (value) "Escaneando su QR" else "Sin QR"
            else -> if (value) "Sí" else "No"
        }

    fun choice(
        field: String,
        value: String,
    ): String =
        when (field) {
            "tipo" -> BusinessType.fromWire(value)?.label ?: value
            "entrega_en_espacio" ->
                when (value) {
                    "mesa" -> "Te lo llevamos"
                    "barra" -> "Pasa por él"
                    "ambas" -> "Las dos: el cliente elige"
                    else -> value
                }
            else -> value
        }

    fun number(
        field: String,
        value: Long,
    ): String =
        when (field) {
            "gracia_liberacion_min" -> "$value min"
            "dias_adelanto_pedido" -> if (value == 0L) "Solo para hoy" else "Hasta $value días antes"
            "anticipo_porcentaje" -> "$value %"
            "encargos_minutos_aceptar" -> "$value min"
            else -> "$value"
        }

    fun money(value: String): String = "$$value"

    fun windows(windows: List<OrderWindow>): String =
        if (windows.isEmpty()) {
            "Siempre que la tienda esté abierta"
        } else {
            windows.joinToString(" · ") { "${it.from} a ${it.to}" }
        }
}
