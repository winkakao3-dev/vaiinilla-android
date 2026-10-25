package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.guardianship.GuardianLinkCode
import com.vaiinilla.app.domain.guardianship.PrivacyNotice
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.screens.OperationalColors
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

internal fun newKey(): String = UUID.randomUUID().toString()

/**
 * La llave de idempotencia para el siguiente intento. Tras un error del servidor (4xx) la acción no se aplicó y
 * se estrena otra; tras un corte de red o un 5xx puede haberse aplicado, y el reintento debe llevar la misma
 * llave para que el servidor devuelva lo que ya hizo en vez de un 409 engañoso.
 */
internal fun keyAfterFailure(
    error: Throwable,
    current: String,
): String {
    val status = (error as? ApiClientException)?.httpStatus
    return if (status != null && status in 400..499 && status != REQUEST_TIMEOUT) newKey() else current
}

private const val REQUEST_TIMEOUT = 408

/** Las hojas del Modo Padre: título, subtítulo, cerrar, el contenido con scroll y el botón fijo abajo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParentSheet(
    title: String,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    busy: Boolean = false,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // El estado de la hoja se recuerda con confirmValueChange como llave: una lambda nueva al cambiar busy
    // lo rehacía oculto y dejaba la ventana invisible encima, comiéndose los toques (F07). Una sola
    // lambda que lee el valor vigente.
    val currentBusy by rememberUpdatedState(busy)
    val confirmChange = remember { { target: SheetValue -> target != SheetValue.Hidden || !currentBusy } }
    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = confirmChange)
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).imePadding()) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 22.sp, color = colors.textPrimary)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .physicalPress(enabled = !busy, scale = PhysicalPressScale.Small, onClick = onDismiss)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, CircleShape)
                            .semantics { contentDescription = "Cerrar" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
            if (footer != null) {
                Column(
                    modifier = Modifier.padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = footer,
                )
            } else {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/**
 * El botón principal de una hoja: un solo objeto que pasa de acción a progreso y a hecho, como el
 * `MorphingActionButton` de iOS. Va en lima porque es la acción principal de la hoja.
 */
internal sealed interface MorphState {
    data class Idle(
        val label: String,
    ) : MorphState

    data class Working(
        val label: String,
    ) : MorphState

    data class Done(
        val label: String,
    ) : MorphState
}

internal val MorphState.isBusy: Boolean get() = this !is MorphState.Idle

@Composable
internal fun MorphingButton(
    state: MorphState,
    enabled: Boolean,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    onClick: () -> Unit,
) {
    val active = enabled && state is MorphState.Idle
    val container =
        animateSelectionColor(
            if (active ||
                state !is MorphState.Idle
            ) {
                colors.accentLime
            } else {
                colors.pillBackground
            },
            "morph-bg",
        )
    val ink =
        animateSelectionColor(
            if (active ||
                state !is MorphState.Idle
            ) {
                colors.accentInk
            } else {
                colors.textSecondary
            },
            "morph-ink",
        )
    val reduced = reducedMotion()
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .physicalPress(enabled = active, onClick = onClick)
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(container)
                .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                if (reduced) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    fadeIn(spring(stiffness = 300f)) togetherWith
                        fadeOut(spring(stiffness = 600f))
                }
            },
            label = "morph",
        ) { current ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                when (current) {
                    is MorphState.Working ->
                        CircularProgressIndicator(
                            color = ink,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    is MorphState.Done ->
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = ink,
                            modifier = Modifier.size(20.dp),
                        )
                    is MorphState.Idle -> Unit
                }
                val label =
                    when (current) {
                        is MorphState.Idle -> current.label
                        is MorphState.Working -> current.label
                        is MorphState.Done -> current.label
                    }
                Text(
                    label,
                    color = ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = if (current is MorphState.Idle) 0.dp else 8.dp, end = 4.dp),
                )
            }
        }
    }
}

/** Un mensaje de error dentro de una hoja, con reintentar o cerrar. */
@Composable
internal fun ParentErrorCard(
    message: String,
    colors: OperationalColors,
    title: String? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardInner)
                .border(1.dp, ErrorCoral.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(14.dp),
    ) {
        if (title != null) Text(title, color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Text(message, color = colors.textPrimary, fontSize = 14.sp, lineHeight = 20.sp)
        val action = onRetry ?: onDismiss
        if (action != null) {
            Text(
                if (onRetry != null) "Reintentar" else "Entendido",
                color = colors.textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier =
                    Modifier
                        .padding(
                            top = 8.dp,
                        ).physicalPress(scale = PhysicalPressScale.Small, onClick = action),
            )
        }
    }
}

internal val ErrorCoral = Color(0xFFE5645A)

