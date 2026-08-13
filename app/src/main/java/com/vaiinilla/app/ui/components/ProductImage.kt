package com.vaiinilla.app.ui.components

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.vaiinilla.app.R
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProductImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (productImageIsRemote(imageUrl)) {
        var remote by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(imageUrl) {
            remote =
                withContext(Dispatchers.IO) {
                    runCatching {
                        URL(imageUrl).openStream().use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }.getOrNull()
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
            ProductImagePlaceholder(modifier)
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
            ProductImagePlaceholder(modifier)
        }
    }
}

@Composable
private fun ProductImagePlaceholder(modifier: Modifier) {
    Box(modifier = modifier.background(LocalVaiinillaColors.current.paper2))
}

fun productImageIsRemote(imageUrl: String): Boolean =
    imageUrl.startsWith("https://", ignoreCase = true) ||
        imageUrl.startsWith("http://", ignoreCase = true)

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
