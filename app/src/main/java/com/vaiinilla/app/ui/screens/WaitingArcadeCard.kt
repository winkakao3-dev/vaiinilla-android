package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.arcade.ArcadeBoardRow
import com.vaiinilla.app.domain.arcade.ArcadeKind
import com.vaiinilla.app.domain.arcade.ArcadeSession
import com.vaiinilla.app.ui.arcade.ArcadeHaptic
import com.vaiinilla.app.ui.arcade.ArcadeStage
import com.vaiinilla.app.ui.arcade.PixelCanvas
import com.vaiinilla.app.ui.arcade.PrefsArcadeScoreStore
import com.vaiinilla.app.ui.arcade.PrefsSkateProgressStore
import com.vaiinilla.app.ui.components.VaiinillaHaptics
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlin.math.min

private val ArcadePanel = Color(0xFF1C1D1B)
private val ArcadeWell = Color(0xFF0A0A0C)
private val ArcadeControl = Color(0xFF232427)
private val ArcadePill = Color(0xFF4A4A4E)
private val ArcadeInk = Color(0xFFF5F2E8)
private val ArcadeDim = Color(0xFF8E8E93)
private val ArcadeLine = Color.White.copy(alpha = 0.09f)

/**
 * Mini games shown while an order is in progress: five pixel-art games drawn by the arcade engine, with a selector
 * and a leaderboard around the stage.
 */
@Composable
internal fun WaitingArcadeCard(
    haptics: VaiinillaHaptics = rememberVaiinillaHaptics(),
    stageOverride: ArcadeStage? = null,
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val currentHaptics = rememberUpdatedState(haptics)
    val stage =
        stageOverride ?: remember(context) {
            ArcadeStage(
                ArcadeSession(PrefsArcadeScoreStore(context), PrefsSkateProgressStore(context)),
                haptic = {
                    when (it) {
                        ArcadeHaptic.SOFT -> currentHaptics.value.click()
                        ArcadeHaptic.HIT -> currentHaptics.value.impact()
                    }
                },
            )
        }
    val session = stage.session
    stage.version // redraw the card whenever the stage repaints or the player acts

    // One callback per display frame while something moves; nothing at all once the stage has frozen.
    LaunchedEffect(stage) {
        stage.resetClock()
        while (true) {
            if (stage.isFrozen) {
                stage.awaitWake()
                stage.resetClock()
            }
            withFrameNanos { now -> stage.tick(now / 1_000_000_000.0) }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ArcadePanel,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ArcadeControl)
                            .padding(3.dp),
                ) {
                    ArcadeKind.entries.forEach { k ->
                        val selected = k == session.kind
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (selected) ArcadePill else Color.Transparent)
                                    .pointerInput(k) {
                                        detectTapGestures {
                                            haptics.click()
                                            session.select(k)
                                            stage.wake()
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                k.label,
                                color = if (selected) ArcadeInk else ArcadeDim,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ArcadeControl)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    haptics.click()
                                    session.boardOpen = !session.boardOpen
                                    stage.wake()
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.EmojiEvents,
                        contentDescription = "Leaderboard",
                        tint = if (session.boardOpen) colors.accent else ArcadeDim,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .aspectRatio(PixelCanvas.WIDTH.toFloat() / PixelCanvas.HEIGHT)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ArcadeWell),
            ) {
                // A touch starts or plays; dragging also steers Galaxia and a downward swipe drops Skate.
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .testTag("waiting-arcade")
                            .pointerInput(stage) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (session.boardOpen) return@awaitEachGesture
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    haptics.click()
                                    session.aim(down.position.x / w * PixelCanvas.WIDTH.toDouble())
                                    session.primary()
                                    stage.wake()
                                    val fallDistance = 28.dp.toPx()
                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break
                                        if (change.pressed) {
                                            session.aim(change.position.x / w * PixelCanvas.WIDTH.toDouble())
                                            if (change.position.y - down.position.y > fallDistance) session.fall(true)
                                        }
                                    } while (event.changes.any { it.pressed })
                                    session.release()
                                    session.fall(false)
                                }
                            },
                ) {
                    stage.version
                    val scale = size.width / PixelCanvas.WIDTH
                    drawImage(
                        image = stage.image,
                        dstOffset = IntOffset((stage.shakeX * scale).toInt(), (stage.shakeY * scale).toInt()),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        filterQuality = FilterQuality.None,
                    )
                }
                if (session.boardOpen) {
                    ArcadeBoardOverlay(
                        kind = session.kind,
                        rows = session.leaderboard(),
                        accent = colors.accent,
                        onClose = {
                            haptics.click()
                            session.boardOpen = false
                            stage.wake()
                        },
                    )
                }
            }

            Text(
                "Mini juegos mientras va tu pedido",
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp, start = 4.dp, end = 4.dp),
                color = ArcadeDim,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ArcadeBoardOverlay(
    kind: ArcadeKind,
    rows: List<ArcadeBoardRow>,
    accent: Color,
    onClose: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ArcadeWell)
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text("Leaderboard", color = ArcadeInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "${kind.label} · top ${min(7, rows.size)}",
            color = ArcadeDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
        )
        rows.forEachIndexed { i, row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${i + 1}",
                    modifier = Modifier.width(18.dp),
                    color = ArcadeDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    row.name,
                    modifier = Modifier.weight(1f),
                    color = if (row.me) accent else ArcadeInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${row.points}",
                    color = if (row.me) accent else ArcadeInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            if (i < rows.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(ArcadeLine))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Seguir jugando",
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .pointerInput(Unit) { detectTapGestures { onClose() } }
                    .padding(4.dp),
            color = ArcadeDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(name = "Arcade", showBackground = true, widthDp = 360)
@Composable
private fun WaitingArcadeCardPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        Box(modifier = Modifier.padding(16.dp)) {
            WaitingArcadeCard()
        }
    }
}
