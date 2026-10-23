package com.vaiinilla.app.ui.counter

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NfcAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
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
    DisposableEffect(enabled, context) {
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

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
