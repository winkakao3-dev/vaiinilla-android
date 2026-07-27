package com.vaiinilla.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.Lime

@Composable
fun VaiinillaMark(
    modifier: Modifier = Modifier,
    cream: Color = Cream,
    leafA: Color = Lime,
    leafB: Color = Color(0xFF8FB84E),
    coral: Color = Coral,
) {
    Canvas(modifier = modifier) {
        val scale = minOf(size.width / 510f, size.height / 420f)
        withTransform({
            scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
            translate(-410f, -420f)
        }) {
            drawPath(VaiinillaMarkPaths.cream, cream)
            drawPath(VaiinillaMarkPaths.leafA, leafA)
            drawPath(VaiinillaMarkPaths.leafB, leafB)
            drawPath(VaiinillaMarkPaths.coral, coral)
        }
    }
}

private object VaiinillaMarkPaths {
    val cream: Path =
        Path().apply {
            parseSvgPath(
                "M423.405 438.866H503.841C536.212 438.866 562.697 455.542 580.354 485.951L706.894 718.431L645.095 825.352L423.405 438.866Z",
            )
        }
    val leafA: Path =
        Path().apply {
            parseSvgPath(
                "M658.828 596.796C676.485 561.482 696.103 523.226 715.722 491.836C735.341 459.466 765.749 438.866 803.025 438.866H867.766L832.452 500.665C811.853 535.978 777.52 557.559 735.341 563.444C704.932 567.368 676.485 577.177 658.828 596.796Z",
            )
        }
    val leafB: Path =
        Path().apply {
            parseSvgPath(
                "M787.33 577.92C760.845 581.844 730.436 584.786 710.818 595.577C695.123 604.405 688.256 622.062 693.161 639.718C698.066 658.356 712.78 682.879 723.57 698.574L787.33 577.92Z",
            )
        }
    val coral: Path =
        Path().apply {
            parseSvgPath(
                "M876 444.262C876 451.029 881.494 456.523 888.262 456.523C895.029 456.523 900.523 451.029 900.523 444.262C900.523 437.494 895.029 432 888.262 432C881.494 432 876 437.494 876 444.262Z",
            )
        }
}

private fun Path.parseSvgPath(data: String) {
    androidx.compose.ui.graphics.vector
        .PathParser()
        .parsePathString(data)
        .toPath(this)
}
