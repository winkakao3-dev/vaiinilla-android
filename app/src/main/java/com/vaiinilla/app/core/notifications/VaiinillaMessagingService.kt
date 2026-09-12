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
 */
@AndroidEntryPoint
class VaiinillaMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var tokenRegistrar: DeviceTokenRegistrar

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Si no hay sesión activa el POST falla y el próximo login lo reintenta.
        tokenRegistrar.registerToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        when (data["type"]) {
            "order_state" -> notifyOrderState(data)
            "staff_alert" -> notifyStaffAlert(data)
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

    private fun notifyStaffAlert(data: Map<String, String>) {
        val orderId = data["orderId"] ?: return
        val folio = data["folio"]?.toIntOrNull() ?: return
        val alert = data["alert"] ?: return
        OrderAdvanceNotifier.notifyStaff(applicationContext, orderId, folio, alert)
    }

    private companion object {
        const val TAG = "VaiinillaMessaging"
    }
}
