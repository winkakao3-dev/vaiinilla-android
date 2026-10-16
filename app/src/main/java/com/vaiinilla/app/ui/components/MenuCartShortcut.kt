package com.vaiinilla.app.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone

/**
 * Atajo al carrito sobre el dock, del lado del carrito: el ícono del dock solo no se entendía. Sale de
 * esa esquina cuando hay algo en el carrito, salta un poco con cada producto que entra (el número y el
 * total ruedan) y se regresa a la esquina al vaciarse o al abrir la ficha de un platillo.
 */
@Composable
fun MenuCartShortcut(
    count: Int,
    /** Total ya con formato de dinero ("$325"). */
    total: String,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val shown = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible) {
        shown.animateTo(
            if (visible) 1f else 0f,
            if (reduced) snap() else spring(dampingRatio = 0.62f, stiffness = 420f),
        )
    }
    val bump = remember { Animatable(1f) }
    var lastCount by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        val grew = count > lastCount
        lastCount = count
        if (grew && visible && !reduced) {
            bump.animateTo(1.08f, spring(dampingRatio = 1f, stiffness = 900f))
            bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 420f))
        }
    }
    if (!visible && shown.value <= 0.001f) return

    val settled = shown.value.coerceIn(0f, 1f)
    val blurRadius = ((1f - settled) * 8f).dp
    val layer =
        modifier.graphicsLayer {
            transformOrigin = TransformOrigin(1f, 1f)
            val scale = (0.8f + 0.2f * shown.value) * bump.value
            scaleX = scale
            scaleY = scale
            alpha = settled
            translationY = (1f - settled) * 24.dp.toPx()
        }
    val withBlur =
        if (!reduced && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.2.dp) {
            layer.blur(blurRadius)
        } else {
            layer
        }
    SeaGlassButton(
        tone = SeaGlassTone.lima,
        onClick = onClick,
        enabled = visible,
        modifier =
            withBlur
                .heightIn(min = 52.dp)
                .semantics { contentDescription = "Pedir ya: ${productsLabel(count)}, $total. Abre el carrito" },
    ) {
        val ink = LocalContentColor.current
        Box(
            modifier =
                Modifier
                    .padding(start = 8.dp)
                    .clip(CircleShape)
                    .background(ink.copy(alpha = 0.14f))
                    .height(34.dp)
                    .width(34.dp),
            contentAlignment = Alignment.Center,
        ) {
            NumberTicker(value = count, label = "cart-shortcut-count") { value ->
                Text("$value", fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
        Text(
            "Pedir ya",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 10.dp),
        )
        Spacer(Modifier.width(12.dp))
        RollingText(
            text = total,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = ink,
            modifier = Modifier.padding(end = 18.dp),
        )
    }
}

private fun productsLabel(count: Int): String = if (count == 1) "1 producto" else "$count productos"
