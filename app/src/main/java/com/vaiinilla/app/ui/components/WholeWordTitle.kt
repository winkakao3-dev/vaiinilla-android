package com.vaiinilla.app.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Título grande que nunca parte una palabra (EQUIPO-16): con letra grande, «AUTOSERVICIO» salía como
 * «AUTOSERVICI / O». Baja el tamaño hasta que la palabra más larga quepa en un renglón; las demás
 * palabras pasan completas al siguiente.
 */
@Composable
fun WholeWordTitle(
    text: String,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit,
    maxLines: Int,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Black,
    minFontSize: TextUnit = 18.sp,
) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val base = LocalTextStyle.current
        val maxWidth = constraints.maxWidth
        val scale =
            remember(text, maxWidth, density, base, fontSize, letterSpacing, fontWeight) {
                val words = text.split(' ').filter { it.isNotEmpty() }
                var factor = 1f
                while (fontSize.value * factor > minFontSize.value) {
                    val style =
                        base.merge(
                            fontSize = fontSize * factor,
                            letterSpacing = letterSpacing * factor,
                            fontWeight = fontWeight,
                        )
                    val width =
                        words.maxOfOrNull { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width }
                            ?: 0
                    if (width <= maxWidth) break
                    factor -= 0.05f
                }
                factor
            }
        Text(
            text = text,
            color = color,
            fontWeight = fontWeight,
            fontSize = fontSize * scale,
            lineHeight = lineHeight * scale,
            letterSpacing = letterSpacing * scale,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
