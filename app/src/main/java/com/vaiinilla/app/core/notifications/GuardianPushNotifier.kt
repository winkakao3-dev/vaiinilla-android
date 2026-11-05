package com.vaiinilla.app.core.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vaiinilla.app.MainActivity
import com.vaiinilla.app.R
import com.vaiinilla.app.domain.guardianship.compactMoney

/**
 * Avisos de Padres e hijos (notifications.service del backend). Al tocarlos abren la ficha del hijo en
 * Cartera, en su escuela ([ChildDeepLink]); el de un pedido abre además ese pedido, solo para verlo (P08).
 */
object GuardianPushNotifier {
    private const val CHANNEL_ID = "order_tracking"

    data class Text(
        val title: String,
        val body: String,
    )

    /** Lo que dice el backend al tutor (TEXTO_ESTADO_TUTOR), con el nombre de pila del hijo y en frase completa. */
    fun orderStateText(
        childName: String?,
        folio: String,
        state: String,
    ): Text? {
        val name = childName ?: "Tu hijo"
        // Sin nombre, «tu hijo» a media frase: decía «Tu hijo su pedido está listo para recoger» (15).
        val ofChild = childName?.let { "de $it" } ?: "de tu hijo"
        val body =
            when (state) {
                "cobrado" -> "$name pidió y ya está en cocina."
                "preparando" -> "La comida $ofChild se está preparando."
                "listo" -> "El pedido $ofChild está listo para recoger."
                "en_camino" -> "El pedido $ofChild va en camino."
                "entregado" -> "$name ya recogió su pedido."
                "cancelado" -> "El pedido $ofChild se canceló."
                "no_recogido" -> "$name no recogió su pedido."
                "expirado" -> "El pedido $ofChild expiró."
                else -> return null
            }
        return Text(title = "$name · Pedido #$folio", body = body)
    }

    fun lowBalanceText(
        childName: String?,
        balance: String?,
    ): Text =
        Text(
            title = childName?.let { "Saldo bajo de $it" } ?: "Saldo bajo",
            body =
                balance?.let { "Le quedan ${money(it)}. Recarga para que no se quede sin comer." }
                    ?: "Recarga para que no se quede sin comer.",
        )

    /** El dato no trae el monto, solo el saldo nuevo; a un hijo con cuenta propia también le llega. */
    fun reloadCreditedText(
        childName: String?,
        balance: String?,
    ): Text =
        Text(
            title = childName?.let { "Recarga lista para $it" } ?: "Recarga lista",
            body = balance?.let { "Saldo: ${money(it)}." } ?: "Ya entró la recarga.",
        )

    private fun money(value: String): String =
        value.toBigDecimalOrNull()?.let(::compactMoney) ?: ("$" + value.removeSuffix(".00"))

    @SuppressLint("MissingPermission")
    fun post(
        context: Context,
        tag: String,
        text: Text,
        link: ChildDeepLink?,
        noticeId: String? = null,
    ): Boolean {
        if (!OrderAdvanceNotifier.canPost(context, NotificationMute.Scope.CLIENT)) return false
        OrderAdvanceNotifier.ensureChannel(context)
        val intent =
            Intent(context, MainActivity::class.java)
                .setAction("${context.packageName}.open.guardian")
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        noticeId?.let { intent.putExtra(OrderAdvanceNotifier.EXTRA_NOTICE_ID, it) }
        link?.let {
            intent.putExtra(ChildDeepLink.EXTRA_DEPENDENT_ID, it.dependentUserId)
            it.establishmentId?.let { id -> intent.putExtra(ChildDeepLink.EXTRA_ESTABLISHMENT_ID, id) }
            it.orderId?.let { id -> intent.putExtra(ChildDeepLink.EXTRA_ORDER_ID, id) }
        }
        val launch =
            PendingIntent.getActivity(
                context,
                // Uno por aviso: con un solo código, el de otro hijo pisaría los datos de este.
                tag.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_order_notification)
                .setContentTitle(text.title)
                .setContentText(text.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
                .setAutoCancel(true)
                // Con su propio grupo Android no lo junta en el resumen automático, que abre la app sin el hijo (F20).
                .setGroup(tag)
                .setContentIntent(launch)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        NotificationManagerCompat.from(context).notify(tag.hashCode(), notification)
        return true
    }
}
