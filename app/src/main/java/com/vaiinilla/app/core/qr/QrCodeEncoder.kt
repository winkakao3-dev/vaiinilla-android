package com.vaiinilla.app.core.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeEncoder {
    fun encode(value: String): BitMatrix {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) { "El contenido del QR no puede estar vacío." }
        return QRCodeWriter().encode(
            normalized,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.MARGIN to 1),
        )
    }
}
