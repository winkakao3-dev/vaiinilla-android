package com.vaiinilla.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Control segmentado con UN solo indicador que se desliza al segmento elegido.
 * El indicador se dibuja en la fase de draw: animar no recompone.
 */
@Composable
fun SlidingSegments(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    trackColor: Color,
    indicatorColor: Color,
    selectedContentColor: Color,
    idleContentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    verticalPadding: Dp = 10.dp,
) {
    val reduced = reducedMotion()
    val position by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.75f, stiffness = 520f),
        label = "segments-indicator",
    )
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(trackColor)
                .padding(4.dp)
                .drawBehind {
                    val segmentWidth = size.width / labels.size
                    drawRoundRect(
                        color = indicatorColor,
                        topLeft = Offset(position * segmentWidth, 0f),
                        size = Size(segmentWidth, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                },
    ) {
        labels.forEachIndexed { index, label ->
            val contentColor by animateColorAsState(
                targetValue = if (index == selectedIndex) selectedContentColor else idleContentColor,
                animationSpec = if (reduced) snap() else spring(stiffness = 500f),
                label = "segment-content",
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) { onSelect(index) }
                        .padding(vertical = verticalPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
