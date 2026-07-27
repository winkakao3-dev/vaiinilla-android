package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.theme.DarkSplash
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.delay

private val BootEaseIn = CubicBezierEasing(0.22f, 0.8f, 0.25f, 1f)
private val BootEaseExpand = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val colors = LocalVaiinillaColors.current
    val themeMode = LocalVaiinillaThemeMode.current
    val splashBackground =
        when (themeMode) {
            VaiinillaThemeMode.Dark, VaiinillaThemeMode.Amoled -> DarkSplash
            VaiinillaThemeMode.Light -> colors.paper
        }
    val iconIn = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.7f) }
    val splashAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        iconIn.animateTo(1f, tween(450, easing = BootEaseIn))
        iconScale.animateTo(1f, tween(450, easing = BootEaseIn))
        delay(500)
        iconScale.animateTo(2.75f, tween(700, easing = BootEaseExpand))
        iconIn.animateTo(0f, tween(700, easing = BootEaseExpand))
        splashAlpha.animateTo(0f, tween(400))
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
                    .size(96.dp)
                    .graphicsLayer {
                        alpha = iconIn.value
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    },
            contentAlignment = Alignment.Center,
        ) {
            VaiinillaMark(modifier = Modifier.fillMaxSize())
        }
    }
}
