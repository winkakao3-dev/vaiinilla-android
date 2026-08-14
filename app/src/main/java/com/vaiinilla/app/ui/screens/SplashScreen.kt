package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.R
import com.vaiinilla.app.ui.theme.DarkSplash
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.delay

private val BootEaseIn = CubicBezierEasing(0.22f, 0.8f, 0.25f, 1f)
private val BootEaseExpand = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val colors = LocalVaiinillaColors.current
    val themeMode = LocalVaiinillaThemeMode.current
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveMode = themeMode.resolveEffectiveMode(isSystemDark)
    val isDarkTheme = effectiveMode == VaiinillaThemeMode.Dark || effectiveMode == VaiinillaThemeMode.Amoled
    val splashBackground = if (isDarkTheme) DarkSplash else colors.paper
    val splashLogoRes = if (isDarkTheme) R.drawable.logo_splash_dark else R.drawable.logo_splash_light

    val iconIn = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.85f) }
    val splashAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        iconIn.animateTo(1f, tween(450, easing = BootEaseIn))
        iconScale.animateTo(1f, tween(450, easing = BootEaseIn))
        delay(600)
        iconScale.animateTo(1.6f, tween(500, easing = BootEaseExpand))
        iconIn.animateTo(0f, tween(500, easing = BootEaseExpand))
        splashAlpha.animateTo(0f, tween(350))
        onFinished()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(splashAlpha.value)
                .background(splashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        alpha = iconIn.value
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = splashLogoRes),
                contentDescription = "Vaiinilla",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Preview(name = "Splash", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun SplashScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        SplashScreen(onFinished = {})
    }
}
