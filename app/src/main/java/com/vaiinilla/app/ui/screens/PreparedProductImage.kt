package com.vaiinilla.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.vaiinilla.app.core.io.readBytesLimited
import java.io.ByteArrayOutputStream

internal data class PreparedProductImage(
    val bytes: ByteArray,
    val filename: String,
    val mimeType: String,
)

internal fun prepareProductImage(
    context: android.content.Context,
    uri: Uri,
): PreparedProductImage? {
    val original =
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytesLimited(MAX_SOURCE_PRODUCT_IMAGE_BYTES)
        } ?: return null
    if (original.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(original, 0, original.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    val sample = if (longest <= 1600) 1 else Integer.highestOneBit((longest - 1) / 1600)
    val decoded =
        BitmapFactory.decodeByteArray(
            original,
            0,
            original.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null
    return try {
        var quality = 88
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            if (!decoded.compress(Bitmap.CompressFormat.JPEG, quality, out)) return null
            bytes = out.toByteArray()
            quality -= 8
        } while (bytes.size > MAX_PRODUCT_IMAGE_BYTES && quality >= 50)
        if (bytes.size > MAX_PRODUCT_IMAGE_BYTES) return null
        PreparedProductImage(bytes, "producto.jpg", "image/jpeg")
    } finally {
        decoded.recycle()
    }
}

private const val MAX_PRODUCT_IMAGE_BYTES = 5 * 1024 * 1024
private const val MAX_SOURCE_PRODUCT_IMAGE_BYTES = 16 * 1024 * 1024
