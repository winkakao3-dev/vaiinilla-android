package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.order.trayItemKind

// El menú en el Vainiverso es la pared de repisas de la tienda: aquí la estrella es lo que se vende, no
// Vaini. Cada producto vive en su nicho de madera con luz propia; sin foto, se dibuja sobre la repisa.

private val TagCream = Color(0xFFF6EFDD)
private val TagInk = Color(0xFF1A1414)
private val TagLime = Color(0xFFA8D52C)

/** La pared de madera detrás de todo el menú. Va dentro de un `LocalScene` ya provisto. */
@Composable
internal fun MenuShelfWall(modifier: Modifier = Modifier) {
    val c = LocalScene.current
    Canvas(
        modifier
            .fillMaxSize()
            .background(c.facade),
    ) {
        val hair = 1.dp.toPx()
        val plank = size.width / 7f
        var x = 0f
        var i = 0
        while (x < size.width) {
            drawRect(
                if (i % 2 == 0) c.facade else lerpColor(c.facade, c.facadeShade, 0.25f),
                topLeft = Offset(x, 0f),
                size = Size(plank, size.height),
            )
            drawLine(c.facadeShade.copy(alpha = 0.6f), Offset(x, 0f), Offset(x, size.height), strokeWidth = hair)
            x += plank
            i++
        }
        // Luz cálida que baja de arriba, como si hubiera lámparas sobre la pared.
        drawRect(
            Brush.verticalGradient(listOf(c.lampGlow.copy(alpha = c.lampGlow.alpha * 0.6f), Color.Transparent), endY = size.height * 0.45f),
            size = size,
        )
    }
}

private fun lerpColor(
    a: Color,
    b: Color,
    t: Float,
): Color = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha)

/** Un producto en su nicho iluminado, con el precio colgando en una etiqueta. */
@Composable
internal fun ProductNiche(
    product: Product,
    modifier: Modifier = Modifier,
) {
    val c = LocalScene.current
    val kind = trayItemKind(product.name)
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .border(1.dp, c.glassEdge, shape),
    ) {
        Canvas(Modifier.fillMaxSize()) { drawNiche(c) }
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(16.dp)),
            placeholder = { m ->
                // Sin foto: el producto dibujado, apoyado en la repisa del nicho.
                Canvas(m) {
                    val shelfTop = size.height * 0.8f
                    drawTrayItem(kind, size.width / 2f, shelfTop, size.width * 0.46f, alpha = 1f)
                }
            },
        )
        Text(
            moneyLabel(product.digitalPrice),
            color = TagInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TagCream)
                    .border(width = 2.dp, color = TagLime, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun DrawScope.drawNiche(c: ScenePalette) {
    val w = size.width
    val h = size.height
    val hair = 1.dp.toPx()
    // Fondo del nicho con la luz que cae desde arriba al centro.
    drawRect(Brush.verticalGradient(listOf(c.shopLightDeep, c.shopLight), endY = h), size = size)
    drawCircle(
        Brush.radialGradient(listOf(c.lampGlow, Color.Transparent), Offset(w / 2f, 0f), w * 0.7f),
        radius = w * 0.7f,
        center = Offset(w / 2f, 0f),
    )
    // Sombra de la orilla de arriba y de los lados: se lee como un hueco en la pared.
    drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent), endY = h * 0.12f), size = Size(w, h * 0.12f))
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent), endX = w * 0.1f), size = Size(w * 0.1f, h))
    drawRect(
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.12f)), startX = w * 0.9f, endX = w),
        topLeft = Offset(w * 0.9f, 0f),
        size = Size(w * 0.1f, h),
    )
    // Repisa.
    val shelfTop = h * 0.8f
    drawRect(c.shopShelf, topLeft = Offset(0f, shelfTop), size = Size(w, h - shelfTop))
    drawLine(Color.White.copy(alpha = 0.3f), Offset(0f, shelfTop), Offset(w, shelfTop), strokeWidth = hair * 1.5f)
    drawRect(Color.Black.copy(alpha = 0.12f), topLeft = Offset(0f, shelfTop + h * 0.06f), size = Size(w, h - shelfTop - h * 0.06f))
}
