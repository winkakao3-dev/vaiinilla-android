package com.vaiinilla.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Aviso flotante al pie: sube con un resorte y se va desvaneciendo con su
 * mensaje (conserva el último texto, no se vacía mientras sale).
 */
@Composable
fun BoxScope.ToastPill(
    message: String?,
    containerColor: Color,
    contentColor: Color,
    badgeColor: Color,
    badgeContentColor: Color,
) {
    var lastMessage by remember { mutableStateOf("") }
    message?.let { lastMessage = it }
    val reduced = reducedMotion()
    AnimatedVisibility(
        visible = message != null,
        enter =
            if (reduced) {
                fadeIn(snap())
            } else {
                fadeIn(spring(stiffness = 400f)) +
                    slideInVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) { it / 2 }
            },
        exit =
            if (reduced) {
                fadeOut(snap())
            } else {
                fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 2 }
            },
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = containerColor,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(badgeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = badgeContentColor,
                        modifier = Modifier.size(13.dp),
                    )
                }
                Text(lastMessage, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
