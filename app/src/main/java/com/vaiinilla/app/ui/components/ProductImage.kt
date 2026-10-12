package com.vaiinilla.app.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.R
import com.vaiinilla.app.core.io.readBytesLimited
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

@Composable
fun ProductImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    /** Lo que se ve si no hay foto; por defecto el ícono con "Sin foto". */
    placeholder: @Composable (Modifier) -> Unit = { ProductImagePlaceholder(it) },
) {
    if (productImageIsRemote(imageUrl)) {
        var remote by remember(imageUrl) {
            mutableStateOf(remoteProductImageCache.get(imageUrl)?.asImageBitmap())
        }
        LaunchedEffect(imageUrl) {
            if (remote == null) {
                remote = withContext(Dispatchers.IO) { loadRemoteProductImage(imageUrl) }
            }
        }
        val bitmap = remote
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        } else {
            placeholder(modifier)
        }
    } else {
        val local = productImageResource(imageUrl)
        if (local != null) {
            Image(
                painter = painterResource(local),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        } else {
            placeholder(modifier)
        }
    }
}

/** El color que domina la foto del producto, para teñir la luz que la rodea. Null mientras no hay foto. */
@Composable
fun rememberProductTone(imageUrl: String): Color? {
    val context = LocalContext.current
    var tone by remember(imageUrl) { mutableStateOf(productToneCache[imageUrl]?.let(::Color)) }
    LaunchedEffect(imageUrl) {
        if (tone != null) return@LaunchedEffect
        val argb =
            withContext(Dispatchers.IO) {
                val bitmap =
                    if (productImageIsRemote(imageUrl)) {
                        loadRemoteProductImage(imageUrl)?.asAndroidBitmap()
                    } else {
                        productImageResource(imageUrl)?.let { res ->
                            BitmapFactory.decodeResource(
                                context.resources,
                                res,
                                BitmapFactory.Options().apply {
                                    inSampleSize =
                                        8
                                },
                            )
                        }
                    }
                bitmap?.let { full ->
                    val small = android.graphics.Bitmap.createScaledBitmap(full, TONE_SAMPLE, TONE_SAMPLE, true)
                    val pixels = IntArray(TONE_SAMPLE * TONE_SAMPLE)
                    small.getPixels(pixels, 0, TONE_SAMPLE, 0, 0, TONE_SAMPLE, TONE_SAMPLE)
                    dominantTone(pixels)
                }
            }
        if (argb != null) {
            productToneCache[imageUrl] = argb
            tone = Color(argb)
        }
    }
    return tone
}

/**
 * Promedio de los pixeles pesado por saturación: el fondo blanco o la mesa gris casi no cuentan, el
 * producto sí. Si toda la foto es gris, queda el promedio simple.
 */
internal fun dominantTone(pixels: IntArray): Int? {
    if (pixels.isEmpty()) return null
    var r = 0.0
    var g = 0.0
    var b = 0.0
    var weight = 0.0
    var plainR = 0.0
    var plainG = 0.0
    var plainB = 0.0
    for (p in pixels) {
        val pr = (p shr 16 and 0xFF) / 255.0
        val pg = (p shr 8 and 0xFF) / 255.0
        val pb = (p and 0xFF) / 255.0
        plainR += pr
        plainG += pg
        plainB += pb
        val max = maxOf(pr, pg, pb)
        val min = minOf(pr, pg, pb)
        if (max < 0.12) continue
        val sat = (max - min) / max
        val w = sat * sat * max
        r += pr * w
        g += pg * w
        b += pb * w
        weight += w
    }
    val (fr, fg, fb) =
        if (weight > pixels.size * 0.01) {
            Triple(r / weight, g / weight, b / weight)
        } else {
            Triple(plainR / pixels.size, plainG / pixels.size, plainB / pixels.size)
        }

    fun channel(v: Double) = (v.coerceIn(0.0, 1.0) * 255).toInt()
    return (0xFF shl 24) or (channel(fr) shl 16) or (channel(fg) shl 8) or channel(fb)
}

private const val TONE_SAMPLE = 16
private val productToneCache = ConcurrentHashMap<String, Int>()

