package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.MySchool
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import com.vaiinilla.app.ui.theme.seaGlassInk
import kotlinx.coroutines.delay

/** "Tus escuelas" arriba de Dónde pedir: las escuelas donde ya es cliente, sin la actual. */
@Composable
internal fun DiscoverySchoolsRow(
    schools: List<MySchool>,
    openingSchoolId: String?,
    onOpen: (MySchool) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth().testTag("guest-my-schools")) {
        Text("Tus escuelas", color = colors.muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    // Aire para la sombra de las fichas.
                    .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            schools.forEachIndexed { index, school ->
                SchoolChip(
                    school = school,
                    index = index,
                    opening = openingSchoolId == school.id,
                    enabled = openingSchoolId == null || openingSchoolId == school.id,
                    onOpen = onOpen,
                )
            }
        }
    }
}

@Composable
private fun SchoolChip(
    school: MySchool,
    index: Int,
    opening: Boolean,
    enabled: Boolean,
    onOpen: (MySchool) -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val ink = seaGlassInk(SeaGlassTone.vainilla)
    // Las escuelas entran una tras otra desde la izquierda.
    val arrival = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        delay(index * 50L)
        arrival.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 300f))
    }
    SeaGlassButton(
        tone = SeaGlassTone.vainilla,
        enabled = enabled,
        onClick = {
            haptics.selection()
            onOpen(school)
        },
        modifier =
            Modifier
                .graphicsLayer {
                    alpha = arrival.value
                    translationX = (1f - arrival.value) * -14.dp.toPx()
                }.height(42.dp)
                .testTag("guest-school-${school.slug}")
                .semantics { contentDescription = "Volver a ${school.name}" },
    ) {
        Spacer(Modifier.width(14.dp))
        Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = opening,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        (fadeIn() + scaleIn(spring(stiffness = 500f), initialScale = 0.6f)) togetherWith
                            (fadeOut() + scaleOut(targetScale = 0.6f))
                    }
                },
                label = "school-chip",
            ) { busy ->
                if (busy) {
                    CircularProgressIndicator(color = ink, strokeWidth = 2.dp, modifier = Modifier.size(15.dp))
                } else {
                    Icon(Icons.Filled.School, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            school.name,
            color = ink,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 14.dp),
        )
    }
}
