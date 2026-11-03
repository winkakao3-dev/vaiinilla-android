package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** La hoja de Cartera y Cuenta (Doop E03, E13): sube con un resorte sobre un velo, con su asa arriba. */
@Composable
internal fun WalletBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    BackHandler(enabled = visible && dismissible, onBack = onDismiss)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(if (reduced) snap() else spring(stiffness = 400f)),
        exit = fadeOut(if (reduced) snap() else spring(stiffness = 500f)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (dismissible) onDismiss() },
                    ),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter =
            if (reduced) {
                fadeIn(snap())
            } else {
                slideInVertically(spring(dampingRatio = 0.86f, stiffness = 380f)) { it / 2 } + fadeIn()
            },
        exit = if (reduced) fadeOut(snap()) else slideOutVertically(spring(stiffness = 500f)) { it / 2 } + fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .background(colors.paper)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ).navigationBarsPadding()
                        .verticalScroll(rememberScrollState()),
            ) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 10.dp)
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(walletTile(colors)),
                )
                content()
            }
        }
    }
}