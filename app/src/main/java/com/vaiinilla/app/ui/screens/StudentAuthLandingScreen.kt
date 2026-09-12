package com.vaiinilla.app.ui.screens

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.R
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val EaseOutLiz = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) < 0.01f
    }
}

@Composable
private fun WelcomeEntrance(
    delayMs: Int,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    travelDp: Float = 12f,
    content: @Composable () -> Unit,
) {
    val progress = remember(reduceMotion) { Animatable(if (reduceMotion) 1f else 0f) }
    val travelPx = with(LocalDensity.current) { travelDp.dp.toPx() }
    LaunchedEffect(delayMs, reduceMotion) {
        if (reduceMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            delay(delayMs.toLong())
            progress.animateTo(
                1f,
                tween(durationMillis = 460, easing = EaseOutLiz),
            )
        }
    }
    Box(
        modifier =
            modifier.graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * travelPx
            },
    ) {
        content()
    }
}

@Composable
private fun WelcomeTopBar(
    onBack: (() -> Unit)?,
    onExplore: () -> Unit,
    enabled: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
            }
        }
        Text(
            text = "Vaiinilla.",
            color = colors.ink,
            fontFamily = VaiinillaSerif,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.35).sp,
            modifier = Modifier.align(Alignment.Center),
        )
        TextButton(
            onClick = onExplore,
            enabled = enabled,
            modifier = Modifier.align(Alignment.CenterEnd).heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Text(
                "Explorar",
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class WelcomeBubbleSpec(
    @DrawableRes val drawable: Int,
    val x: Float,
    val y: Float,
    val size: Dp,
    val start: Float,
    val curve: Dp,
    val rotation: Float,
)

private val WelcomeBubbleSpecs =
    listOf(
        WelcomeBubbleSpec(R.drawable.welcome_bubble_jamaica, 0.47f, 0.06f, 34.dp, 0.05f, (-18).dp, -22f),
        WelcomeBubbleSpec(R.drawable.welcome_bubble_waffle, 0.20f, 0.08f, 32.dp, 0.10f, 22.dp, 18f),
        WelcomeBubbleSpec(R.drawable.welcome_bubble_mascot_play, 0.61f, 0.13f, 46.dp, 0.16f, (-26).dp, -28f),
        WelcomeBubbleSpec(R.drawable.welcome_bubble_mascot_karate, 0.33f, 0.15f, 44.dp, 0.22f, 24.dp, 30f),
        WelcomeBubbleSpec(R.drawable.welcome_bubble_torta, 0.76f, 0.05f, 34.dp, 0.28f, (-20).dp, -18f),
        WelcomeBubbleSpec(R.drawable.welcome_bubble_mascot_workshop, 0.30f, 0.34f, 40.dp, 0.35f, 28.dp, 26f),
        WelcomeBubbleSpec(R.drawable.welcome_bubble_mascot_laptop, 0.08f, 0.17f, 38.dp, 0.40f, (-26).dp, -32f),
        WelcomeBubbleSpec(R.drawable.welcome_bubble_burrito, 0.16f, 0.38f, 28.dp, 0.46f, 20.dp, 20f),
        WelcomeBubbleSpec(R.drawable.welcome_bubble_fruta, 0.66f, 0.36f, 32.dp, 0.52f, (-22).dp, -24f),
        WelcomeBubbleSpec(R.drawable.welcome_bubble_mascot_travel, 0.91f, 0.19f, 38.dp, 0.56f, 24.dp, 32f),
        WelcomeBubbleSpec(R.drawable.welcome_bubble_quesadilla, 0.85f, 0.40f, 28.dp, 0.58f, (-18).dp, -20f),
    )

@Composable
private fun WelcomeMascotStage(
    compact: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val density = LocalDensity.current
    val stageHeight = if (compact) 300.dp else 350.dp
    val mascotSize = if (compact) 186.dp else 218.dp
    val timeline = remember(reduceMotion) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            timeline.snapTo(1f)
        } else {
            timeline.snapTo(0f)
            delay(100)
            timeline.animateTo(
                1f,
                tween(durationMillis = 2300, easing = LinearEasing),
            )
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(stageHeight)) {
        val originX = maxWidth * 0.5f
        val originY = maxHeight * 0.66f
        WelcomeBubbleSpecs.forEach { spec ->
            val size = spec.size * if (compact) 1.20f else 1.22f
            val targetX = maxWidth * spec.x - size / 2
            val targetY = maxHeight * spec.y - size / 2
            val fromXPx = with(density) { (originX - (targetX + size / 2)).toPx() }
            val fromYPx = with(density) { (originY - (targetY + size / 2)).toPx() }
            val curvePx = with(density) { spec.curve.toPx() }
            val liftPx = with(density) { 8.dp.toPx() }
            Image(
                painter = painterResource(spec.drawable),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .offset(x = targetX, y = targetY)
                        .size(size)
                        .graphicsLayer {
                            val raw =
                                if (reduceMotion) {
                                    1f
                                } else {
                                    ((timeline.value - spec.start) / 0.38f).coerceIn(0f, 1f)
                                }
                            val progress = EaseOutLiz.transform(raw)
                            val arc = sin(PI.toFloat() * progress)
                            alpha = (raw * 4f).coerceIn(0f, 1f)
                            translationX = fromXPx * (1f - progress) + curvePx * arc
                            translationY = fromYPx * (1f - progress) - liftPx * arc
                            rotationZ = spec.rotation * (1f - progress)
                            val scale = 0.18f + 0.82f * progress
                            scaleX = scale
                            scaleY = scale
                            shape = CircleShape
                            clip = true
                        }
                        .border(
                            1.dp,
                            colors.paper.copy(alpha = if (colors.isDark) 0.58f else 0.82f),
                            CircleShape,
                        ),
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = if (compact) (-4).dp else (-8).dp)
                    .size(mascotSize + 18.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = if (colors.isDark) 0.055f else 0.075f)),
        )
        VaiinillaMascot(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = if (compact) (-4).dp else (-8).dp)
                    .size(mascotSize),
            delayMs = 70,
            paperColor = Color(0xFFF5ECDA),
            accentColor = colors.accent,
            inkColor = Color(0xFF1D1C18),
            reduceMotion = reduceMotion,
        )
    }
}

