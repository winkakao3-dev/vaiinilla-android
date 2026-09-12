package com.vaiinilla.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vaiinilla.app.MainActivity
import com.vaiinilla.app.R
import com.vaiinilla.app.domain.model.OrderState

/**
 * Posts local notifications when a client order advances through the tracking flow.
 * Sound and vibration come from the channel defaults (system notification sound).
 */
object OrderAdvanceNotifier {
    const val EXTRA_ORDER_ID = "order_id"
    private const val CHANNEL_ID = "order_tracking"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Seguimiento de pedidos",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Avisos cuando tu pedido avanza de estado"
                },
            )
        }
    }

    private const val STAFF_CHANNEL_ID = "staff_orders"

    private fun ensureStaffChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(STAFF_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    STAFF_CHANNEL_ID,
                    "Pedidos del establecimiento",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Avisos de comandas nuevas para caja, cocina y meseros"
                },
            )
        }
    }

    /** Posts a staff-facing heads-up (nueva comanda en cocina, pedido por cobrar, etc.). */
    fun notifyStaff(
        context: Context,
        orderId: String,
        folio: Int,
        alert: String,
    ) {
        if (!canPost(context)) return
        ensureStaffChannel(context)
        val launchIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_ORDER_ID, orderId)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val (title, text) =
            when (alert) {
                "por_cobrar" -> "Pedido #$folio por cobrar" to "Entro un pedido en efectivo."
                "nueva_comanda" -> "Nueva comanda #$folio" to "Cocina: pedido cobrado listo para preparar."
                "listo_mesero" -> "Pedido #$folio listo" to "Entregar al espacio del alumno."
                else -> "Pedido #$folio" to "El pedido cambio de estado."
            }
        val notification =
            NotificationCompat
                .Builder(context, STAFF_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_order_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(launchIntent)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        NotificationManagerCompat.from(context).notify("staff:$orderId:$alert".hashCode(), notification)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun notify(
        context: Context,
        orderId: String,
        folio: Int,
        newState: OrderState,
    ) {
        if (!canPost(context)) return
        ensureChannel(context)
        val launchIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_ORDER_ID, orderId)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_order_notification)
                .setContentTitle("Pedido #$folio")
                .setContentText(messageFor(newState))
                .setAutoCancel(true)
                .setContentIntent(launchIntent)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        NotificationManagerCompat.from(context).notify(orderId.hashCode(), notification)
    }

    private fun messageFor(state: OrderState): String =
        when (state) {
            OrderState.PENDING_PAYMENT -> "Caja espera tu pago."
            OrderState.PAID -> "Pago confirmado. Cocina recibio tu comanda."
            OrderState.PREPARING -> "Tu comida se esta preparando."
            OrderState.READY -> "Tu pedido esta listo. Recogelo en la barra."
            OrderState.DELIVERED -> "Pedido entregado. Buen provecho."
            OrderState.CANCELED -> "Tu pedido fue cancelado."
            OrderState.NOT_PICKED_UP -> "Tu pedido no fue recogido."
            OrderState.EXPIRED -> "Tu pedido expiro."
        }
}
