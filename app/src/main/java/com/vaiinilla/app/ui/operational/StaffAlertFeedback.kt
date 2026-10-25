package com.vaiinilla.app.ui.operational

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.vaiinilla.app.core.notifications.OrderAdvanceNotifier

private val AlertAudio =
    AudioAttributes
        .Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

private val AlertPattern = longArrayOf(0, 120, 80, 120)

/** El nombre con que el backend manda este aviso en el push; null si el push no lo cubre. */
internal fun StaffAlert.pushAlertName(): String? =
    when (kind) {
        StaffAlertKind.NEW_KITCHEN_ORDER -> "nueva_comanda"
        StaffAlertKind.CASH_DUE -> "por_cobrar"
        StaffAlertKind.READY_FOR_PICKUP -> null
    }

/** Qué hace un aviso según el modo del timbre del teléfono: sonido y vibración, solo vibración, o nada. */
internal enum class AlertFeedback { SOUND_AND_VIBRATION, VIBRATION_ONLY, SILENT }

internal fun alertFeedbackFor(ringerMode: Int): AlertFeedback =
    when (ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> AlertFeedback.SOUND_AND_VIBRATION
        AudioManager.RINGER_MODE_VIBRATE -> AlertFeedback.VIBRATION_ONLY
        else -> AlertFeedback.SILENT
    }

/**
 * Sonido y vibración de un aviso dentro de la app, con el timbre y el volumen de notificaciones del sistema:
 * en silencio no suena ni vibra. Si el push de ese mismo pedido ya está en pantalla, el push ya sonó y aquí no
 * se repite.
 */
fun playStaffAlert(
    context: Context,
    alert: StaffAlert,
) {
    val pushName = alert.pushAlertName()
    if (pushName != null && OrderAdvanceNotifier.hasActiveStaffNotification(context, alert.orderId, pushName)) return
    playAlertFeedback(context)
}

/** Aviso sin pedido de por medio (una mesa que llama, un rechazo): mismo criterio de timbre del sistema. */
fun playAlertFeedback(context: Context) {
    val audio = context.getSystemService(AudioManager::class.java)
    val feedback = alertFeedbackFor(audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL)
    if (feedback == AlertFeedback.SILENT) return
    if (feedback == AlertFeedback.SOUND_AND_VIBRATION) {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, uri)?.apply { audioAttributes = AlertAudio }?.play()
        }
    }
    runCatching {
        val vibrator =
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                ?: context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createWaveform(AlertPattern, -1), AlertAudio)
    }
}
