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

/**
 * Lo que el asistente propone. [patchBody] es el JSON tal cual para `PATCH /establecimiento/configuracion`:
 * el servidor ya lo validó y solo trae lo que cambia.
 */
data class AssistantProposal(
    val patchBody: String,
    val changes: List<AssistantChange>,
)

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
            "barra_prepara" -> "La barra prepara pedidos"
            "cocina_prepara_sin_cobrar" -> "Cocina prepara antes de cobrar"
            "gracia_liberacion_min" -> "Tiempo de gracia"
            "franjas_pedido" -> "¿Cuándo se reciben pedidos?"
            "visible_en_directorio" -> "Aparecer en el directorio"
            "fondo_sugerido" -> "Fondo de caja sugerido"
            "modo_escuela" -> "Es una escuela"
            "dias_adelanto_pedido" -> "Días para apartar"
            "hora_corte_pedido" -> "Hora límite para apartar"
            "reparto_activo" -> "Entrega a domicilio"
            "reparto_costo" -> "Costo del envío"
            "reparto_minimo" -> "Compra mínima a domicilio"
            "permite_contra_entrega" -> "Pagar al recibir"
            "encargos_requieren_aceptar" -> "Aceptar cada encargo"
            "anticipo_porcentaje" -> "Anticipo de encargos"
            else -> field
        }

    fun yesNo(field: String, value: Boolean): String =
        when (field) {
            "permite_pago_al_final" -> if (value) "Al final, con cuenta abierta" else "Al pedir"
            "entrega_requiere_qr" -> if (value) "Escaneando su QR" else "Sin QR"
            else -> if (value) "Sí" else "No"
        }

    fun choice(field: String, value: String): String =
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

    fun number(field: String, value: Long): String =
        when (field) {
            "gracia_liberacion_min" -> "$value min"
            "dias_adelanto_pedido" -> if (value == 0L) "Solo para hoy" else "Hasta $value días antes"
            "anticipo_porcentaje" -> "$value %"
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
