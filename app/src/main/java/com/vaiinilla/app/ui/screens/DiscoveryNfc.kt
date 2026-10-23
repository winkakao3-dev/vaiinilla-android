package com.vaiinilla.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Sin adaptador NFC no hay etiqueta que leer: la tarjeta "Acercar al NFC" ni sale. */
@Composable
internal fun rememberHasNfcReader(): Boolean {
    val context = LocalContext.current
    return remember(context) { runCatching { NfcAdapter.getDefaultAdapter(context) != null }.getOrDefault(false) }
}

/**
 * La etiqueta de la mesa la lee MainActivity (foreground dispatch) mientras la app está al frente y abre la mesa
 * por el mismo camino que el QR. Si el NFC está apagado no llega nada: se manda a prenderlo.
 */
internal fun openNfcSettingsIfOff(context: Context) {
    val adapter = runCatching { NfcAdapter.getDefaultAdapter(context) }.getOrNull() ?: return
    if (adapter.isEnabled) return
    try {
        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Sin pantalla de ajustes de NFC, la pista de la tarjeta basta.
    }
}
