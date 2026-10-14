package com.vaiinilla.app.ui.screens

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.vaiinilla.app.ui.components.productImageIsRemote
import com.vaiinilla.app.ui.components.productImageResource
import com.vaiinilla.app.ui.components.rememberProductTone
import com.vaiinilla.app.ui.order.TrayItemKind
import com.vaiinilla.app.ui.order.trayItemKind
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

// En el menú el Vainiverso es solo vidrio y luz: la foto del producto es la estrella y su propia
// imagen desenfocada es la luz que la rodea. No hay escena atrás ni Vaini. Sin foto no hay luz: el
// color se gana con la foto, no tiñe el vidrio.

/** Si el producto trae una foto (remota o del paquete). Sin ella no hay luz de color ni tono de fondo. */
internal fun hasProductPhoto(product: Product): Boolean =
    productImageIsRemote(product.imageUrl) || productImageResource(product.imageUrl) != null

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
 * desenfoque (Android < 12) queda una sombra de elevación teñida con el color de la foto. Sin foto no dibuja nada.
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
    if (!hasProductPhoto(product)) return
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
                placeholder = {},
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

/** Producto sin foto en la tarjeta del menú: el producto dibujado al centro. [drop] lo hace bajar al llegar. */
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

/**
 * Mientras el producto no tenga foto: un plano neutro con su inicial. No imita una foto ni repite el mismo dibujo en
 * todo el menú, así cada producto se distingue por su nombre.
 */
@Composable
internal fun ProductMonogram(
    name: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    BoxWithConstraints(modifier.background(colors.paper2), contentAlignment = Alignment.Center) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "",
            color = colors.muted.copy(alpha = 0.55f),
            fontWeight = FontWeight.Black,
            fontSize = (maxWidth.value * 0.38f).sp,
            letterSpacing = (-1).sp,
        )
    }
}
