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
 * Cartera, en su escuela ([ChildDeepLink]); el pedido es del hijo y el tutor no lo puede abrir con su
 * propia sesión.
 */
object GuardianPushNotifier {
    private const val CHANNEL_ID = "order_tracking"

    data class Text(
        val title: String,
        val body: String,
    )

    /** Lo mismo que dice el backend al tutor (TEXTO_ESTADO_TUTOR), con el nombre de pila guardado del hijo. */
    fun orderStateText(
        childName: String?,
        folio: String,
        state: String,
    ): Text? {
        val action =
            when (state) {
                "cobrado" -> "pidió y ya está en cocina."
                "preparando" -> "su comida se está preparando."
                "listo" -> "su pedido está listo para recoger."
                "entregado" -> "ya recogió su pedido."
                "cancelado" -> "su pedido se canceló."
                "no_recogido" -> "no recogió su pedido."
                "expirado" -> "su pedido expiró."
                else -> return null
            }
        val name = childName ?: "Tu hijo"
        return Text(title = "$name · Pedido #$folio", body = "$name $action")
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
    ) {
        if (!OrderAdvanceNotifier.canPost(context)) return
        OrderAdvanceNotifier.ensureChannel(context)
        val intent =
            Intent(context, MainActivity::class.java)
                .setAction("${context.packageName}.open.guardian")
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        link?.let {
            intent.putExtra(ChildDeepLink.EXTRA_DEPENDENT_ID, it.dependentUserId)
            it.establishmentId?.let { id -> intent.putExtra(ChildDeepLink.EXTRA_ESTABLISHMENT_ID, id) }
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
                .setContentIntent(launch)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        NotificationManagerCompat.from(context).notify(tag.hashCode(), notification)
    }
}
