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
private fun productImageResource(imageUrl: String): Int = when (imageUrl.removePrefix("fixture://")) {
    "jamaica" -> R.drawable.jamaica
    "burrito_norteno" -> R.drawable.burrito_norteno
    "waffle" -> R.drawable.waffle
    else -> R.drawable.waffle
}
