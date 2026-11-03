package com.vaiinilla.app.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.screens.OperationalColors
import java.math.BigDecimal

/** El paso 3 de agregar a un hijo: qué sigue según si la escuela ya lo dejó ver o todavía revisa al tutor. */
internal fun addedChildTitle(child: Dependent): String =
    if (child.isAwaitingSchool) "Le avisamos a la escuela" else "${child.firstName} ya está contigo"

/** Ya aprobado: su primera recarga, que se puede dejar para después. */
@Composable
internal fun ColumnScope.AddedChildReady(
    child: Dependent,
    colors: OperationalColors,
) {
    AddedChildHeader(child = child, colors = colors, awaiting = false)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "¿Le recargas para que coma?",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            "Tiene ${compactMoney(child.balance ?: BigDecimal.ZERO)}. También le pueden recargar en Caja.",
            color = colors.textSecondary,
            fontSize = 14.sp,
        )
    }
    Text(
        child.principalFirstName?.let { "$it pone el límite, los bloqueos y las alergias. Tú ves, recargas y apartas." }
            ?: "Después, en su ficha, le pones un límite al día, bloqueas productos y marcas sus alergias.",
        color = colors.textMuted,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

/** La escuela revisa a los tutores: hasta que te apruebe no hay saldo ni ficha. */
@Composable
internal fun ColumnScope.AddedChildAwaiting(
    child: Dependent,
    colors: OperationalColors,
) {
    AddedChildHeader(child = child, colors = colors, awaiting = true)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        AwaitingStep(
            mark = StepMark.DONE,
            title = "Lo agregaste",
            detail = listOfNotNull(child.name, child.group?.takeIf { it.isNotBlank() }).joinToString(" · "),
            colors = colors,
        )
        AwaitingStep(
            mark = StepMark.NOW,
            title = "La escuela te aprueba",
            detail = "Hasta entonces no ves su saldo ni abres su ficha.",
            colors = colors,
        )
        AwaitingStep(
            mark = StepMark.NEXT,
            title = "Le recargas y le pones sus controles",
            detail = "Con tarjeta desde aquí, o en Caja.",
            colors = colors,
            last = true,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ASÍ LO VES EN MIS HIJOS", color = colors.textMuted, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).alpha(0.6f).clip(CircleShape).background(colors.cardInner),
                contentAlignment = Alignment.Center,
            ) {
                Text(child.initials, color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    child.firstName,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Esperando a que la escuela te apruebe", color = colors.textSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
        }
    }
    Text(
        "Cada vez que abras la app revisamos si ya te aprobaron.",
        color = colors.textMuted,
        fontSize = 13.sp,
    )
}

/** Los botones del paso 3: recargar o ir a su ficha si ya está aprobado; si no, solo cerrar. */
@Composable
internal fun ColumnScope.AddedChildActions(
    child: Dependent,
    colors: OperationalColors,
    onReload: () -> Unit,
    onOpenChild: () -> Unit,
    onDone: () -> Unit,
) {
    if (child.isAwaitingSchool) {
        StepButton("Listo", primary = true, colors = colors, tag = "added-child-done", onClick = onDone)
    } else {
        StepButton(
            "Recargar a ${child.firstName}",
            primary = true,
            colors = colors,
            tag = "added-child-reload",
            onClick = onReload,
        )
        StepButton("Ahora no, ver su ficha", primary = false, colors = colors, tag = "added-child-open", onClick = onOpenChild)
    }
}

@Composable
private fun AddedChildHeader(
    child: Dependent,
    colors: OperationalColors,
    awaiting: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Box {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(colors.cardInner),
                contentAlignment = Alignment.Center,
            ) {
                Text(child.initials, color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.background)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(if (awaiting) AWAITING_YOLK else colors.accentLime),
                contentAlignment = Alignment.Center,
            ) {
                if (!awaiting) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(11.dp))
                }
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                if (awaiting) "${child.firstName} quedó en espera" else child.name,
                color = colors.textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (awaiting) {
                    "La escuela revisa a los tutores antes de dejarte ver a ${child.firstName}."
                } else {
                    child.group?.takeIf { it.isNotBlank() } ?: "Ya aparece en Mis hijos"
                },
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

private enum class StepMark { DONE, NOW, NEXT }

@Composable
private fun AwaitingStep(
    mark: StepMark,
    title: String,
    detail: String,
    colors: OperationalColors,
    last: Boolean = false,
) {
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
                                StepMark.DONE -> colors.accentLime.copy(alpha = 0.18f)
                                StepMark.NOW -> AWAITING_YOLK.copy(alpha = 0.2f)
                                StepMark.NEXT -> colors.cardInner
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                when (mark) {
                    StepMark.DONE ->
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accentLime, modifier = Modifier.size(13.dp))
                    StepMark.NOW -> Box(Modifier.size(8.dp).clip(CircleShape).background(AWAITING_YOLK))
                    StepMark.NEXT -> Text("3", color = colors.textMuted, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    title,
                    color = if (mark == StepMark.NEXT) colors.textSecondary else colors.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
                Text(detail, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardBorder))
    }
}

@Composable
private fun StepButton(
    label: String,
    primary: Boolean,
    colors: OperationalColors,
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(tag)
                .physicalPress(onClick = onClick)
                .height(if (primary) 54.dp else 50.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(if (primary) colors.accentLime else colors.buttonSecondary)
                .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) colors.accentInk else colors.buttonSecondaryInk,
            fontWeight = FontWeight.Black,
            fontSize = if (primary) 16.sp else 15.sp,
            maxLines = 1,
        )
    }
}

private val AWAITING_YOLK = Color(0xFFFFD15B)