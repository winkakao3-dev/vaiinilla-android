package com.vaiinilla.app.core.notifications

import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.Venue

/**
 * Qué es un aviso, quién lo provoca y a quién va, leído solo de lo que trae. Los textos siguen a
 * `notifications.service.js` del servidor (cada `alert` sale hacia un rol del negocio del pedido) y son los
 * mismos que `NoticeExplanation.swift` de iOS.
 */
data class NoticeExplanation(
    val kind: String,
    val what: String,
    val trigger: String,
    val audience: String,
    /** Los modos a los que el servidor manda este aviso. Vacío cuando va a una persona y no a un modo. */
    val roles: List<OperationalRole>,
    val orderId: String?,
    val establishmentId: String?,
    /** El `alert` de los avisos al personal; vacío en los demás. */
    val alert: String = "",
) {
    /**
     * El aviso al personal pide algo que el pedido todavía no tiene: un «por cobrar» sigue por cobrar, una comanda
     * sigue sin estar lista, etc. Sin el pedido leído no se sabe, así que no cuenta como pendiente.
     */
    fun isPending(lookup: NoticeLookup?): Boolean {
        if (roles.isEmpty() || lookup !is NoticeLookup.Found) return false
        val state = lookup.order.stateWire ?: return false
        return when (alert) {
            "por_cobrar" -> state == OrderState.PENDING_PAYMENT.wireValue
            "nueva_comanda" -> state == OrderState.PAID.wireValue || state == OrderState.PREPARING.wireValue
            "listo_mesero", "listo_barra" -> state == OrderState.READY.wireValue
            "por_aceptar" -> state == OrderState.AWAITING_ACCEPTANCE.wireValue
            "pedido_domicilio" -> state !in FINISHED
            else -> false
        }
    }

    /** Por qué este teléfono recibió el aviso, comparando sus modos con los modos a los que va. */
    fun reason(
        memberships: List<NoticeMembership>,
        openMode: String?,
    ): String {
        if (roles.isEmpty()) {
            return "Va a una persona y no a un modo: llega a todos los teléfonos donde esa cuenta tiene sesión."
        }
        val matching = memberships.filter { it.role in roles }
        val modes = roles.joinToString(" o ") { Venue.roleLabel(it) }
        if (matching.isEmpty()) {
            return "Esta cuenta no tiene $modes en ningún negocio ahora. Pudo tenerlo cuando llegó el aviso."
        }
        val places = matching.joinToString(", ") { "${Venue.roleLabel(it.role, it.isSchool)} en ${it.establishmentName}" }
        val open = openMode?.let { " La app estaba en «$it»." }.orEmpty()
        return "Esta cuenta tiene $places. El servidor manda el aviso a cada teléfono de las cuentas con $modes " +
            "en el negocio del pedido, aunque la app esté en otro modo o en otro negocio.$open"
    }

    companion object {
        private const val MESA_O_ESPACIO = "mesa o espacio"

        private val FINISHED =
            setOf(
                OrderState.DELIVERED,
                OrderState.CANCELED,
                OrderState.NOT_PICKED_UP,
                OrderState.EXPIRED,
            ).map { it.wireValue }.toSet()

        fun explain(
            payload: Map<String, String>,
            isRemote: Boolean,
        ): NoticeExplanation {
            val type = payload["type"].orEmpty()
            val alert = payload["alert"].orEmpty()
            val orderId = payload["orderId"] ?: payload["order_id"]
            val establishmentId = payload["establecimientoId"]
            val space = payload["espacio_nombre"] ?: payload["espacioNombre"] ?: "Un espacio"

            fun make(
                kind: String,
                what: String,
                trigger: String,
                audience: String,
                roles: List<OperationalRole>,
            ) = NoticeExplanation(
                kind,
                what,
                trigger,
                audience,
                roles,
                orderId,
                establishmentId,
                alert = if (type == "staff_alert") alert else "",
            )

            return when {
                type == "staff_alert" && alert == "por_cobrar" ->
                    make(
                        "Caja · por cobrar",
                        "Entró un pedido en efectivo y Caja tiene que cobrarlo.",
                        "La persona que hizo el pedido.",
                        "Caja del negocio del pedido.",
                        listOf(OperationalRole.CASHIER),
                    )
                type == "staff_alert" && alert == "nueva_comanda" && payload["station"] == "caja" ->
                    make(
                        "Barra · comanda",
                        "Un pedido ya cobrado tiene productos que prepara la barra.",
                        "Caja o el pago con tarjeta o saldo, al quedar cobrado.",
                        "Caja del negocio del pedido, porque ahí está la barra.",
                        listOf(OperationalRole.CASHIER),
                    )
                type == "staff_alert" && alert == "nueva_comanda" ->
                    make(
                        "Cocina · comanda",
                        "Un pedido ya cobrado está listo para prepararse.",
                        "Caja o el pago con tarjeta o saldo, al quedar cobrado.",
                        "Cocina del negocio del pedido.",
                        listOf(OperationalRole.KITCHEN),
                    )
                type == "staff_alert" && alert == "listo_mesero" ->
                    make(
                        "Mesero · listo",
                        "Un pedido está listo para llevarlo a su ${if (Venue.isHotel) Venue.room else MESA_O_ESPACIO}.",
                        "Cocina o Caja, al marcarlo listo.",
                        "Meseros del negocio del pedido.",
                        listOf(OperationalRole.WAITER),
                    )
                type == "staff_alert" && alert == "listo_barra" ->
                    make(
                        "Caja · pasa por él",
                        "Un pedido está listo y el cliente pasa a recogerlo a la barra.",
                        "Cocina o Caja, al marcarlo listo.",
                        "Caja del negocio del pedido.",
                        listOf(OperationalRole.CASHIER),
                    )
                type == "staff_alert" && alert == "llamada_mesa" ->
                    make(
                        "Mesero · llamada",
                        "$space llama a un mesero.",
                        "La persona ${if (Venue.isHotel) "hospedada" else "sentada"} en $space.",
                        "Meseros del negocio de ese espacio.",
                        listOf(OperationalRole.WAITER),
                    )
                type == "staff_alert" && alert == "turno_por_terminar" ->
                    make(
                        "Turno por terminar",
                        "El tiempo rentado de $space está por terminar o ya terminó.",
                        "El servidor, al acercarse la hora de fin.",
                        "Meseros y Caja del negocio de ese espacio.",
                        listOf(OperationalRole.WAITER, OperationalRole.CASHIER),
                    )
                type == "staff_alert" && alert == "por_aceptar" ->
                    make(
                        "Admin · encargo",
                        "Llegó un encargo que el negocio tiene que aceptar o rechazar.",
                        "La persona que lo encargó.",
                        "Administración del negocio.",
                        listOf(OperationalRole.ADMIN),
                    )
                type == "staff_alert" && alert == "pedido_domicilio" ->
                    make(
                        "Admin · a domicilio",
                        "Entró un pedido para entregar a domicilio.",
                        "La persona que lo pidió.",
                        "Administración del negocio.",
                        listOf(OperationalRole.ADMIN),
                    )
                type == "order_state" && payload["dependienteUsuarioId"] != null ->
                    make(
                        "Pedido de tu hijo o hija",
                        "Avanzó un pedido de alguien de quien eres tutor${stateSuffix(payload)}.",
                        "El negocio, al mover el pedido.",
                        "Los tutores de quien hizo el pedido.",
                        emptyList(),
                    )
                type == "order_state" ->
                    make(
                        "Tu pedido",
                        "Avanzó un pedido hecho con esta cuenta${stateSuffix(payload)}.",
                        if (isRemote) {
                            "El negocio, al mover el pedido."
                        } else {
                            "Esta app, al ver el cambio de tu pedido abierto. No vino del servidor."
                        },
                        "La cuenta que hizo el pedido.",
                        emptyList(),
                    )
                type == "saldo_bajo" ->
                    make(
                        "Saldo bajo",
                        "El saldo de alguien de quien eres tutor bajó del límite que pusiste" +
                            "${payload["saldo"]?.let { ": le quedan $$it" }.orEmpty()}.",
                        "Su último pedido pagado con saldo.",
                        "Los tutores de esa persona.",
                        emptyList(),
                    )
                type == "recarga_acreditada" ->
                    make(
                        "Recarga",
                        "Se abonó una recarga de saldo.",
                        "Caja o el pago con tarjeta, al acreditarla.",
                        "La dueña o el dueño de la cartera y sus tutores.",
                        emptyList(),
                    )
                type == "mesero_en_camino" ->
                    make(
                        "Mesero en camino",
                        "Tu mesero va a ${SpaceCopy.yourPlace(null)}.",
                        "El mesero, al tomar tu llamada.",
                        "La persona que llamó al mesero.",
                        emptyList(),
                    )
                else ->
                    make(
                        "Otro aviso",
                        "Un aviso que esta versión de la app no reconoce. Abajo están todos sus datos.",
                        if (isRemote) "El servidor de Vaiinilla." else "Esta app.",
                        "No se sabe.",
                        emptyList(),
                    )
            }
        }

        private fun stateSuffix(payload: Map<String, String>): String {
            val state = payload["state"] ?: return ""
            val label = runCatching { OrderState.fromWireValue(state).label }.getOrDefault(state)
            return ": ahora está «$label»"
        }
    }
}