@Composable
fun StudentAuthLandingScreen(
    state: StudentAuthUiState,
    onBack: (() -> Unit)?,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onExplore: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = rememberReduceMotion()
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(colors.paper),
    ) {
        val compact = maxHeight < 820.dp
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) {
            WelcomeEntrance(delayMs = 0, reduceMotion = reduceMotion, travelDp = 6f) {
                WelcomeTopBar(
                    onBack = onBack,
                    onExplore = onExplore,
                    enabled = !state.loading,
                )
            }
            Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
            WelcomeEntrance(delayMs = 70, reduceMotion = reduceMotion, travelDp = 10f) {
                WelcomeMascotStage(
                    compact = compact,
                    reduceMotion = reduceMotion,
                )
            }
            WelcomeEntrance(
                delayMs = 260,
                reduceMotion = reduceMotion,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Tu cafetería,",
                        color = colors.ink,
                        fontFamily = VaiinillaSerif,
                        fontSize = if (compact) 38.sp else 43.sp,
                        lineHeight = if (compact) 40.sp else 45.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1.1).sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "a tu ritmo.",
                        color = colors.ink,
                        fontFamily = VaiinillaSerif,
                        fontSize = if (compact) 38.sp else 43.sp,
                        lineHeight = if (compact) 40.sp else 45.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-1.0).sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Pide, sigue tu pedido y paga desde un solo lugar.",
                        color = colors.muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = if (compact) 10.dp else 14.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            WelcomeEntrance(
                delayMs = 500,
                reduceMotion = reduceMotion,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = if (compact) 10.dp else 18.dp,
                            ),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    state.errorMessage?.let { error ->
                        AuthErrorBanner(error)
                    }
                    EditorialAccentButton(
                        text = "Crear cuenta",
                        onClick = onRegister,
                        enabled = !state.loading,
                    )
                    GooglePillSignInButton(
                        onClick = onGoogleSignIn,
                        enabled = !state.loading,
                        background = colors.paper2,
                        border = colors.line,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "¿Ya tienes cuenta?",
                            color = colors.muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        TextButton(
                            onClick = onLogin,
                            enabled = !state.loading,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        ) {
                            Text(
                                "Iniciar sesión",
                                color = colors.ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxSize().background(colors.paper.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = colors.accent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/** Full-width secondary auth surface with the same height and radius as the primary CTA. */
@Composable
private fun AuthPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    background: Color,
    border: Color,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .physicalPress(enabled = enabled, onClick = onClick)
                .clip(shape)
                .background(background)
                .border(1.dp, border, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Full-width Google option using the official multicolor mark and explicit action label. */
@Composable
private fun GooglePillSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    background: Color,
    border: Color,
) {
    AuthPillButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = background,
        border = border,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                "Continuar con Google",
                color = LocalVaiinillaColors.current.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(name = "Bienvenida V2 · claro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentAuthLandingScreenLightPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentAuthLandingScreen(
            state = StudentAuthUiState(),
            onBack = null,
            onRegister = {},
            onLogin = {},
            onGoogleSignIn = {},
            onExplore = {},
        )
    }
}

@Preview(name = "Bienvenida V2 · oscuro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentAuthLandingScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        StudentAuthLandingScreen(
            state = StudentAuthUiState(),
            onBack = {},
            onRegister = {},
            onLogin = {},
            onGoogleSignIn = {},
            onExplore = {},
        )
    }
}