/** Un chip que se prende: lo elegido va en tinta, el lima queda para la acción principal. */
@Composable
internal fun ToggleChip(
    label: String,
    selected: Boolean,
    colors: OperationalColors,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val container = animateSelectionColor(if (selected) colors.textPrimary else colors.cardInner, "toggle-chip-bg")
    val ink = animateSelectionColor(if (selected) colors.background else colors.textPrimary, "toggle-chip-ink")
    Box(
        modifier =
            Modifier
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .graphicsLayer { alpha = if (enabled) 1f else 0.55f }
                .clip(shape)
                .background(container)
                .border(1.dp, if (selected) colors.textPrimary else colors.cardBorder, shape)
                .semantics {
                    role = Role.Checkbox
                    this.selected = selected
                }.padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(label, color = ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

/** Título de una sección con su nota debajo. */
@Composable
internal fun SheetSectionTitle(
    title: String,
    colors: OperationalColors,
    note: String? = null,
) {
    Column {
        Text(title, color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
        if (note !=
            null
        ) {
            Text(
                note,
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Un código que se dicta: una ficha por letra; mientras llega, fichas que brillan. Con ocho, dos grupos de cuatro. */
@Composable
internal fun CodeTiles(
    code: String?,
    length: Int,
    colors: OperationalColors,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = code?.toList()?.joinToString(" ") ?: "Sacando el código" },
    ) {
        repeat(length) { index ->
            if (length == 8 && index == 4) Spacer(Modifier.width(8.dp))
            val letter = code?.getOrNull(index)
            if (letter == null) {
                SkeletonBlock(modifier = Modifier.size(width = 36.dp, height = 48.dp), corner = 12.dp)
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(width = 36.dp, height = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cardInner)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter.toString(), color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
            }
        }
    }
}

/** "Sirve una vez, hasta hoy a las 13:40." */
internal fun expiryText(
    code: GuardianLinkCode,
    sayDay: Boolean,
): String {
    val expires =
        code.expiresAt?.let {
            runCatching {
                OffsetDateTime
                    .parse(
                        it,
                    ).atZoneSameInstant(ZoneId.systemDefault())
            }.getOrNull()
        }
            ?: return "Sirve una sola vez."
    val time = expires.format(DateTimeFormatter.ofPattern("HH:mm"))
    if (!sayDay) return "Sirve una vez, hasta las $time."
    val day = if (expires.toLocalDate() == LocalDate.now()) "hoy" else "mañana"
    return "Sirve una vez, hasta $day a las $time."
}

/** Pasos numerados de una guía. */
@Composable
internal fun GuideStep(
    number: Int,
    text: String,
    ink: Color,
    circle: Color,
    line: Color,
) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(circle)
                    .border(1.dp, line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Text(
            text,
            color = ink,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(start = 12.dp, top = 2.dp),
        )
    }
}

/** Una fila discreta con ícono, título, detalle y flecha (o progreso). */
@Composable
internal fun QuietRow(
    icon: ImageVector,
    title: String,
    detail: String?,
    ink: Color,
    muted: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    trailing: ImageVector? = Icons.Outlined.ChevronRight,
    trailingDescription: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(enabled = enabled && !busy, scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics(mergeDescendants = true) { role = Role.Button }
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(title, color = muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (detail != null) Text(detail, color = muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        when {
            busy -> CircularProgressIndicator(color = ink, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            trailing != null ->
                Icon(
                    trailing,
                    contentDescription = trailingDescription,
                    tint = muted,
                    modifier = Modifier.size(18.dp),
                )
        }
    }
}

/** El aviso tal como lo manda el servidor, con la marca de borrador mientras no lo revise un abogado. */
@Composable
internal fun PrivacyNoticeText(
    notice: PrivacyNotice,
    colors: OperationalColors,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardInner)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(notice.title, color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
        notice.paragraphs.forEach { Text(it, color = colors.textPrimary, fontSize = 14.sp, lineHeight = 20.sp) }
        if (notice.isDraft) {
            Text(
                "Borrador en revisión legal · versión ${notice.version}",
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }
    }
}

/** La casilla del aviso: marcada va en tinta, como lo elegido en el resto de la app. */
@Composable
internal fun ConsentCheck(
    checked: Boolean,
    enabled: Boolean,
    colors: OperationalColors,
    testTag: String,
    onToggle: () -> Unit,
) {
    val reduced = reducedMotion()
    val scale by animateFloatAsState(
        if (checked) 1f else 0.94f,
        if (reduced) snap() else spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "consent-scale",
    )
    val box = animateSelectionColor(if (checked) colors.textPrimary else colors.cardInner, "consent-box")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onToggle)
                .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
                .semantics(mergeDescendants = true) {
                    role = Role.Checkbox
                    selected = checked
                    contentDescription = if (checked) "Aceptado" else "Sin aceptar"
                },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.clip(RoundedCornerShape(7.dp))
                    .background(box)
                    .border(1.5.dp, if (checked) Color.Transparent else colors.cardBorder, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.background,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            "Soy su mamá, papá o tutor y acepto que la escuela y Vaiinilla usen sus datos, incluidas sus alergias, como dice el aviso.",
            color = colors.textPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Esqueleto de unas filas mientras llega algo. */
@Composable
internal fun SkeletonRows(count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) { SkeletonBlock(modifier = Modifier.fillMaxWidth().height(40.dp), corner = 12.dp) }
    }
}

@Composable
internal fun CenteredNote(
    text: String,
    color: Color,
) {
    Text(
        text,
        color = color,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
