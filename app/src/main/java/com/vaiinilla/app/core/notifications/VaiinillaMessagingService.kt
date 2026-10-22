package com.vaiinilla.app.core.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vaiinilla.app.domain.model.OrderState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives FCM data messages for order events.
 *
 * `order_state` (cliente): the order advanced — shows the same tracking
 * notification the in-app poller produces, but works even with the app killed.
 * `staff_alert` (caja/cocina/mesero): new actionable work for the venue staff.
 * Padres e hijos: `order_state` con `dependienteUsuarioId` (el pedido de un hijo), `saldo_bajo` y
 * `recarga_acreditada` se ven como aviso normal, sin abrir al hijo.
 */
@AndroidEntryPoint
class VaiinillaMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var tokenRegistrar: DeviceTokenRegistrar

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Si no hay sesión activa se omite; el registro se reintenta al activarse un contexto.
        tokenRegistrar.registerToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        when (data["type"]) {
            "order_state" ->
                if (data["dependienteUsuarioId"] != null) notifyChildOrderState(data) else notifyOrderState(data)
            "saldo_bajo" ->
                notifyGuardian(
                    data,
                    "saldo-bajo",
                ) { name -> GuardianPushNotifier.lowBalanceText(name, data["saldo"]) }
            "recarga_acreditada" ->
                notifyGuardian(data, "recarga") { name -> GuardianPushNotifier.reloadCreditedText(name, data["saldo"]) }
            "staff_alert" -> notifyStaffAlert(data)
            "mesero_en_camino" -> notifyCallerOnTheWay(data)
        }
    }

    private fun notifyOrderState(data: Map<String, String>) {
        val orderId = data["orderId"] ?: return
        val folio = data["folio"]?.toIntOrNull() ?: return
        val state =
            data["state"]?.let {
                try {
                    OrderState.fromWireValue(it)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Estado desconocido en push: $it")
                    null
                }
            } ?: return
        OrderAdvanceNotifier.notify(applicationContext, orderId, folio, state)
    }

    private fun childName(data: Map<String, String>): String? =
        data["dependienteUsuarioId"]?.let { DependentNamesStore.firstName(applicationContext, it) }

    private fun notifyChildOrderState(data: Map<String, String>) {
        val orderId = data["orderId"] ?: return
        val text =
            GuardianPushNotifier.orderStateText(childName(data), data["folio"] ?: return, data["state"] ?: return)
                ?: return
        GuardianPushNotifier.post(applicationContext, "hijo:$orderId", text)
    }

    private fun notifyGuardian(
        data: Map<String, String>,
        kind: String,
        text: (String?) -> GuardianPushNotifier.Text,
    ) {
        GuardianPushNotifier.post(
            applicationContext,
            "$kind:${data["dependienteUsuarioId"].orEmpty()}",
            text(childName(data)),
        )
    }

    private fun notifyStaffAlert(data: Map<String, String>) {
        val alert = data["alert"] ?: return
        if (alert == "llamada_mesa") {
            val callId = data["llamada_id"] ?: data["llamadaId"] ?: return
            OrderAdvanceNotifier.notifyTableCall(
                context = applicationContext,
                callId = callId,
                spaceName = data["espacio_nombre"] ?: data["espacioNombre"].orEmpty(),
                reason = data["motivo"],
            )
            return
        }
        if (alert == "turno_por_terminar") {
            OrderAdvanceNotifier.notifyTurnEnding(
                context = applicationContext,
                spaceName = data["espacio_nombre"] ?: data["espacioNombre"].orEmpty(),
                minutesLeft = data["restante_min"]?.toIntOrNull() ?: 0,
            )
            return
        }
        val orderId = data["orderId"] ?: return
        val folio = data["folio"]?.toIntOrNull() ?: return
        OrderAdvanceNotifier.notifyStaff(applicationContext, orderId, folio, alert)
    }

    private fun notifyCallerOnTheWay(data: Map<String, String>) {
        val callId = data["llamada_id"] ?: data["llamadaId"] ?: "mesero"
        OrderAdvanceNotifier.notifyCallerOnTheWay(
            context = applicationContext,
            callId = callId,
            waiterName = data["mesero_nombre"] ?: data["meseroNombre"],
        )
    }

    private companion object {
        const val TAG = "VaiinillaMessaging"
    }
}
