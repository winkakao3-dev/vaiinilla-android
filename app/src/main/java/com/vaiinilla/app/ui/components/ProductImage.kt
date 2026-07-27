package com.vaiinilla.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.vaiinilla.app.R

@Composable
fun ProductImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Image(
        painter = painterResource(productImageResource(imageUrl)),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

@DrawableRes
fun productImageResource(imageUrl: String): Int {
    val key = imageUrl.removePrefix("fixture://")
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
        else -> R.drawable.waffle
    }
}
