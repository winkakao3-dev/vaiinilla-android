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
) {
    val productCount: Int get() = menu.sumOf { it.products.size }
}

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
}

/** Los pasos en el orden en que se aplican: primero ajustes y lugares, luego menú, fotos y al final el equipo. */
fun AssistantProposal.steps(): List<AssistantStep> =
    buildList {
        patchBody?.let { add(AssistantStep.Settings(it, changes.associate { c -> c.field to c.title })) }
        spaces.forEach { add(AssistantStep.Spaces(it)) }
        menu.forEach { add(AssistantStep.Menu(it)) }
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

/** Cómo se lee cada ajuste en la revisión; los títulos son los de Flujo de mi tienda. */
object AssistantLabels {
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
