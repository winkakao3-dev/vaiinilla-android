package com.vaiinilla.app.ui.screens

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.rememberProductTone
import com.vaiinilla.app.ui.order.TrayItemKind
import com.vaiinilla.app.ui.order.trayItemKind
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

// En el menú el Vainiverso es solo vidrio y luz: la foto del producto es la estrella y su color
// ilumina el vidrio por debajo. No hay escena atrás ni Vaini.

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

/** Luz del color del producto que cae por debajo de su tarjeta. */
internal fun Modifier.toneGlow(
    tone: Color,
    strength: Float,
): Modifier =
    drawBehind {
        val w = size.width
        val center = Offset(w / 2f, size.height * 0.97f)
        val radius = w * 0.62f
        // El rectángulo cubre el círculo completo del degradado, así la luz se apaga sin dejar orilla.
        drawRect(
            Brush.radialGradient(listOf(tone.copy(alpha = strength), Color.Transparent), center, radius),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
        )
    }

/** Tarjeta del menú: la foto dentro de un vidrio que se tiñe del color del producto. */
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
    Box(modifier.fillMaxWidth().toneGlow(tone, if (colors.isDark) 0.42f else 0.3f)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.glassCard)
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, tone.copy(alpha = 0.26f)),
                            startY = size.height * 0.45f,
                        ),
                    )
                }.border(1.dp, c.glassEdge, shape)
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
