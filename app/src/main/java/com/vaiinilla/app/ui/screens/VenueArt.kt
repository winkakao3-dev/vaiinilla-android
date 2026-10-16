package com.vaiinilla.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.ui.components.rememberRemoteImage
import kotlin.math.roundToInt

/**
 * Las imágenes del negocio para las escenas del cliente: el logo va en la pared del mostrador y en el piso del
 * plano, y la foto reemplaza la fachada en "Mis pedidos". Si el negocio no subió alguna, la escena queda como
 * siempre.
 */
internal class VenueArt(
    val logo: ImageBitmap? = null,
    val photo: ImageBitmap? = null,
    /** El logo bajado a la cuadrícula del plano, para el tapete. */
    val floorLogo: FloorLogo? = null,
) {
    companion object {
        val None = VenueArt()
    }
}

internal val LocalVenueArt = staticCompositionLocalOf { VenueArt.None }

@Composable
internal fun rememberVenueArt(establishment: PublicEstablishment?): VenueArt {
    val logo = rememberRemoteImage(establishment?.logoUrl)
    val photo = rememberRemoteImage(establishment?.photoUrl)
    val logoUrl = establishment?.logoUrl
    return remember(logo, photo) {
        if (logo == null && photo == null) {
            VenueArt.None
        } else {
            VenueArt(logo = logo, photo = photo, floorLogo = logo?.let { FloorLogo.of(logoUrl.orEmpty(), it) })
        }
    }
}

/**
 * El logo en pixeles de la cuadrícula del plano ([SIZE] × [SIZE]), con su transparencia para que el piso se asome.
 * [edgeColor] es el color de su orilla sobre crema: con él se pinta la pared del mostrador para que el logo se funda.
 */
internal class FloorLogo(
    val key: String,
    val pixels: IntArray,
    val edgeColor: Int,
) {
    companion object {
        const val SIZE = 52
        private const val BACKDROP = 0xFFF5EEDE.toInt()

        fun of(
            key: String,
            image: ImageBitmap,
        ): FloorLogo {
            val source = image.asAndroidBitmap()
            // Recorte cuadrado al centro y luego a la cuadrícula: así el logo cabe entero en el tapete.
            val side = minOf(source.width, source.height)
            val square =
                Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
            val small = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
            val pixels = IntArray(SIZE * SIZE)
            small.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
            return FloorLogo(key, pixels, averageEdge(IntArray(pixels.size) { over(pixels[it], BACKDROP) }))
        }

        private fun averageEdge(pixels: IntArray): Int {
            var r = 0
            var g = 0
            var b = 0
            var count = 0
            for (i in 0 until SIZE) {
                for (index in intArrayOf(i, (SIZE - 1) * SIZE + i, i * SIZE, i * SIZE + SIZE - 1)) {
                    val color = pixels[index]
                    r += (color shr 16) and 0xFF
                    g += (color shr 8) and 0xFF
                    b += color and 0xFF
                    count++
                }
            }
            return (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }

        /** [color] (con transparencia) encima de [bottom], ya opaco. */
        fun over(
            color: Int,
            bottom: Int,
        ): Int {
            val alpha = (color ushr 24) / 255f
            if (alpha >= 0.99f) return color

            fun mix(shift: Int): Int {
                val top = (color shr shift) and 0xFF
                val under = (bottom shr shift) and 0xFF
                return (top * alpha + under * (1f - alpha)).roundToInt()
            }
            return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
        }
    }
}

/** [image] entera dentro de [rect], centrada y sin deformarse. */
internal fun DrawScope.drawImageContain(
    image: ImageBitmap,
    rect: Rect,
) {
    if (rect.width <= 0f || rect.height <= 0f) return
    val scale = minOf(rect.width / image.width, rect.height / image.height)
    val width = image.width * scale
    val height = image.height * scale
    drawImage(
        image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset((rect.center.x - width / 2f).roundToInt(), (rect.center.y - height / 2f).roundToInt()),
        dstSize = IntSize(width.roundToInt(), height.roundToInt()),
    )
}

/** [image] llenando [rect] sin deformarse: recorta lo que sobra, centrado (o pegado arriba con [alignTop]). */
internal fun DrawScope.drawImageCover(
    image: ImageBitmap,
    rect: Rect,
    alpha: Float = 1f,
    alignTop: Boolean = false,
) {
    if (rect.width <= 0f || rect.height <= 0f) return
    val scale = maxOf(rect.width / image.width, rect.height / image.height)
    val srcWidth = (rect.width / scale).roundToInt().coerceIn(1, image.width)
    val srcHeight = (rect.height / scale).roundToInt().coerceIn(1, image.height)
    val srcX = (image.width - srcWidth) / 2
    val srcY = if (alignTop) 0 else (image.height - srcHeight) / 2
    drawImage(
        image,
        srcOffset = IntOffset(srcX, srcY),
        srcSize = IntSize(srcWidth, srcHeight),
        dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
        dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
        alpha = alpha,
    )
}
