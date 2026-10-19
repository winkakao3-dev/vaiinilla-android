package com.vaiinilla.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.ui.components.rememberRemoteImage
import kotlin.math.roundToInt

/**
 * Las imágenes del negocio para las escenas del cliente: el logo va en la pared del mostrador y la foto reemplaza la
 * fachada en "Mis pedidos". Si el negocio no subió alguna, la escena queda como siempre.
 */
internal class VenueArt(
    val logo: ImageBitmap? = null,
    val photo: ImageBitmap? = null,
    /** El logo bajado a una cuadrícula chica: de ahí salen el color de su orilla y si es logo o foto. */
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
 * El logo en una cuadrícula de [SIZE] × [SIZE] pixeles, con su transparencia.
 * [edgeColor] es el color de su orilla sobre crema: con él se pinta la pared del mostrador para que el logo se funda.
 * [edgeSpread] dice qué tan dispareja es esa orilla: 0 si es de un solo color, 1 si va de negro a blanco. Un logo
 * tiene la orilla lisa o transparente; una foto, no.
 */
internal class FloorLogo(
    val key: String,
    val pixels: IntArray,
    val edgeColor: Int,
    val edgeSpread: Double = 0.0,
) {
    companion object {
        const val SIZE = 52
        private const val BACKDROP = 0xFFF5EEDE.toInt()

        fun of(
            key: String,
            image: ImageBitmap,
        ): FloorLogo {
            val source = image.asAndroidBitmap()
            // Recorte cuadrado al centro y luego a la cuadrícula.
            val side = minOf(source.width, source.height)
            val square =
                Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
            val small = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
            val pixels = IntArray(SIZE * SIZE)
            small.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
            return of(key, pixels)
        }

        /** Desde los pixeles ya en la cuadrícula (ARGB). */
        fun of(
            key: String,
            pixels: IntArray,
        ): FloorLogo {
            val opaque = IntArray(pixels.size) { over(pixels[it], BACKDROP) }
            val edge = averageEdge(opaque)
            return FloorLogo(key, pixels, edge, edgeSpread(opaque, edge))
        }

        private val edgeIndices: IntArray =
            (0 until SIZE).flatMap { i -> listOf(i, (SIZE - 1) * SIZE + i, i * SIZE, i * SIZE + SIZE - 1) }.toIntArray()

        private fun averageEdge(pixels: IntArray): Int {
            var r = 0
            var g = 0
            var b = 0
            for (index in edgeIndices) {
                val color = pixels[index]
                r += (color shr 16) and 0xFF
                g += (color shr 8) and 0xFF
                b += color and 0xFF
            }
            val count = edgeIndices.size
            return (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }

        private fun edgeSpread(
            pixels: IntArray,
            average: Int,
        ): Double {
            var total = 0L
            for (index in edgeIndices) {
                val color = pixels[index]
                for (shift in intArrayOf(16, 8, 0)) {
                    total += kotlin.math.abs(((color shr shift) and 0xFF) - ((average shr shift) and 0xFF))
                }
            }
            return total.toDouble() / (edgeIndices.size * 3 * 255)
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

/**
 * Cómo va la imagen del logo en la pared del carrito. El dueño sube lo que tenga (un logo, una foto del celular,
 * una captura) sin pensar en tamaños; la app decide para que siempre llene bien la pared. Igual que
 * `CounterWallStyle` en iOS.
 */
internal enum class CounterWallStyle {
    /** Logo: entero, sobre la pared pintada del color de su orilla. */
    Logo,

    /** Foto: es toda la pared, recortada sin deformarse. */
    Photo,

    /** Foto chica: la misma foto agrandada y desenfocada de fondo, y nítida al frente, para que no se pixele. */
    SmallPhoto,
    ;

    companion object {
        /** Hasta aquí la orilla cuenta como lisa: logos con fondo de un color, aunque vengan en JPEG. */
        const val LOGO_EDGE_SPREAD = 0.06

        /** Lado corto mínimo, en pixeles, para estirar una foto a todo el ancho del teléfono sin que se vea borrosa. */
        const val PHOTO_MIN_SIDE = 600

        fun decide(
            logo: FloorLogo,
            width: Int,
            height: Int,
        ): CounterWallStyle =
            when {
                logo.edgeSpread <= LOGO_EDGE_SPREAD -> Logo
                minOf(width, height) < PHOTO_MIN_SIDE -> SmallPhoto
                else -> Photo
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

/**
 * [image] llenando [rect] sin deformarse: recorta lo que sobra. Si sobra alto, [anchorY] dice qué parte queda: 0 la
 * de arriba, 0.5 el centro ([alignTop] es lo mismo que 0).
 */
internal fun DrawScope.drawImageCover(
    image: ImageBitmap,
    rect: Rect,
    alpha: Float = 1f,
    alignTop: Boolean = false,
    anchorY: Float = 0.5f,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
) {
    if (rect.width <= 0f || rect.height <= 0f) return
    val scale = maxOf(rect.width / image.width, rect.height / image.height)
    val srcWidth = (rect.width / scale).roundToInt().coerceIn(1, image.width)
    val srcHeight = (rect.height / scale).roundToInt().coerceIn(1, image.height)
    val srcX = (image.width - srcWidth) / 2
    val srcY = if (alignTop) 0 else ((image.height - srcHeight) * anchorY).roundToInt()
    drawImage(
        image,
        srcOffset = IntOffset(srcX, srcY),
        srcSize = IntSize(srcWidth, srcHeight),
        dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
        dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
        alpha = alpha,
        filterQuality = filterQuality,
    )
}
