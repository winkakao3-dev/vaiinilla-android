package com.vaiinilla.app.ui.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Le entrega a un lector abierto lo que leería la cámara o el NFC, para recorrer esos flujos en la JVM, donde no hay
 * hardware (tests y jvm-roles). Pasa por el mismo callback que el lector real. Solo se alcanza desde el mismo proceso:
 * ninguna pantalla lo llama.
 */
internal object ScanBridge {
    var qrConsumer: ((String) -> Unit)? = null
    var nfcConsumer: ((String) -> Unit)? = null

    /** false si no hay un lector de QR abierto. */
    fun injectQr(payload: String): Boolean = qrConsumer?.let { it(payload) } != null

    /** false si no hay un lector de NFC armado. [uid] va en hexadecimal, como `StudentCredentialPayload.nfcUid`. */
    fun injectNfc(uid: String): Boolean = nfcConsumer?.let { it(uid) } != null
}

@Composable
internal fun RegisterQrConsumer(onPayload: (String) -> Unit) {
    val current by rememberUpdatedState(onPayload)
    DisposableEffect(Unit) {
        val consumer: (String) -> Unit = { current(it) }
        ScanBridge.qrConsumer = consumer
        onDispose { if (ScanBridge.qrConsumer === consumer) ScanBridge.qrConsumer = null }
    }
}

@Composable
internal fun RegisterNfcConsumer(
    enabled: Boolean,
    onCard: (String) -> Unit,
) {
    val current by rememberUpdatedState(onCard)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val consumer: (String) -> Unit = { current(it) }
        ScanBridge.nfcConsumer = consumer
        onDispose { if (ScanBridge.nfcConsumer === consumer) ScanBridge.nfcConsumer = null }
    }
}
