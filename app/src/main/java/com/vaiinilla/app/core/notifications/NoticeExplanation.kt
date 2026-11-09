package com.vaiinilla.app.core.notifications

import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.VenueContext

/**
 * Qué es un aviso, leído solo de lo que trae. Los textos siguen a
 * `notifications.service.js` del servidor (cada `alert` sale hacia un rol del negocio del pedido) y son los
 * mismos que `NoticeExplanation.swift` de iOS.
 */
data class NoticeExplanation(
    val kind: String,
    val what: String,
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

    companion object {
        private val FINISHED =
            setOf(
                OrderState.DELIVERED,
                OrderState.CANCELED,
                OrderState.NOT_PICKED_UP,
                OrderState.EXPIRED,
            ).map { it.wireValue }.toSet()

        fun explain(
            venue: VenueContext,
            payload: Map<String, String>,
        ): NoticeExplanation {
            val type = payload["type"].orEmpty()
            val alert = payload["alert"].orEmpty()
            val orderId = payload["orderId"] ?: payload["order_id"]
            val establishmentId = payload["establecimientoId"]
            val space = payload["espacio_nombre"] ?: payload["espacioNombre"] ?: "Un espacio"

            fun make(
                kind: String,
                what: String,
                roles: List<OperationalRole>,
            ) = NoticeExplanation(
                kind,
                what,
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
                        listOf(OperationalRole.CASHIER),
                    )
                type == "staff_alert" && alert == "nueva_comanda" && payload["station"] == "caja" ->
                    make(
                        "Barra · comanda",
                        "Un pedido ya cobrado tiene productos que prepara la barra.",
                        listOf(OperationalRole.CASHIER),
                    )
                type == "staff_alert" && alert == "nueva_comanda" ->
                    make(
                        "Cocina · comanda",
                        "Un pedido ya cobrado está listo para prepararse.",
                        listOf(OperationalRole.KITCHEN),
                    )
                type == "staff_alert" && alert == "listo_mesero" ->
                    make(
                        "Mesero · listo",
                        "Un pedido está listo para llevarlo a su ${venue.vocabulary.waitingPlace}.",
                        listOf(OperationalRole.WAITER),
                    )
                type == "staff_alert" && alert == "listo_barra" ->
                    make(
                        "Caja · pasa por él",
                        "Un pedido está listo y el cliente pasa a recogerlo a la barra.",
                        listOf(OperationalRole.CASHIER),
                    )
                type == "staff_alert" && alert == "llamada_mesa" ->
                    make(
                        if (venue.capabilities.waiterIsReception) "Recepción · llamada" else "Mesero · llamada",
                        if (venue.capabilities.waiterIsReception) "$space llama a recepción." else "$space llama a un mesero.",
                        listOf(OperationalRole.WAITER),
                    )
                type == "staff_alert" && alert == "turno_por_terminar" ->
                    make(
                        "Turno por terminar",
                        "El tiempo rentado de $space está por terminar o ya terminó.",
                        listOf(OperationalRole.WAITER, OperationalRole.CASHIER),
                    )
                type == "staff_alert" && alert == "por_aceptar" ->
                    make(
                        "Admin · encargo",
                        "Llegó un encargo que el negocio tiene que aceptar o rechazar.",
                        listOf(OperationalRole.ADMIN),
                    )
                type == "staff_alert" && alert == "pedido_domicilio" ->
                    make(
                        "Admin · a domicilio",
                        "Entró un pedido para entregar a domicilio.",
                        listOf(OperationalRole.ADMIN),
                    )
                type == "order_state" && payload["dependienteUsuarioId"] != null ->
                    make(
                        "Pedido de tu hijo o hija",
                        "Avanzó un pedido de alguien de quien eres tutor${stateSuffix(payload)}.",
                        emptyList(),
                    )
                type == "order_state" ->
                    make(
                        "Tu pedido",
                        "Avanzó un pedido hecho con esta cuenta${stateSuffix(payload)}.",
                        emptyList(),
                    )
                type == "saldo_bajo" ->
                    make(
                        "Saldo bajo",
                        "El saldo de alguien de quien eres tutor bajó del límite que pusiste" +
                            "${payload["saldo"]?.let { ": le quedan $$it" }.orEmpty()}.",
                        emptyList(),
                    )
                type == "recarga_acreditada" ->
                    make(
                        "Recarga",
                        "Se abonó una recarga de saldo.",
                        emptyList(),
                    )
                type == "mesero_en_camino" ->
                    make(
                        if (venue.capabilities.waiterIsReception) "Ya vienen a tu habitación" else "Mesero en camino",
                        if (venue.capabilities.waiterIsReception) {
                            "Recepción va a ${SpaceCopy.yourPlace(venue, null)}."
                        } else {
                            "Tu mesero va a ${SpaceCopy.yourPlace(venue, null)}."
                        },
                        emptyList(),
                    )
                else ->
                    make(
                        "Otro aviso",
                        "Un aviso que esta versión de la app no reconoce.",
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
