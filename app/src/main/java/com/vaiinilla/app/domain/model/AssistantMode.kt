package com.vaiinilla.app.domain.model

/**
 * Qué asistentes de IA tiene una tienda (`asistente_modo`). BOTH: el de configurar la tienda (personal) y el de
 * pedidos para clientes. STAFF: solo el de configurar. NONE: ninguno. Lo cambian el dueño y Vaiinilla.
 */
enum class AssistantMode(
    val wire: String,
    val label: String,
) {
    BOTH("ambos", "Personal y clientes"),
    STAFF("personal", "Solo personal"),
    NONE("ninguno", "Apagado"),
    ;

    val forCustomers: Boolean get() = this == BOTH
    val forStaff: Boolean get() = this != NONE

    companion object {
        /** Desconocido o ausente (servidor anterior): los dos, como hasta ahora. */
        fun fromWire(value: String?): AssistantMode = entries.firstOrNull { it.wire == value } ?: BOTH
    }
}
