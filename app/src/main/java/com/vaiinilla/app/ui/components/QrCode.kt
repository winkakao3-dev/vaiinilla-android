package com.vaiinilla.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.core.qr.QrCodeEncoder
import kotlin.math.min

@Composable
fun VaiinillaQrCode(
    value: String,
    modifier: Modifier = Modifier,
    qrSize: Dp = 148.dp,
) {
    val matrix = remember(value) { runCatching { QrCodeEncoder.encode(value) }.getOrNull() }
    Canvas(
        modifier =
            modifier
                .size(qrSize)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(10.dp),
    ) {
        val encoded = matrix ?: return@Canvas
        val moduleSize = min(this.size.width / encoded.width, this.size.height / encoded.height)
        val qrWidth = moduleSize * encoded.width
        val qrHeight = moduleSize * encoded.height
        val left = (this.size.width - qrWidth) / 2f
        val top = (this.size.height - qrHeight) / 2f
        for (x in 0 until encoded.width) {
            for (y in 0 until encoded.height) {
                if (encoded[x, y]) {
                    drawRect(
                        color = Color.Black,
                        topLeft =
                            Offset(
                                x = left + x * moduleSize,
                                y = top + y * moduleSize,
                            ),
                        size = Size(moduleSize, moduleSize),
                    )
                }
            }
        }
    }
}