private fun loadRemoteProductImage(imageUrl: String): ImageBitmap? {
    remoteProductImageCache.get(imageUrl)?.let { return it.asImageBitmap() }
    val lock = remoteProductImageLocks.computeIfAbsent(imageUrl) { Any() }
    return synchronized(lock) {
        remoteProductImageCache.get(imageUrl)?.let { return@synchronized it.asImageBitmap() }
        runCatching {
            val url = URL(imageUrl)
            val connection = url.openConnection() as? HttpURLConnection ?: return@runCatching null
            try {
                connection.connectTimeout = REMOTE_IMAGE_CONNECT_TIMEOUT_MS
                connection.readTimeout = REMOTE_IMAGE_READ_TIMEOUT_MS
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "image/*")
                val status = connection.responseCode
                if (status !in 200..299) return@runCatching null
                val contentLength = connection.contentLengthLong
                if (contentLength > REMOTE_IMAGE_MAX_BYTES) return@runCatching null
                val contentType =
                    connection.contentType
                        .orEmpty()
                        .substringBefore(';')
                        .trim()
                        .lowercase()
                if (contentType.isNotEmpty() && !contentType.startsWith("image/")) return@runCatching null
                val bytes =
                    connection.inputStream.use { input ->
                        input.readBytesLimited(REMOTE_IMAGE_MAX_BYTES)
                    } ?: return@runCatching null
                decodeBoundedImage(bytes)?.let { bitmap ->
                    remoteProductImageCache.put(imageUrl, bitmap)
                    bitmap.asImageBitmap()
                }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}

/** Warms the in-memory cache before product cards are composed. */
suspend fun prefetchProductImages(imageUrls: List<String>) =
    coroutineScope {
        val limiter = Semaphore(REMOTE_IMAGE_PREFETCH_CONCURRENCY)
        imageUrls
            .asSequence()
            .distinct()
            .filter(::productImageIsRemote)
            .map { imageUrl ->
                async(Dispatchers.IO) {
                    limiter.withPermit { loadRemoteProductImage(imageUrl) }
                }
            }.toList()
            .awaitAll()
        Unit
    }

private fun decodeBoundedImage(bytes: ByteArray): android.graphics.Bitmap? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    if (bounds.outWidth > REMOTE_IMAGE_MAX_SOURCE_DIMENSION || bounds.outHeight > REMOTE_IMAGE_MAX_SOURCE_DIMENSION) {
        return null
    }
    var sample = 1
    while (
        bounds.outWidth / sample > REMOTE_IMAGE_MAX_DECODE_DIMENSION ||
        bounds.outHeight / sample > REMOTE_IMAGE_MAX_DECODE_DIMENSION
    ) {
        sample *= 2
    }
    return BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    )
}

@Composable
private fun ProductImagePlaceholder(modifier: Modifier) {
    val colors = LocalVaiinillaColors.current
    BoxWithConstraints(
        modifier = modifier.background(colors.paper2),
        contentAlignment = Alignment.Center,
    ) {
        // En miniaturas no cabe el texto: solo el ícono, a la escala de la miniatura.
        val compact = maxWidth < 96.dp
        val iconSize = if (compact) maxWidth * 0.42f else 34.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(iconSize),
            )
            if (!compact) {
                androidx.compose.material3.Text(
                    text = "Sin foto",
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

fun productImageIsRemote(imageUrl: String): Boolean {
    if (!imageUrl.startsWith("https://", ignoreCase = true)) return false
    return runCatching {
        val url = URL(imageUrl)
        val host = url.host.trim().lowercase()
        host.isNotEmpty() &&
            host != "localhost" &&
            url.userInfo == null &&
            (url.port == -1 || url.port == 443)
    }.getOrDefault(false)
}

@DrawableRes
fun productImageResource(imageUrl: String): Int? {
    val key = imageUrl.substringAfterLast('/').substringAfterLast(':').substringBefore('?')
    return when (key) {
        "jamaica" -> R.drawable.jamaica
        "burrito_norteno" -> R.drawable.burrito_norteno
        "waffle" -> R.drawable.waffle
        "burrito_barbacoa" -> R.drawable.burrito_barbacoa
        "burrito_frijol_queso" -> R.drawable.burrito_frijol_queso
        "burrito_machaca" -> R.drawable.burrito_machaca
        "fruta" -> R.drawable.fruta
        "montado_asada" -> R.drawable.montado_asada
        "montado_chorizo" -> R.drawable.montado_chorizo
        "montado_machaca" -> R.drawable.montado_machaca
        "montado_norteno" -> R.drawable.montado_norteno
        "quesa" -> R.drawable.quesa
        "quesadilla_harina" -> R.drawable.quesadilla_harina
        "sincronizada_nortena" -> R.drawable.sincronizada_nortena
        "torta" -> R.drawable.torta
        else -> null
    }
}

private val remoteProductImageLocks = ConcurrentHashMap<String, Any>()

private val remoteProductImageCache =
    object : LruCache<String, android.graphics.Bitmap>(48 * 1024) {
        override fun sizeOf(
            key: String,
            value: android.graphics.Bitmap,
        ): Int = (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

private const val REMOTE_IMAGE_MAX_BYTES = 8 * 1024 * 1024
private const val REMOTE_IMAGE_MAX_DECODE_DIMENSION = 1280
private const val REMOTE_IMAGE_MAX_SOURCE_DIMENSION = 32_768
private const val REMOTE_IMAGE_PREFETCH_CONCURRENCY = 6
private const val REMOTE_IMAGE_CONNECT_TIMEOUT_MS = 7_000
private const val REMOTE_IMAGE_READ_TIMEOUT_MS = 10_000
