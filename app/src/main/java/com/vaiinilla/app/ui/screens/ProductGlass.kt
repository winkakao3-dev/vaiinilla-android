package com.vaiinilla.app.ui.screens

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.rememberProductTone
import com.vaiinilla.app.ui.order.TrayItemKind
import com.vaiinilla.app.ui.order.trayItemKind
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

// En el menú el Vainiverso es solo vidrio y luz: la foto del producto es la estrella y su propia
// imagen desenfocada es la luz que la rodea. No hay escena atrás ni Vaini.

internal val TagCream = Color(0xFFF6EFDD)
internal val TagInk = Color(0xFF1A1414)
internal val TagLime = Color(0xFFA8D52C)

/** Color de la luz cuando el producto aún no tiene foto, según lo que es. */
internal fun fallbackTone(kind: TrayItemKind): Color =
    when (kind) {
        TrayItemKind.DRINK -> Color(0xFFB07A4F)
        TrayItemKind.PASTRY -> Color(0xFFD9A441)
        TrayItemKind.PLATE -> Color(0xFF8DBA3A)
    }

@Composable
internal fun productTone(product: Product): Color =
    rememberProductTone(product.imageUrl) ?: fallbackTone(trayItemKind(product.name))

/**
 * Sombra hecha de la propia foto: la misma imagen, desenfocada, más chica y un poco abajo, como las
 * portadas de Apple Music. Así la luz trae los colores reales de cada parte del producto. Sin
 * desenfoque (Android < 12) queda una sombra de elevación teñida con el color de la foto.
 */
@Composable
internal fun ProductPhotoShadow(
    product: Product,
    tone: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    blur: Dp = 26.dp,
    drop: Dp = 12.dp,
    inset: Dp = 12.dp,
    strength: Float = 0.65f,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Box(
            modifier
                .padding(horizontal = inset)
                .graphicsLayer {
                    translationY = drop.toPx()
                    alpha = strength
                    clip = false
                    renderEffect = BlurEffect(blur.toPx(), blur.toPx(), TileMode.Decal)
                },
        ) {
            ProductImage(
                imageUrl = product.imageUrl,
                contentDescription = "",
                modifier = Modifier.fillMaxSize().clip(shape),
                placeholder = { m -> Box(m.background(tone)) },
            )
        }
    } else {
        Box(modifier.shadow(18.dp, shape, ambientColor = tone, spotColor = tone))
    }
}

/** Canto de vidrio: brilla arriba, donde pega la luz, y se apaga hacia abajo. */
internal fun Modifier.glassEdge(
    edge: Color,
    shape: Shape,
): Modifier =
    border(
        1.dp,
        Brush.verticalGradient(listOf(edge, edge.copy(alpha = edge.alpha * 0.2f))),
        shape,
    )

/** Tarjeta del menú: la foto dentro de un vidrio, con la luz de la propia foto debajo. */
@Composable
internal fun ProductGlassCard(
    product: Product,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val c = LocalScene.current
    val tone = productTone(product)
    val kind = trayItemKind(product.name)
    val shape = RoundedCornerShape(26.dp)
    Box(modifier.fillMaxWidth()) {
        ProductPhotoShadow(
            product = product,
            tone = tone,
            shape = shape,
            modifier = Modifier.matchParentSize(),
            drop = 14.dp,
            strength = if (colors.isDark) 0.75f else 0.55f,
        )
        // Vidrio denso: la luz se ve alrededor y debajo, y adentro apenas tiñe.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.glassTint)
                .glassEdge(c.glassEdge, shape)
                .padding(7.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(tone.copy(alpha = 0.16f)),
            ) {
                ProductImage(
                    imageUrl = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = { m -> ProductIllustration(kind, m) },
                )
                PriceTag(
                    moneyLabel(product.digitalPrice),
                    Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }
            Text(
                text = product.name,
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp, top = 10.dp, end = 6.dp, bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun PriceTag(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        label,
        color = TagInk,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(TagCream)
                .border(2.dp, TagLime, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Mientras el producto no tenga foto: el producto dibujado al centro. [drop] lo hace bajar al llegar. */
@Composable
internal fun ProductIllustration(
    kind: TrayItemKind,
    modifier: Modifier = Modifier,
    drop: () -> Float = { 1f },
) {
    Canvas(modifier) {
        val d = drop()
        val u = minOf(size.width, size.height) * 0.4f
        val base = size.height * 0.74f - (1f - d) * size.height * 0.4f
        drawTrayItem(kind, size.width / 2f, base, u, alpha = (d * 2.5f).coerceIn(0f, 1f))
    }
}
