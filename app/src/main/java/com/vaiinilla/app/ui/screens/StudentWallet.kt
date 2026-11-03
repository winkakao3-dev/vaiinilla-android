package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.PersonNames
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** La cartera del alumno con tutor (Doop E14): el anillo de lo que le queda hoy y su saldo con quién lo cuida. */
@Composable
internal fun StudentAllowance(
    controls: MyGuardianControls,
    balance: String?,
) {
    val colors = LocalVaiinillaColors.current
    val limit = controls.dailyLimit
    val left = controls.availableToday
    val fraction =
        if (limit != null && left != null && limit.signum() > 0) {
            (left.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
    val sweep by animateFloatAsState(
        targetValue = fraction ?: 0f,
        animationSpec = if (reducedMotion()) snap() else spring(dampingRatio = 0.9f, stiffness = 90f),
        label = "allowance_ring",
    )
    val track = colors.paper2
    val fill = colors.accent
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier.size(172.dp).semantics(mergeDescendants = true) {
                    contentDescription =
                        if (left != null) {
                            "Hoy te quedan ${compactMoney(left)}" + (limit?.let { " de ${compactMoney(it)}" } ?: "")
                        } else {
                            "Tu saldo"
                        }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(172.dp)) {
                val stroke = 13.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                if (fraction != null && sweep > 0f) {
                    drawArc(fill, -90f, 360f * sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (left != null) "HOY TE QUEDAN" else "TU SALDO",
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.2.sp,
                )
                Text(
                    left?.let(::compactMoney) ?: balance?.let(::moneyLabel) ?: "—",
                    color = colors.ink,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    maxLines = 1,
                )
                if (left != null && limit != null) {
                    Text("de ${compactMoney(limit)}", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(
            studentBalanceLine(balance, controls.tutorNames, strong = colors.ink),
            color = colors.ink2,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
        )
    }
}

/** «Tu saldo: $42 · te cuida Kao» (Doop E14). */
internal fun studentBalanceLine(
    balance: String?,
    tutorNames: List<String>,
    strong: Color = Color.Unspecified,
): AnnotatedString {
    val bold = SpanStyle(color = strong, fontWeight = FontWeight.Black)
    return buildAnnotatedString {
        if (balance != null) {
            append("Tu saldo: ")
            withStyle(bold) { append(moneyLabel(balance)) }
        }
        if (tutorNames.isNotEmpty()) {
            val verb = if (tutorNames.size == 1) "cuida" else "cuidan"
            append(if (balance != null) " · te $verb " else "Te $verb ")
            withStyle(bold) { append(PersonNames.list(tutorNames)) }
        }
    }
}

/** Una fila de «Con candado»: qué no puede pedir y por qué. */
internal data class LockedLine(
    val title: String,
    val reason: String,
    val allergy: Boolean,
)

/** Lo que el alumno no puede pedir, en palabras: sus alergias, lo que bloqueó el tutor y lo que la escuela no revisó. */
internal fun lockedLines(controls: MyGuardianControls): List<LockedLine> =
    buildList {
        val allergic = controls.allergenProductIds.size
        if (controls.allergens.isNotEmpty() || allergic > 0) {
            val labels = controls.allergens.map { it.label }
            val title =
                if (labels.isNotEmpty()) {
                    PersonNames.list(listOf(labels.first()) + labels.drop(1).map { it.lowercase() })
                } else if (allergic == 1) {
                    "1 producto"
                } else {
                    "$allergic productos"
                }
            add(LockedLine(title, "Por tu alergia", allergy = true))
        }
        val products = controls.blockedProductIds.size
        val categories = controls.blockedCategoryIds.size
        if (products + categories > 0) {
            val parts =
                listOfNotNull(
                    products.takeIf { it > 0 }?.let { if (it == 1) "1 producto" else "$it productos" },
                    categories.takeIf { it > 0 }?.let { if (it == 1) "1 categoría" else "$it categorías" },
                )
            val who =
                when (controls.tutorNames.size) {
                    0 -> "Lo decidió tu tutor"
                    1 -> "Lo decidió ${PersonNames.first(controls.tutorNames[0])}"
                    else -> "Lo decidieron tus tutores"
                }
            add(LockedLine(parts.joinToString(" y ").replaceFirstChar { it.uppercase() }, who, allergy = false))
        }
        val unreviewed = controls.unreviewedProductIds.size
        if (unreviewed > 0) {
            add(
                LockedLine(
                    if (unreviewed == 1) "1 producto sin revisar" else "$unreviewed productos sin revisar",
                    "La escuela aún no dice qué alérgenos tiene",
                    allergy = true,
                ),
            )
        }
    }

@Composable
internal fun StudentLockedCard(controls: MyGuardianControls) {
    val colors = LocalVaiinillaColors.current
    val lines = lockedLines(controls)
    if (lines.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 26.dp)) {
        WalletSectionHeader(title = "Con candado")
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.paper2),
        ) {
            lines.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider(color = colors.line, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (line.allergy) colors.coral.copy(alpha = 0.16f) else walletTile(colors)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (line.allergy) colors.coral else colors.ink2,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line.title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            line.reason,
                            color = colors.muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

/** «Mostrar mi código en Caja» y, abajo, lo del alumno: dar su código a otro tutor y cómo funciona. */
@Composable
internal fun StudentActions(
    codeReady: Boolean,
    onCode: () -> Unit,
    onLinkCode: () -> Unit,
    onHelp: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp)) {
        WalletButton(
            label = "Mostrar mi código en Caja",
            primary = true,
            enabled = codeReady,
            onClick = onCode,
            height = 54,
            modifier = Modifier.fillMaxWidth().testTag("wallet-code"),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            listOf(
                Triple("Otro tutor", onLinkCode, "wallet-school-link-code"),
                Triple("Cómo funciona", onHelp, "wallet-school-help"),
            ).forEachIndexed { index, (label, action, tag) ->
                if (index > 0) Text("·", color = colors.muted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp))
                Text(
                    label,
                    color = colors.ink2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag(tag).physicalPress(scale = PhysicalPressScale.Small, onClick = action),
                )
            }
        }
    }
}