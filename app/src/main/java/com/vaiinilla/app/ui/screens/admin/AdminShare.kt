package com.vaiinilla.app.ui.screens.admin

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.core.qr.QrCodeEncoder
import java.io.File

/** Abre un enlace en el navegador (Stripe, el QR de una mesa). */
internal fun openLink(
    context: Context,
    url: String,
) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

internal fun copyLink(
    context: Context,
    label: String,
    url: String,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, url))
}

/** Comparte el QR como imagen (para imprimirlo o mandarlo), con el enlace como texto. */
internal fun shareQr(
    context: Context,
    url: String,
    name: String,
): Boolean =
    runCatching {
        val matrix = QrCodeEncoder.encode(url)
        val scale = (QR_PIXELS / matrix.width).coerceAtLeast(1)
        val size = matrix.width * scale
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x / scale, y / scale]) Color.BLACK else Color.WHITE)
            }
        }
        val folder = File(context.cacheDir, "qr_codes").apply { mkdirs() }
        val safeName = name.replace(Regex("[^A-Za-z0-9_-]+"), "-").trim('-').ifEmpty { "qr" }
        val file = File(folder, "$safeName-qr.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.product_photos", file)
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TEXT, url)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "QR de $name").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

private const val QR_PIXELS = 720

/**
 * Graba un enlace en una etiqueta NFC, como "Grabar en NFC" de la web. Mientras espera la etiqueta usa reader
 * mode (apaga la emulación de tarjeta solo ese rato) y lo suelta al terminar o al salir de la pantalla.
 */
internal class NfcLinkWriter(
    private val activity: Activity?,
) {
    private val adapter: NfcAdapter? = activity?.let { NfcAdapter.getDefaultAdapter(it) }

    val available: Boolean get() = adapter != null

    val enabled: Boolean get() = adapter?.isEnabled == true

    fun start(
        url: String,
        onResult: (success: Boolean, message: String) -> Unit,
    ) {
        val activity = activity ?: return onResult(false, "No se pudo usar NFC.")
        val adapter = adapter ?: return onResult(false, "Este teléfono no tiene NFC.")
        if (!adapter.isEnabled) return onResult(false, "Prende el NFC en los ajustes del teléfono.")
        val flags =
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        adapter.enableReaderMode(
            activity,
            { tag ->
                val outcome = write(tag, url)
                activity.runOnUiThread {
                    stop()
                    onResult(outcome == null, outcome ?: "Etiqueta grabada.")
                }
            },
            flags,
            null,
        )
    }

    fun stop() {
        val activity = activity ?: return
        runCatching { adapter?.disableReaderMode(activity) }
    }

    /** null si se grabó; si no, qué pasó. */
    private fun write(
        tag: Tag,
        url: String,
    ): String? {
        val link = NdefRecord.createUri(url)
        // Android abre una etiqueta con enlace como cualquier enlace (en Chrome si el dominio no publica
        // assetlinks.json); el registro de app la manda directo a la app de cliente. El enlace va primero: el
        // iPhone lee ese.
        val withApp = NdefMessage(arrayOf(link, NdefRecord.createApplicationRecord(BuildConfig.CLIENT_APP_ID)))
        val linkOnly = NdefMessage(arrayOf(link))
        var message = withApp
        return runCatching {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                try {
                    if (!ndef.isWritable) return "La etiqueta está bloqueada y no se puede grabar."
                    // En una etiqueta chica cabe solo el enlace: abre la web, pero la mesa sigue sirviendo.
                    if (ndef.maxSize < message.byteArrayLength) message = linkOnly
                    if (ndef.maxSize < message.byteArrayLength) return "El enlace no cabe en esta etiqueta."
                    ndef.writeNdefMessage(message)
                } finally {
                    runCatching { ndef.close() }
                }
                null
            } else {
                val formatable = NdefFormatable.get(tag) ?: return "Esta etiqueta no se puede grabar."
                formatable.connect()
                try {
                    formatable.format(message)
                } finally {
                    runCatching { formatable.close() }
                }
                null
            }
        }.getOrElse { "No se pudo grabar. Deja la etiqueta quieta junto al teléfono e intenta de nuevo." }
    }
}

@Composable
internal fun rememberNfcLinkWriter(): NfcLinkWriter {
    val context = LocalContext.current
    val writer = remember(context) { NfcLinkWriter(context.findActivity()) }
    DisposableEffect(writer) { onDispose { writer.stop() } }
    return writer
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
