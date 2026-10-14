package com.vaiinilla.app.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.common.BitMatrix
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

// "Recargar en caja" en el Vainiverso, como `WalletCodeOverlay.swift` en iOS: la cámara se acerca al frasco de
// tesoros y el frasco se pixela hasta formar tu QR personal. Cuando Caja registra la recarga, el QR se deshace de
// vuelta en el frasco y caen las monedas (eso lo hace `WalletJarBackdrop` al recibir el saldo nuevo). Mismo motor
// que la recogida (`SceneMosaic.kt`).

private const val CAMERA_FROM = 0.04f
private const val CAMERA_TO = 0.5f
private const val HANDOFF = 0.4f

/** De la casita completa al frasco, que termina del tamaño y en el lugar del QR. */
private fun codeCamera(
    p: Float,
    stage: Size,
): SceneCamera {
    val jar = walletJarRect(stage)
    val target = qrCodeRect(stage)
    return SceneCamera.flight(
        focus = jar.center,
        target = target.center,
        zoom = target.width / jar.width,
        t = SceneCurve.eased(p, CAMERA_FROM, CAMERA_TO),
    )
}

/** El QR personal y los colores del frasco que tenía debajo al pasar la cámara por [HANDOFF]. */
private class WalletCodeMosaic(
    val cells: SceneMosaic,
    val sceneCenter: Offset,
    val sceneSide: Float,
)

/**
 * El QR personal de la cartera, encima de la casita. [shown] abre o cierra con un solo resorte que se puede
 * interrumpir; cerrar regresa el vuelo. [fill] y [label] son los del frasco mientras el QR está abierto.
 */
@Composable
internal fun WalletCodeOverlay(
    matrix: BitMatrix,
    shown: Boolean,
    fill: Float,
    label: String?,
    onClose: () -> Unit,
    /** Solo para capturas: deja el vuelo quieto en ese punto. */
    frozenAt: Float? = null,
) {
    val palette = LocalScene.current
    val reduceMotion = reducedMotion()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val p = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(shown) {
        val target = if (shown) 1f else 0f
        if (reduceMotion) p.snapTo(target) else p.animateTo(target, spring(dampingRatio = 1f, stiffness = 14f))
    }
    BackHandler(enabled = shown, onBack = onClose)
    ScreenBrightnessBoost(active = shown)
    val progress = frozenAt ?: p.value
    if (progress <= 0.001f && !shown) return

    val home = JarMotion(blink = 1f, happy = false, fill = fill, falling = null, label = label)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // Aparece en el primer instante del vuelo, mientras la hoja de la cartera baja.
            .graphicsLayer { alpha = min(progress * 12f, 1f) }
            .background(palette.stage),
    ) {
        val stage = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val mosaic =
            remember(matrix, stage, palette, label, fill) {
                val camera = codeCamera(HANDOFF, stage)
                val sceneCenter = camera.toScene(qrCodeRect(stage).center)
                val sceneSide = walletJarRect(stage).width
                val cells =
                    SceneMosaic.build(matrix, stage, sceneCenter, sceneSide, density, palette.stage) { size ->
                        drawVainiHome(size, textMeasurer, palette, home)
                    }
                WalletCodeMosaic(cells, sceneCenter, sceneSide)
            }
        val camera = codeCamera(progress, stage)
        val sceneAlpha = 1f - SceneCurve.segment(progress, 0.46f, 0.62f)
        val speedBlur = if (reduceMotion) 0f else 10f * sin(PI.toFloat() * SceneCurve.segment(progress, 0.03f, 0.5f))
        if (sceneAlpha > 0.001f) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = camera.zoom
                        scaleY = camera.zoom
                        translationX = camera.translation.x
                        translationY = camera.translation.y
                        alpha = sceneAlpha
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && speedBlur > 0.5f) {
                            renderEffect = BlurEffect(speedBlur, speedBlur, TileMode.Clamp)
                        }
                    },
            ) {
                drawVainiHome(size, textMeasurer, palette, home)
            }
        }
        if (progress > 0.3f) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Tu código personal de Cartera" },
            ) {
                drawFlightMosaic(
                    mosaic.cells,
                    mosaic.sceneCenter,
                    mosaic.sceneSide,
                    codeCamera(progress, size),
                    qrCodeRect(size),
                    progress,
                )
            }
        }
        WalletCodeChrome(p = progress, onClose = onClose)
    }
}

/** El vidrio sobre el QR: el botón para cerrar arriba y qué hacer con el código abajo. */
@Composable
private fun WalletCodeChrome(
    p: Float,
    onClose: () -> Unit,
) {
    val c = LocalScene.current
    val shown = SceneCurve.eased(p, 0.78f, 1f)
    val button = SceneCurve.eased(p, 0.2f, 0.6f)
    var height by remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxSize()) {
        GlassPanel(
            backdrop = null,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { height = it.height.toFloat() }
                    .graphicsLayer {
                        translationY = (height + 40.dp.toPx()) * (1f - shown)
                        alpha = shown
                    },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
            ) {
                Text(
                    "Muéstralo en Caja",
                    color = c.ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                )
                Text(
                    "Caja lo escanea para encontrar tu cuenta y registrar la recarga.",
                    color = c.muted,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(14.dp))
                WaitingForReload()
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LightMode, contentDescription = null, tint = c.muted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Subimos el brillo mientras está abierto. El código solo identifica tu cuenta.",
                        color = c.muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
        SeaGlassButton(
            tone = SeaGlassTone.vainilla,
            onClick = onClose,
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .size(42.dp)
                    .graphicsLayer {
                        val scale = 0.6f + 0.4f * button
                        scaleX = scale
                        scaleY = scale
                        alpha = button
                    }.semantics { contentDescription = "Cerrar código" },
            shape = CircleShape,
        ) {
            Icon(Icons.Filled.Close, contentDescription = null)
        }
    }
}

/** "Esperando la recarga" con un punto que respira: la caja todavía no la registra. */
@Composable
private fun WaitingForReload() {
    val c = LocalScene.current
    val reduceMotion = reducedMotion()
    val breathing = rememberInfiniteTransition(label = "waiting_reload")
    val wave by breathing.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "breath",
    )
    val breath = if (reduceMotion) 0f else wave
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(9.dp + 10.dp * breath)
                    .background(SceneLime.copy(alpha = 0.35f * (1f - breath)), CircleShape),
            )
            Box(Modifier.size(9.dp).background(SceneLime, CircleShape))
        }
        Spacer(Modifier.width(9.dp))
        Text("Esperando la recarga", color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}
