package com.vaiinilla.app.ui.assistant

import com.vaiinilla.app.domain.model.SpaceDelivery
import com.vaiinilla.app.domain.model.VenueKind

/** Lo que la app sabe de dónde y quién le pregunta al asistente; nada que identifique a la persona. */
data class AssistantPlaceFacts(
    val kind: VenueKind = VenueKind.STANDARD,
    val isSchool: Boolean = false,
    /** «Habitación 204», «Pista 3», «Mesa 5»; null si no está en un espacio. */
    val placeName: String? = null,
    val placeIsRoom: Boolean = false,
    val chargeToRoom: Boolean = false,
    val kitchenFrom: String? = null,
    val kitchenTo: String? = null,
    val payAtEnd: Boolean = false,
    /** «2 × Café americano». */
    val cart: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    /** Lo que le queda hoy del límite que puso su tutor, ya con formato («$40»). */
    val availableToday: String? = null,
    val hasGuardianBlocks: Boolean = false,
    /** «Restaurante con mesero», «Boliche»…; null si el servidor no lo manda. */
    val businessLabel: String? = null,
    /** Cómo llega lo que pide: a la mesa, en la barra o lo elige el cliente; null si no se sabe. */
    val delivery: SpaceDelivery? = null,
)

/**
 * El servidor solo le pasa al modelo el menú y las pistas, y no acepta otro campo: este contexto viaja como el primer
 * mensaje del cliente, oculto en la pantalla. Cabe en el tope de letras por mensaje y no lleva números largos, que el
 * servidor tacharía como teléfono.
 */
object AssistantPlaceNote {
    const val MAX_LETTERS = 600

    fun build(facts: AssistantPlaceFacts): String? {
        val parts = mutableListOf<String>()
        place(facts)?.let(parts::add)
        facts.businessLabel?.let { parts += "El negocio es: ${it.lowercase()}." }
        when (facts.delivery) {
            SpaceDelivery.TABLE -> parts += "Lo que pido me lo llevan a mi lugar."
            SpaceDelivery.BAR -> parts += "Lo que pido lo recojo en la barra."
            SpaceDelivery.BOTH -> parts += "Puedo elegir si me lo llevan o lo recojo en la barra."
            null -> Unit
        }
        if (facts.allergens.isNotEmpty()) {
            parts +=
                "Tengo alergia a: ${facts.allergens.joinToString(", ") { it.lowercase() }}. No me propongas nada que " +
                "los tenga ni productos sin alérgenos revisados, y avísame si pido uno."
        }
        if (facts.hasGuardianBlocks) parts += "Mi tutor me bloqueó algunos productos; si pido uno, la app no me deja."
        facts.availableToday?.let { parts += "Hoy me quedan $it de mi límite: no me armes un pedido que pase de eso." }
        if (facts.cart.isNotEmpty()) parts += "Ya tengo en el carrito: ${facts.cart.joinToString(", ")}."
        if (parts.isEmpty()) return null
        val header = "Contexto que manda la app (no lo repitas):"
        // Sin esta regla el modelo respondía otra pregunta y armaba un pedido con lo del carrito (Kao, 7 nov).
        val footer =
            "Contesta exactamente lo que te pregunte; no me armes ni cambies un pedido si no te lo pido."
        val body = StringBuilder(header)
        for (part in parts) {
            if (body.length + part.length + footer.length + 2 > MAX_LETTERS) continue
            body.append(' ').append(part)
        }
        return body.append(' ').append(footer).toString().take(MAX_LETTERS)
    }

    private fun place(facts: AssistantPlaceFacts): String? {
        val name = facts.placeName?.trim()?.takeIf(String::isNotEmpty)
        return when {
            facts.kind == VenueKind.HOTEL && facts.placeIsRoom ->
                buildString {
                    append("Estoy hospedado en un hotel, en ${name ?: "mi habitación"}.")
                    append(" Lo que pida me lo llevan a la habitación")
                    append(if (facts.chargeToRoom) " y se carga a mi cuenta del hotel." else ".")
                    append(" Para algo en persona (toallas, limpieza, un problema) me atiende recepción, no un mesero.")
                    if (facts.kitchenFrom != null && facts.kitchenTo != null) {
                        append(" La cocina recibe pedidos a habitaciones de ${facts.kitchenFrom} a ${facts.kitchenTo}.")
                    }
                }
            facts.kind == VenueKind.HOTEL ->
                "Estoy en el restaurante de un hotel${name?.let { ", en $it" } ?: ""}." + payAtEnd(facts)
            facts.kind == VenueKind.BOWLING ->
                "Estoy en un boliche${name?.let { ", en $it" } ?: ""}." + payAtEnd(facts)
            facts.isSchool -> "Soy alumno y pido en la cafetería de mi escuela."
            name != null -> "Estoy en $name." + payAtEnd(facts)
            else -> null
        }
    }

    private fun payAtEnd(facts: AssistantPlaceFacts): String =
        if (facts.payAtEnd) " Aquí se puede pagar al final con la cuenta abierta." else ""
}