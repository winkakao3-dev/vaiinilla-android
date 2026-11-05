package com.vaiinilla.app.core.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vaiinilla.app.core.notifications.OrderAdvanceNotifier.Shown
import com.vaiinilla.app.domain.model.OrderDestination
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
 * `recarga_acreditada` abren la ficha del hijo en su escuela (`establecimientoId`).
 * Cada mensaje queda en [ReceivedNoticeLog] para explicarlo en «Avisos recibidos» (AVISOS-01).
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
        val noticeId = message.messageId ?: "push:${data["type"]}:${data["orderId"]}:${message.sentTime}"
        val shown: Shown? =
            when (data["type"]) {
                "order_state" ->
                    if (data["dependienteUsuarioId"] != null) {
                        notifyChildOrderState(data, noticeId)
                    } else {
                        notifyOrderState(data, noticeId)
                    }
                "saldo_bajo" ->
                    notifyGuardian(
                        data,
                        "saldo-bajo",
                        noticeId,
                    ) { name -> GuardianPushNotifier.lowBalanceText(name, data["saldo"]) }
                "recarga_acreditada" ->
                    notifyGuardian(data, "recarga", noticeId) { name ->
                        GuardianPushNotifier.reloadCreditedText(name, data["saldo"])
                    }
                "staff_alert" -> notifyStaffAlert(data, noticeId)
                "mesero_en_camino" ->
                    notifyCallerOnTheWay(data, noticeId).also { TableCallPushes.notifyChanged() }
                else -> null
            }
        if (data["type"] == "order_state" || data["type"] == "staff_alert") OrderPushes.notifyChanged()
        // Todo aviso se anota, también el que no se pudo mostrar: es justo el que hay que explicar.
        val explanation = NoticeExplanation.explain(data, isRemote = true)
        ReceivedNoticeLog.record(
            context = applicationContext,
            id = noticeId,
            title = shown?.title ?: message.notification?.title ?: explanation.kind,
            body = shown?.body ?: message.notification?.body ?: explanation.what,
            payload = data,
            isRemote = true,
            arrival =
                when {
                    shown?.posted != true -> ReceivedNotice.Arrival.NOT_SHOWN
                    ReceivedNoticeLog.appInForeground() -> ReceivedNotice.Arrival.SHOWN
                    else -> ReceivedNotice.Arrival.BACKGROUND
                },
            receivedAtMillis = message.sentTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
    }

    private fun notifyOrderState(
        data: Map<String, String>,
        noticeId: String,
    ): Shown? {
        val orderId = data["orderId"] ?: return null
        val folio = data["folio"]?.toIntOrNull() ?: return null
        val state =
            data["state"]?.let {
                try {
                    OrderState.fromWireValue(it)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Estado desconocido en push: $it")
                    null
                }
            } ?: return null
        return OrderAdvanceNotifier.notify(
            applicationContext,
            orderId,
            folio,
            state,
            // Un servidor anterior no manda destino, espacioTipo, recogeEnBarra ni renta: el texto de siempre.
            inSpace = data["destino"] == "en_espacio",
            spaceType = data["espacioTipo"]?.takeIf { it.isNotBlank() },
            noticeId = noticeId,
            acceptedAwaitingPayment = data["pagarTrasAceptar"] == "true",
            pickUpAtBar = data["recogeEnBarra"] == "true",
            homeDelivery = data["destino"] == OrderDestination.HOME_DELIVERY.wireValue,
            rental = data["renta"] == "true",
            rentalSpaceName = data["espacioNombre"],
        )
    }

    private fun childName(data: Map<String, String>): String? =
        data["dependienteUsuarioId"]?.let { DependentNamesStore.firstName(applicationContext, it) }

    private fun notifyChildOrderState(
        data: Map<String, String>,
        noticeId: String,
    ): Shown? {
        val orderId = data["orderId"] ?: return null
        val folio = data["folio"] ?: return null
        val state = data["state"] ?: return null
        val text = GuardianPushNotifier.orderStateText(childName(data), folio, state) ?: return null
        val posted =
            GuardianPushNotifier.post(applicationContext, "hijo:$orderId", text, ChildDeepLink.fromPush(data), noticeId)
        return Shown(text.title, text.body, posted)
    }

    private fun notifyGuardian(
        data: Map<String, String>,
        kind: String,
        noticeId: String,
        text: (String?) -> GuardianPushNotifier.Text,
    ): Shown {
        val shownText = text(childName(data))
        val posted =
            GuardianPushNotifier.post(
                applicationContext,
                "$kind:${data["dependienteUsuarioId"].orEmpty()}",
                shownText,
                ChildDeepLink.fromPush(data),
                noticeId,
            )
        return Shown(shownText.title, shownText.body, posted)
    }

    private fun notifyStaffAlert(
        data: Map<String, String>,
        noticeId: String,
    ): Shown? {
        val alert = data["alert"] ?: return null
        if (alert == "llamada_mesa") {
            val callId = data["llamada_id"] ?: data["llamadaId"] ?: return null
            return OrderAdvanceNotifier.notifyTableCall(
                context = applicationContext,
                callId = callId,
                spaceName = data["espacio_nombre"] ?: data["espacioNombre"].orEmpty(),
                reason = data["motivo"],
                noticeId = noticeId,
                establishmentId = data["establecimiento_id"] ?: data["establecimientoId"],
            )
        }
        if (alert == "turno_por_terminar") {
            return OrderAdvanceNotifier.notifyTurnEnding(
                context = applicationContext,
                spaceName = data["espacio_nombre"] ?: data["espacioNombre"].orEmpty(),
                minutesLeft = data["restante_min"]?.toIntOrNull() ?: 0,
                noticeId = noticeId,
            )
        }
        val orderId = data["orderId"] ?: return null
        val folio = data["folio"]?.toIntOrNull() ?: return null
        return OrderAdvanceNotifier.notifyStaff(applicationContext, orderId, folio, alert, noticeId)
    }

    private fun notifyCallerOnTheWay(
        data: Map<String, String>,
        noticeId: String,
    ): Shown {
        val callId = data["llamada_id"] ?: data["llamadaId"] ?: "mesero"
        return OrderAdvanceNotifier.notifyCallerOnTheWay(
            context = applicationContext,
            callId = callId,
            noticeId = noticeId,
        )
    }

    private companion object {
        const val TAG = "VaiinillaMessaging"
    }
}
