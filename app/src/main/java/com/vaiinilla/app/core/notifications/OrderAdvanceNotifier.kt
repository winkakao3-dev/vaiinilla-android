package com.vaiinilla.app.core.notifications

import android.Manifest
import android.annotation.SuppressLint
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
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.repository.CallReason

/**
 * Posts local notifications when a client order advances through the tracking flow.
 * Sound and vibration come from the channel defaults (system notification sound).
 */
object OrderAdvanceNotifier {
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_NOTIFICATION_TARGET = "notification_target"

    /** El id del aviso en [ReceivedNoticeLog], para anotar que se tocó. */
    const val EXTRA_NOTICE_ID = "aviso_id"

    /** El negocio de una llamada de mesa, si el push lo trae: se abre el modo mesero de ese negocio (K02). */
    const val EXTRA_ESTABLISHMENT_ID = "establecimiento_id"

    /** Lo que se mostró (o se habría mostrado) en el teléfono. */
    data class Shown(
        val title: String,
        val body: String,
        val posted: Boolean,
    )

    private const val CHANNEL_ID = "order_tracking"
    private const val ACCEPTED_AWAITING_PAYMENT_TEXT = "El negocio aceptó tu pedido. Págalo para confirmarlo."
    private const val STAFF_MODES_REQUEST_CODE = 710_300
    private const val PLAIN_LAUNCH_REQUEST_CODE = 710_301
    private const val WAITER_CALL_REQUEST_CODE = 710_302

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
    @SuppressLint("MissingPermission")
    fun notifyStaff(
        context: Context,
        orderId: String,
        folio: Int,
        alert: String,
        noticeId: String? = null,
    ): Shown {
        val (title, text) =
            when (alert) {
                "por_cobrar" -> "Pedido #$folio por cobrar" to "Entro un pedido en efectivo."
                "nueva_comanda" -> "Nueva comanda #$folio" to "Cocina: pedido cobrado listo para preparar."
                "listo_mesero" -> "Pedido #$folio listo" to "Entregar al espacio del alumno."
                // «Pasa por él»: el cliente viene a la barra y Caja se lo entrega.
                "listo_barra" -> "Pedido #$folio listo" to "El cliente pasa a recogerlo a la barra."
                // Repostería (CASA-09): llegan a Administración, que acepta y lleva los pedidos.
                "por_aceptar" -> "Encargo #$folio por aceptar" to "Acéptalo o recházalo en Pedidos."
                "pedido_domicilio" -> "Pedido #$folio a domicilio" to "Entró un pedido para entregar."
                else -> "Pedido #$folio" to "El pedido cambio de estado."
            }
        if (!canPost(context)) return Shown(title, text, posted = false)
        ensureStaffChannel(context)
        val launchIntent = orderLaunchIntent(context, orderId, OrderNotificationTarget.STAFF, noticeId)
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
        NotificationManagerCompat.from(context).notify(staffNotificationId(orderId, alert), notification)
        return Shown(title, text, posted = true)
    }

    internal fun staffNotificationId(
        orderId: String,
        alert: String,
    ): Int = "staff:$orderId:$alert".hashCode()

