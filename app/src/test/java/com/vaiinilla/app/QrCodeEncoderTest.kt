package com.vaiinilla.app

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.vaiinilla.app.core.qr.QrCodeEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeEncoderTest {
    @Test
    fun `encodes a non empty pickup token as a QR matrix`() {
        val value = "v1.pickup-token"
        val matrix = QrCodeEncoder.encode(value)

        assertTrue(matrix.width > 20)
        assertTrue(matrix.height > 20)
        assertTrue((0 until matrix.width).any { x -> (0 until matrix.height).any { y -> matrix[x, y] } })

        val scale = 8
        val quietZone = 4
        val renderedWidth = (matrix.width + quietZone * 2) * scale
        val pixels = IntArray(renderedWidth * renderedWidth) { 0xFFFFFFFF.toInt() }
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                if (!matrix[x, y]) continue
                repeat(scale) { offsetX ->
                    repeat(scale) { offsetY ->
                        val renderedX = (x + quietZone) * scale + offsetX
                        val renderedY = (y + quietZone) * scale + offsetY
                        pixels[renderedY * renderedWidth + renderedX] = 0xFF000000.toInt()
                    }
                }
            }
        }
        val decoded =
            MultiFormatReader()
                .decode(
                    BinaryBitmap(
                        HybridBinarizer(RGBLuminanceSource(renderedWidth, renderedWidth, pixels)),
                    ),
                ).text

        assertEquals(value, decoded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects empty QR payload`() {
        QrCodeEncoder.encode(" ")
    }
}
