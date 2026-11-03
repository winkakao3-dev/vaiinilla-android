package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.ui.components.ClientPillButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.math.BigDecimal

/** El monto que sale elegido en la primera recarga, como en el dibujo. */
private const val FIRST_RELOAD_AMOUNT = 200

/** Paso 3 de agregar a un hijo: su primera recarga si la escuela ya lo deja ver, o esperar a que apruebe al tutor. */
@Composable
internal fun AddedChildStep(
    child: Dependent,
    schoolName: String?,
    onOpenChild: (Dependent, Int?) -> Unit,
    onDone: () -> Unit,
) {
    if (child.isAwaitingSchool) {
        AddedChildAwaiting(child, schoolName, onDone)
    } else {
        AddedChildReady(child, schoolName, onOpenChild)
    }
}

@Composable
private fun AddedChildReady(
    child: Dependent,
    schoolName: String?,
    onOpenChild: (Dependent, Int?) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    var amount by rememberSaveable { mutableIntStateOf(FIRST_RELOAD_AMOUNT) }
    AddChildStepPage(
        bottom = {
            ClientPillButton(
                "Recargar ${compactMoney(amount.toBigDecimal())} a ${child.firstName}",
                onClick = { onOpenChild(child, amount) },
                modifier = Modifier.testTag("added-child-reload"),
            )
            ClientPillButton(
                "Ahora no, ver su ficha",
                onClick = { onOpenChild(child, null) },
                primary = false,
                height = 52.dp,
                modifier = Modifier.testTag("added-child-open"),
            )
        },
    ) {
        Row(
            modifier = Modifier.arrive(0).padding(horizontal = 4.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChildBadgeAvatar(child, awaiting = false, pops = true)
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    "${child.firstName} ya está contigo",
                    color = colors.ink,
                    fontSize = 24.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(schoolName, child.group?.takeIf { it.isNotBlank() })
                        .joinToString(" · ")
                        .ifEmpty { "Ya aparece en Mis hijos" },
                    color = colors.muted,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Column(modifier = Modifier.arrive(1).padding(start = 4.dp, end = 4.dp, top = 36.dp)) {
            Text("¿Le recargas para que coma?", color = colors.ink2, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "Tiene ${compactMoney(child.balance ?: BigDecimal.ZERO)}. También le pueden recargar en Caja.",
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Column(
            modifier = Modifier.arrive(2).fillMaxWidth().padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = amount,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        val up = targetState > initialState
                        (
                            slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { if (up) it / 2 else -it / 2 } +
                                fadeIn(spring(stiffness = 400f))
                        ) togetherWith
                            (slideOutVertically(spring(stiffness = 600f)) { if (up) -it / 2 else it / 2 } + fadeOut(spring(stiffness = 700f)))
                    }
                },
                label = "first-reload-amount",
            ) { shown ->
                Text(
                    compactMoney(shown.toBigDecimal()),
                    color = colors.ink,
                    fontSize = 72.sp,
                    lineHeight = 72.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-3.5).sp,
                    maxLines = 1,
                )
            }
            Text(
                "A ${child.firstName} le llegan ${compactMoney(amount.toBigDecimal())}. Antes de pagar te enseñamos el total con la comisión.",
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp).padding(top = 10.dp),
            )
        }
        Row(
            modifier = Modifier.arrive(3).fillMaxWidth().padding(top = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChildReloadModel.QUICK_AMOUNTS.forEach { quick ->
                val selected = quick == amount
                val fill by animateColorAsState(if (selected) colors.ink else colors.paper2, spring(stiffness = 500f), label = "amount-$quick")
                val ink by animateColorAsState(if (selected) colors.paper else colors.ink2, spring(stiffness = 500f), label = "amount-ink-$quick")
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("added-child-amount-$quick")
                            .physicalPress(scale = PhysicalPressScale.Small) {
                                haptics.selection()
                                amount = quick
                            }.clip(RoundedCornerShape(percent = 50))
                            .background(fill)
                            .semantics {
                                role = Role.Button
                                this.selected = selected
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(compactMoney(quick.toBigDecimal()), color = ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Row(
            modifier =
                Modifier
                    .arrive(4)
                    .fillMaxWidth()
                    .padding(top = 26.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.paper2)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.muted, modifier = Modifier.padding(top = 1.dp).size(20.dp))
            Text(
                CHILD_RELOAD_CARD_NOTE,
                color = colors.ink2,
                fontSize = 13.sp,
                lineHeight = 18.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            child.principalFirstName?.let { "$it pone el límite, los bloqueos y las alergias. Tú ves, recargas y apartas." }
                ?: "Después, en su ficha, le pones un límite al día, bloqueas productos y marcas sus alergias.",
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.arrive(5).padding(start = 4.dp, end = 4.dp, top = 20.dp),
        )
    }
}

/** La escuela revisa a los tutores: hasta que te apruebe no hay saldo ni ficha. */
@Composable
private fun AddedChildAwaiting(
    child: Dependent,
    schoolName: String?,
    onDone: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    AddChildStepPage(
        bottom = {
            ClientPillButton("Listo", onClick = onDone, modifier = Modifier.testTag("added-child-done"))
        },
    ) {
        Column(modifier = Modifier.arrive(0).padding(horizontal = 4.dp)) {
            ChildBadgeAvatar(child, awaiting = true, pops = false)
            StepTitle("Le avisamos a la escuela", Modifier.padding(top = 16.dp), fontSize = 30)
            Text(
                "${schoolName ?: "La escuela"} revisa a los tutores antes de dejarte ver a ${child.firstName}.",
                color = colors.ink2,
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp),
            )
        }
        Column(
            modifier =
                Modifier
                    .arrive(1)
                    .padding(top = 26.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.paper2)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            AwaitingStep(
                mark = StepMark.DONE,
                title = "Lo agregaste",
                detail =
                    listOfNotNull(
                        child.name,
                        child.group?.takeIf { it.isNotBlank() },
                        child.studentId?.takeIf { it.isNotBlank() }?.let { "matrícula $it" },
                    ).joinToString(" · "),
            )
            AwaitingStep(
                mark = StepMark.NOW,
                title = "La escuela te aprueba",
                detail = "Hasta entonces no ves su saldo ni abres su ficha.",
            )
            AwaitingStep(
                mark = StepMark.NEXT,
                title = "Le recargas y le pones sus controles",
                detail = "Con tarjeta desde aquí, o en Caja.",
                last = true,
            )
        }
        Column(modifier = Modifier.arrive(2).padding(top = 24.dp)) {
            Text(
                "ASÍ LO VES EN MIS HIJOS",
                color = colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            val dash = colors.ink.copy(alpha = 0.18f)
            Row(
                modifier =
                    Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .drawBehind {
                            drawRoundRect(
                                color = dash,
                                cornerRadius = CornerRadius(18.dp.toPx()),
                                style =
                                    Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                                    ),
                            )
                        }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(child.initials, colors.paper2, colors.ink, colors.line, Modifier.size(40.dp).alpha(0.6f))
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        child.firstName,
                        color = colors.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Esperando a que la escuela te apruebe",
                        color = colors.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        Text(
            "Cada vez que abras la app revisamos si ya te aprobaron.",
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.arrive(3).padding(start = 4.dp, end = 4.dp, top = 22.dp),
        )
    }
}

/** Sus iniciales como en Mis hijos, con la palomita lima si ya está o el punto amarillo si la escuela revisa. */
@Composable
private fun ChildBadgeAvatar(
    child: Dependent,
    awaiting: Boolean,
    pops: Boolean,
    size: Dp = 56.dp,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    val scale = remember { Animatable(if (pops && !reduced) 0.6f else 1f) }
    LaunchedEffect(Unit) { if (pops && !reduced) scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
    Box(modifier = Modifier.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }) {
        Box(
            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(colors.paper2)
                    .border(1.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(child.initials, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 21.sp)
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colors.paper)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(if (awaiting) colors.yolk else colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            if (!awaiting) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(11.dp))
            }
        }
    }
}

private enum class StepMark { DONE, NOW, NEXT }

@Composable
private fun AwaitingStep(
    mark: StepMark,
    title: String,
    detail: String,
    last: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when (mark) {
                                StepMark.DONE -> colors.accent.copy(alpha = 0.16f)
                                StepMark.NOW -> colors.yolk.copy(alpha = 0.16f)
                                StepMark.NEXT -> colors.line
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                when (mark) {
                    StepMark.DONE ->
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(13.dp))
                    StepMark.NOW -> {
                        // Lo que está pasando ahora late despacio; con menos movimiento se queda fijo.
                        val pulse =
                            if (reduced) {
                                1f
                            } else {
                                rememberInfiniteTransition(label = "awaiting-now")
                                    .animateFloat(
                                        initialValue = 1f,
                                        targetValue = 0.35f,
                                        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                        label = "awaiting-pulse",
                                    ).value
                            }
                        Box(Modifier.size(8.dp).alpha(pulse).clip(CircleShape).background(colors.yolk))
                    }
                    StepMark.NEXT -> Text("3", color = colors.muted, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    title,
                    color = if (mark == StepMark.NEXT) colors.ink2 else colors.ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
                Text(
                    detail,
                    color = colors.muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
    }
}