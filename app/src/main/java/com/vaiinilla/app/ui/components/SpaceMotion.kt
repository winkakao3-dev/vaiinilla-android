package com.vaiinilla.app.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/**
 * Las cosas llegan, no aparecen: opacidad + 8 dp de viaje + un desenfoque que se aclara, con un
 * escalonado de ~40 ms por posición. Es un resorte, así que se puede interrumpir sin saltos.
 * Con animaciones del sistema apagadas entra ya asentado.
 */
fun Modifier.arrive(
    index: Int = 0,
    key: Any? = Unit,
): Modifier =
    composed {
        val reduced = reducedMotion()
        val progress = remember(key) { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(key) {
            if (!reduced) {
                delay(index.coerceAtLeast(0) * ARRIVE_STAGGER_MS)
                progress.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 180f))
            }
        }
        val settled = progress.value.coerceIn(0f, 1f)
        val blurRadius = ((1f - settled) * ARRIVE_BLUR_DP).dp
        val base =
            this.graphicsLayer {
                alpha = settled
                translationY = (1f - settled) * ARRIVE_TRAVEL_DP.dp.toPx()
            }
        // El desenfoque necesita RenderEffect (Android 12+); antes solo se omite.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.2.dp) base.blur(blurRadius) else base
    }

private const val ARRIVE_STAGGER_MS = 40L
private const val ARRIVE_BLUR_DP = 6f
private const val ARRIVE_TRAVEL_DP = 8f

/**
 * Texto cuyas cifras ruedan una a una. Cada columna se identifica por su posición desde la
 * derecha, así al cambiar de "9.50" a "10.50" solo entra una columna nueva y las demás ruedan.
 */
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                NumberTicker(value = char, label = "rolling-char") { current ->
                    Text(current.toString(), style = style, color = color)
                }
            }
        }
    }
}

/**
 * Barra fina de avance con resorte: nunca salta, se re-apunta a mitad de camino. Sirve para el
 * turno de una cancha (cuánto se ha jugado del total).
 */
@Composable
fun TurnProgressBar(
    fraction: Float,
    track: Color,
    fill: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val reduced = reducedMotion()
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = if (reduced) snap() else spring(dampingRatio = 1f, stiffness = 120f),
        label = "turn-progress",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(fill),
        )
    }
}

/** Cuánto del turno ya pasó (0..1), o null si no hay un turno con inicio y fin válidos. */
fun turnFraction(
    startedAtIso: String?,
    endsAtIso: String?,
): Float? {
    if (startedAtIso == null || endsAtIso == null) return null
    return runCatching {
        val start = Instant.parse(startedAtIso)
        val end = Instant.parse(endsAtIso)
        val total = Duration.between(start, end).toMillis().toFloat()
        if (total <= 0f) return null
        (Duration.between(start, Instant.now()).toMillis() / total).coerceIn(0f, 1f)
    }.getOrNull()
}

/** "42:10" o "1:05:10": el tiempo que falta, en reloj, para que las cifras rueden. */
fun clockText(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1_000).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$mm:$ss" else "$mm:$ss"
}

/** Milisegundos que faltan para `iso`, o null si no es una fecha válida. */
fun millisUntil(iso: String?): Long? =
    iso?.let {
        runCatching { Duration.between(Instant.now(), Instant.parse(it)).toMillis() }.getOrNull()
    }
