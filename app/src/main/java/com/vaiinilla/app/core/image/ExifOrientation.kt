package com.vaiinilla.app.core.image

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream

/** Cómo hay que girar (en grados, a favor del reloj) y si hay que voltear en espejo una foto según su EXIF. */
internal data class ExifTransform(
    val degrees: Int,
    val mirror: Boolean,
) {
    val isIdentity: Boolean get() = degrees == 0 && !mirror
}

/** Los ocho valores de `Orientation` de EXIF; cualquier otro (o sin etiqueta) se deja como viene. */
internal fun exifTransform(orientation: Int): ExifTransform =
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> ExifTransform(0, true)
        ExifInterface.ORIENTATION_ROTATE_180 -> ExifTransform(180, false)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> ExifTransform(180, true)
        ExifInterface.ORIENTATION_TRANSPOSE -> ExifTransform(90, true)
        ExifInterface.ORIENTATION_ROTATE_90 -> ExifTransform(90, false)
        ExifInterface.ORIENTATION_TRANSVERSE -> ExifTransform(270, true)
        ExifInterface.ORIENTATION_ROTATE_270 -> ExifTransform(270, false)
        else -> ExifTransform(0, false)
    }

/**
 * Las fotos de un teléfono se guardan acostadas con una etiqueta EXIF que dice cómo girarlas. iOS y los
 * navegadores la respetan; `BitmapFactory` no, y las bebidas salían volteadas solo en Android. Si no se puede leer
 * la etiqueta, la imagen se queda como viene.
 */
internal fun Bitmap.withExifOrientation(bytes: ByteArray): Bitmap {
    val orientation =
        runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val transform = exifTransform(orientation)
    if (transform.isIdentity) return this
    val matrix =
        Matrix().apply {
            if (transform.mirror) postScale(-1f, 1f)
            postRotate(transform.degrees.toFloat())
        }
    val turned = runCatching { Bitmap.createBitmap(this, 0, 0, width, height, matrix, true) }.getOrNull() ?: return this
    if (turned !== this) recycle()
    return turned
}
