package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.ClientPillButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.minTouchTarget
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

private val TutorTileFill = Color(0xFFC9A8FF)
private val TutorTileInk = Color(0xFF2A1748)

/**
 * Padres e hijos la primera vez (Doop E05): quien todavía no tiene hijos ve qué gana, una sola acción y, abajo, la
 * entrada del alumno. La acción es agregar a su hijo, o antes el código de la escuela si aún no lo escribió.
 */
@Composable
internal fun ParentFirstTimeContent(
    parentAccess: Boolean?,
    onAdd: () -> Unit,
    onUnlock: () -> Unit,
    onStudent: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .testTag("parent-first-time")
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TwoTiles(reduced)
            Text(
                "Cuida lo que come tu hijo en la escuela",
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                lineHeight = 34.sp,
                letterSpacing = (-1.3).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp, start = 8.dp, end = 8.dp).arrive(1),
            )
            Text(
                "Recárgale, aparta su comida y pon sus límites. Él compra con su credencial o su PIN.",
                color = colors.muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp).arrive(1),
            )
            Column(
                modifier =
                    Modifier
                        .padding(top = 28.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.paper2)
                        .arrive(2),
            ) {
                BenefitRow(
                    Icons.Outlined.AccountBalanceWallet,
                    colors.accent,
                    "Saldo aparte para cada hijo",
                    "Tu saldo y el suyo no se mezclan",
                )
                CardDivider()
                BenefitRow(
                    Icons.Outlined.CalendarMonth,
                    colors.yolk,
                    "Aparta la comida de la semana",
                    "Elige el menú del día y la hora de recoger",
                )
                CardDivider()
                BenefitRow(
                    Icons.Outlined.Block,
                    colors.errorText,
                    "Límite diario y alergias",
                    "La caja no le vende lo que bloqueaste",
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(modifier = Modifier.padding(bottom = 24.dp).arrive(3), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = parentAccess,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        fadeIn(spring(stiffness = 300f)) togetherWith fadeOut(spring(stiffness = 600f))
                    }
                },
                label = "first-time-action",
            ) { access ->
                when (access) {
                    true -> ClientPillButton("Agregar a mi hijo", onAdd, Modifier.testTag("school-options-add"))
                    false -> ClientPillButton("Tengo el código de la escuela", onUnlock, Modifier.testTag("school-options-unlock"))
                    null -> SkeletonBlock(modifier = Modifier.fillMaxWidth().height(56.dp), corner = 28.dp)
                }
            }
            Text(
                buildAnnotatedString {
                    append("¿Eres el alumno? ")
                    withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.Black)) { append("Usa el código de tu tutor") }
                },
                color = colors.muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .testTag("first-time-student")
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onStudent)
                        .semantics { role = Role.Button }
                        .minTouchTarget(),
            )
        }
    }
}

/** El tutor y su hijo, dos fichas que llegan girando con un resorte. Sin hijo aún, van con su ícono, no con iniciales. */
@Composable
private fun TwoTiles(reduced: Boolean) {
    val colors = LocalVaiinillaColors.current
    val pop = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) { if (!reduced) pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
    Box(modifier = Modifier.size(width = 140.dp, height = 110.dp)) {
        Tile(
            Icons.Outlined.Person,
            TutorTileFill,
            TutorTileInk,
            84,
            Modifier
                .offset(y = 10.dp)
                .graphicsLayer {
                    scaleX = 0.8f + 0.2f * pop.value
                    scaleY = scaleX
                    alpha = pop.value.coerceIn(0f, 1f)
                    rotationZ = -8f * pop.value
                },
        )
        Tile(
            Icons.Outlined.ChildCare,
            ChildAvatarFill,
            ChildAvatarInk,
            70,
            Modifier
                .align(Alignment.TopEnd)
                .offset(y = 30.dp)
                .graphicsLayer {
                    scaleX = 0.8f + 0.2f * pop.value
                    scaleY = scaleX
                    alpha = pop.value.coerceIn(0f, 1f)
                    rotationZ = 9f * pop.value
                }.border(4.dp, colors.paper, RoundedCornerShape(24.dp)),
        )
    }
}

@Composable
private fun Tile(
    icon: ImageVector,
    fill: Color,
    ink: Color,
    size: Int,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.size(size.dp).clip(RoundedCornerShape((size / 3).dp)).background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size((size * 0.42f).dp))
    }
}

@Composable
private fun BenefitRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    detail: String,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(
                detail,
                color = colors.muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}