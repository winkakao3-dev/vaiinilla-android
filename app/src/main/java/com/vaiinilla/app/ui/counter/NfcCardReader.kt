package com.vaiinilla.app.ui.counter

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.vaiinilla.app.domain.counter.StudentCredentialPayload

/**
 * Mientras está [enabled], el teléfono lee el UID de la tarjeta NFC que le acerquen (la credencial del
 * alumno). Usa reader mode, que gana sobre el despachador de etiquetas de la app solo ese rato.
 */
@Composable
internal fun NfcCardReaderEffect(
    enabled: Boolean,
    onCard: (uid: String) -> Unit,
) {
    val context = LocalContext.current
    val currentOnCard by rememberUpdatedState(onCard)
    // Si lo prenden en Ajustes y regresan, el lector se arma sin cerrar la hoja.
    val nfcOn = rememberNfcStatus() == NfcStatus.ON
    DisposableEffect(enabled, context, nfcOn) {
        val activity = context.findActivity()
        val adapter = activity?.let { NfcAdapter.getDefaultAdapter(it) }
        if (!enabled || activity == null || adapter == null || !adapter.isEnabled) {
            return@DisposableEffect onDispose {}
        }
        val flags =
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        runCatching {
            adapter.enableReaderMode(
                activity,
                { tag ->
                    val uid = tag.id?.takeIf { it.isNotEmpty() }?.let(StudentCredentialPayload::nfcUid)
                    if (uid != null) activity.runOnUiThread { currentOnCard(uid) }
                },
                flags,
                null,
            )
        }
        onDispose { runCatching { adapter.disableReaderMode(activity) } }
    }
}

internal enum class NfcStatus { MISSING, OFF, ON }

/** Si el teléfono tiene NFC y si está prendido; se vuelve a leer cada vez que la app regresa al frente. */
@Composable
internal fun rememberNfcStatus(): NfcStatus {
    val context = LocalContext.current
    var status by remember(context) { mutableStateOf(nfcStatus(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = nfcStatus(context) }
    return status
}

private fun nfcStatus(context: Context): NfcStatus {
    val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcStatus.MISSING
    return if (adapter.isEnabled) NfcStatus.ON else NfcStatus.OFF
}

/** Abre los ajustes de NFC (o los generales si el teléfono no tiene esa pantalla). */
internal fun openNfcSettings(context: Context) {
    val opened =
        runCatching {
            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    if (!opened) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