    /** ¿Ya hay en pantalla un aviso de ese pedido? Entonces el push ya sonó y la app no lo repite. */
    fun hasActiveStaffNotification(
        context: Context,
        orderId: String,
        alert: String,
    ): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val id = staffNotificationId(orderId, alert)
        return runCatching { manager.activeNotifications.any { it.id == id } }.getOrDefault(false)
    }

    /** Aviso a meseros: una mesa llamó y por qué. Tocar la notificación abre el tablero del mesero (K02). */
    @SuppressLint("MissingPermission")
    fun notifyTableCall(
        context: Context,
        callId: String,
        spaceName: String,
        reason: String?,
        noticeId: String? = null,
        establishmentId: String? = null,
    ): Shown {
        val title = if (spaceName.isBlank()) "Una mesa te llama" else "$spaceName te llama"
        val text = tableCallText(reason)
        if (!canPost(context)) return Shown(title, text, posted = false)
        ensureStaffChannel(context)
        val notification =
            NotificationCompat
                .Builder(context, STAFF_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_order_notification)
                .setContentTitle(title)
                .setContentText(text)
                // El motivo es lo que el mesero necesita: completo aunque el sistema recorte la línea.
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(waiterCallLaunchIntent(context, noticeId, establishmentId))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        NotificationManagerCompat.from(context).notify("staff-call:$callId".hashCode(), notification)
        return Shown(title, text, posted = true)
    }

    /** Aviso al personal: el turno de una cancha está por terminar o ya terminó. Va al tablero del personal. */
    @SuppressLint("MissingPermission")
    fun notifyTurnEnding(
        context: Context,
        spaceName: String,
        minutesLeft: Int,
        noticeId: String? = null,
    ): Shown {
        val title = turnEndingTitle(spaceName)
        val text = turnEndingText(minutesLeft)
        if (!canPost(context)) return Shown(title, text, posted = false)
        ensureStaffChannel(context)
        val notification =
            NotificationCompat
                .Builder(context, STAFF_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_order_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(staffModesLaunchIntent(context, noticeId))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        NotificationManagerCompat.from(context).notify("staff-turn:$spaceName".hashCode(), notification)
        return Shown(title, text, posted = true)
    }

    internal fun turnEndingTitle(spaceName: String): String =
        if (spaceName.isBlank()) "Un turno está por terminar" else "$spaceName: turno por terminar"

    internal fun turnEndingText(minutesLeft: Int): String =
        if (minutesLeft > 0) {
            "Termina en $minutesLeft min. Ve preparando la cuenta."
        } else {
            "Ya terminó. Cobra la cuenta y libera el espacio."
        }

    /** El motivo como lo ve el mesero en su tablero; «cuenta» también (antes caía en «Necesita atención», K02). */
    internal fun tableCallText(reason: String?): String =
        CallReason.entries.firstOrNull { it.wireValue == reason }?.staffLabel ?: CallReason.ATENCION.staffLabel

    /** Aviso al cliente: el mesero que atiende su llamada va en camino. */
    @SuppressLint("MissingPermission")
    fun notifyCallerOnTheWay(
        context: Context,
        callId: String,
        noticeId: String? = null,
    ): Shown {
        val title = "Tu mesero va en camino"
        val text = "Va a tu mesa."
        if (!canPost(context)) return Shown(title, text, posted = false)
        ensureChannel(context)
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_order_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(plainLaunchIntent(context, noticeId))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        NotificationManagerCompat.from(context).notify("call-on-the-way:$callId".hashCode(), notification)
        return Shown(title, text, posted = true)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun notify(
        context: Context,
        orderId: String,
        folio: Int,
        newState: OrderState,
        inSpace: Boolean = false,
        spaceType: String? = null,
        noticeId: String? = null,
        acceptedAwaitingPayment: Boolean = false,
        pickUpAtBar: Boolean = false,
        homeDelivery: Boolean = false,
        rental: Boolean = false,
        rentalSpaceName: String? = null,
    ): Shown {
        // Una renta de cancha no se prepara ni se recoge: su aviso habla del horario, no del pedido.
        val rentalText = if (rental) rentalMessageFor(newState, rentalSpaceName) else null
        val title = if (rentalText != null) rentalTitle(rentalSpaceName) else "Pedido #$folio"
        val text =
            when {
                acceptedAwaitingPayment -> ACCEPTED_AWAITING_PAYMENT_TEXT
                rentalText != null -> rentalText
                else ->
                    messageFor(
                        newState,
                        inSpace,
                        spaceType,
                        pickUpAtBar,
                        homeDelivery,
                    )
            }
        if (!canPost(context)) return Shown(title, text, posted = false)
        ensureChannel(context)
        val launchIntent = orderLaunchIntent(context, orderId, OrderNotificationTarget.CLIENT, noticeId)
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_order_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(launchIntent)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // El mismo avance llega por push y por el sondeo: el segundo aviso reemplaza al primero sin sonar.
                .setOnlyAlertOnce(true)
                .build()
        // Un id por avance: el mismo avance se reemplaza en silencio, pero el siguiente estado sí suena.
        NotificationManagerCompat.from(context).notify("$orderId:${newState.wireValue}".hashCode(), notification)
        return Shown(title, text, posted = true)
    }

    internal fun notificationRequestCode(
        orderId: String,
        target: OrderNotificationTarget,
    ): Int = 31 * orderId.hashCode() + target.ordinal

    private fun orderLaunchIntent(
        context: Context,
        orderId: String,
        target: OrderNotificationTarget,
        noticeId: String?,
    ): PendingIntent {
        val requestCode = notificationRequestCode(orderId, target)
        return PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .setAction("${context.packageName}.order.${target.wireValue}.$requestCode")
                .putExtra(EXTRA_ORDER_ID, orderId)
                .putExtra(EXTRA_NOTIFICATION_TARGET, target.wireValue)
                .withNoticeId(noticeId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Sin pedido asociado: la app resuelve el objetivo STAFF abriendo la consola de modos. Con aviso, un código por
     * aviso: con uno fijo, tocar el viejo anotaría el más nuevo.
     */
    private fun staffModesLaunchIntent(
        context: Context,
        noticeId: String?,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            noticeId?.hashCode() ?: STAFF_MODES_REQUEST_CODE,
            Intent(context, MainActivity::class.java)
                .setAction("${context.packageName}.staff.modes")
                .putExtra(EXTRA_NOTIFICATION_TARGET, OrderNotificationTarget.STAFF.wireValue)
                .withNoticeId(noticeId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Una llamada de mesa: la app abre el tablero del mesero, donde se ve la llamada con su motivo. */
    private fun waiterCallLaunchIntent(
        context: Context,
        noticeId: String?,
        establishmentId: String?,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            noticeId?.hashCode() ?: WAITER_CALL_REQUEST_CODE,
            Intent(context, MainActivity::class.java)
                .setAction("${context.packageName}.staff.waiter_call")
                .putExtra(EXTRA_NOTIFICATION_TARGET, OrderNotificationTarget.WAITER_CALL.wireValue)
                .putExtra(EXTRA_ESTABLISHMENT_ID, establishmentId)
                .withNoticeId(noticeId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun plainLaunchIntent(
        context: Context,
        noticeId: String?,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            noticeId?.hashCode() ?: PLAIN_LAUNCH_REQUEST_CODE,
            Intent(context, MainActivity::class.java)
                .setAction("${context.packageName}.open")
                .withNoticeId(noticeId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun Intent.withNoticeId(noticeId: String?): Intent =
        if (noticeId == null) this else putExtra(EXTRA_NOTICE_ID, noticeId)

    internal const val PICK_UP_AT_BAR_TEXT = "Tu pedido está listo, pasa a la barra."

    internal fun messageFor(
        state: OrderState,
        inSpace: Boolean = false,
        spaceType: String? = null,
        pickUpAtBar: Boolean = false,
        homeDelivery: Boolean = false,
    ): String =
        when (state) {
            OrderState.PENDING_PAYMENT -> "Caja espera tu pago."
            // A domicilio puede pagarse al recibir: «Pago confirmado» no siempre es cierto (igual que el backend).
            OrderState.PAID ->
                when {
                    homeDelivery -> "El negocio recibió tu pedido."
                    // En un lugar puede ir a la cuenta, que nace cobrada sin pagarse: no se promete un pago (K01).
                    inSpace -> "Cocina recibió tu comanda."
                    else -> "Pago confirmado. Cocina recibió tu comanda."
                }
            OrderState.PREPARING -> "Tu comida se está preparando."
            OrderState.READY ->
                when {
                    // «Pasa por él»: el cliente pidió en su lugar pero lo recoge en la barra.
                    pickUpAtBar -> PICK_UP_AT_BAR_TEXT
                    inSpace -> SpaceCopy.readyInSpace(spaceType)
                    homeDelivery -> "Tu pedido está listo para salir."
                    else -> "Tu pedido está listo. Recógelo en la barra."
                }
            OrderState.DELIVERED -> "Pedido entregado. Buen provecho."
            OrderState.CANCELED -> "Tu pedido fue cancelado."
            OrderState.NOT_PICKED_UP -> "Tu pedido no fue recogido."
            OrderState.EXPIRED -> "Tu pedido expiró."
            OrderState.AWAITING_ACCEPTANCE -> "El negocio está revisando tu encargo."
            OrderState.ON_THE_WAY -> "Tu pedido va en camino."
        }

    private fun rentalTitle(spaceName: String?): String =
        spaceName?.takeIf { it.isNotBlank() }?.let { "Renta de $it" } ?: "Tu renta"

    /**
     * Aviso de una renta de cancha (`renta`), como los textos del backend: solo se confirma el horario o se cae la
     * renta. Para los demás estados `null` y se usa el texto del pedido.
     */
    internal fun rentalMessageFor(
        state: OrderState,
        spaceName: String?,
    ): String? {
        val name = spaceName?.takeIf { it.isNotBlank() }
        return when (state) {
            OrderState.DELIVERED -> name?.let { "Tu horario de $it está confirmado." } ?: "Tu horario está confirmado."
            OrderState.CANCELED -> name?.let { "Se canceló tu renta de $it." } ?: "Se canceló tu renta."
            OrderState.EXPIRED -> name?.let { "Tu renta de $it venció sin pagarse." } ?: "Tu renta venció sin pagarse."
            else -> null
        }
    }
}
