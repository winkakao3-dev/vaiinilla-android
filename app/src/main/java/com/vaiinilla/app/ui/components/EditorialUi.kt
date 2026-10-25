package com.vaiinilla.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import com.vaiinilla.app.ui.theme.seaGlass
import com.vaiinilla.app.ui.theme.seaGlassInk
import com.vaiinilla.app.ui.theme.seaGlassNight

/**
 * Búsqueda. [seaGlass]: una pieza de vidrio vainilla, de la misma familia que las categorías sin elegir (menú y
 * descubrir, como iOS); sin él, un campo plano sobre la hoja (la ayuda).
 */
@Composable
fun EditorialSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    seaGlass: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    val ink = if (seaGlass) seaGlassInk(SeaGlassTone.vainilla) else colors.ink
    val night = seaGlassNight()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = if (seaGlass) 52.dp else 50.dp)
                .then(
                    if (seaGlass) {
                        Modifier.seaGlass(SeaGlassTone.vainilla, CircleShape, night)
                    } else {
                        Modifier.clip(RoundedCornerShape(18.dp)).background(colors.paper2)
                    },
                ).padding(horizontal = if (seaGlass) 18.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = ink, modifier = Modifier.size(19.dp))
        Spacer(Modifier.size(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            // El campo ocupa todo el alto de la pieza: antes solo la línea de texto (~22 dp) recibía el toque (BOT-09).
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics { contentDescription = placeholder },
            singleLine = true,
            textStyle = TextStyle(color = ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(ink),
            decorationBox = { input ->
                Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = if (seaGlass) ink.copy(alpha = 0.7f) else colors.muted,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    input()
                }
            },
        )
        if (value.isNotEmpty() && seaGlass) {
            Spacer(Modifier.size(10.dp))
            SeaGlassButton(
                tone = SeaGlassTone.vainilla.picked,
                onClick = { onValueChange("") },
                modifier =
                    Modifier
                        .size(30.dp)
                        .semantics { contentDescription = "Limpiar búsqueda" },
                shape = CircleShape,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        } else if (value.isNotEmpty()) {
            Spacer(Modifier.size(10.dp))
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.line)
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = { onValueChange("") })
                        .semantics { contentDescription = "Limpiar búsqueda" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = ink, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun EditorialTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(7.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = if (singleLine) 48.dp else 86.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(17.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = label },
                singleLine = singleLine,
                visualTransformation =
                    if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = TextStyle(color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp),
                cursorBrush = SolidColor(colors.ink),
                decorationBox = { input ->
                    Box {
                        if (value.isBlank()) {
                            Text(placeholder, color = colors.muted, fontSize = 14.sp)
                        }
                        input()
                    }
                },
            )
        }
    }
}

@Composable
fun EditorialNotesField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(7.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 86.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(17.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp),
                cursorBrush = SolidColor(colors.ink),
                decorationBox = { input ->
                    Box {
                        if (value.isBlank()) {
                            Text(placeholder, color = colors.muted, fontSize = 14.sp)
                        }
                        input()
                    }
                },
            )
        }
    }
}

@Composable
fun EditorialPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Color? = null,
    contentColor: Color? = null,
) {
    val colors = LocalVaiinillaColors.current
    val bg = background ?: colors.ink
    val fg = contentColor ?: colors.paper
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .physicalPress(
                    scale = PhysicalPressScale.Default,
                    enabled = enabled,
                    onClick = onClick,
                ),
        color = if (enabled) bg else colors.paper2,
        shape = RoundedCornerShape(18.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                color = if (enabled) fg else colors.muted,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
            )
        }
    }
}

/** La acción principal: cápsula de vidrio lima de 54, como `VaiinillaPrimaryButtonStyle` en iOS. */
@Composable
fun EditorialAccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SeaGlassCapsuleButton(SeaGlassTone.lima, text, onClick, modifier, enabled)
}

/** La acción secundaria: cápsula de vidrio vainilla de 54, como `VaiinillaSecondaryButtonStyle` en iOS. */
@Composable
fun EditorialSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SeaGlassCapsuleButton(SeaGlassTone.vainilla, text, onClick, modifier, enabled)
}

@Composable
private fun SeaGlassCapsuleButton(
    tone: SeaGlassTone,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    SeaGlassButton(
        tone = tone,
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        enabled = enabled,
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 18.dp))
    }
}

@Composable
fun EditorialSectionHead(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, color = colors.ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
        when {
            onTrailingClick != null ->
                TextButton(onClick = onTrailingClick) {
                    Text(trailing.orEmpty(), color = colors.muted, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            trailing != null ->
                Text(trailing, color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun EditorialHero(
    eyebrow: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    watermark: String? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 208.dp),
        color = colors.accent,
        shape = RoundedCornerShape(34.dp),
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            if (watermark != null) {
                Text(
                    watermark,
                    modifier = Modifier.align(Alignment.TopEnd),
                    color = colors.accentInk.copy(alpha = 0.12f),
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Column {
                Text(
                    eyebrow.uppercase(),
                    color = colors.accentInk.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    title,
                    color = colors.accentInk,
                    fontSize = 34.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    body,
                    color = colors.accentInk.copy(alpha = 0.82f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Spacer(Modifier.height(20.dp))
                actions()
            }
        }
    }
}

@Composable
fun EditorialConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.paper,
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(
                    message,
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Spacer(Modifier.height(18.dp))
                EditorialAccentButton(text = confirmLabel, onClick = onConfirm)
                Spacer(Modifier.height(8.dp))
                EditorialSecondaryButton(text = dismissLabel, onClick = onDismiss)
            }
        }
    }
}

/** Círculo de vidrio vainilla con un ícono (regresar, cerrar), como el `chromeButton` de iOS. */
@Composable
fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
) {
    SeaGlassButton(
        tone = SeaGlassTone.vainilla,
        onClick = onClick,
        modifier = modifier.size(size).semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
    }
}

/** Las iniciales de la cuenta en un círculo vainilla de 42. Mantener presionado cambia el tema, como en iOS. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileInitialsButton(
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .size(42.dp)
                .seaGlass(SeaGlassTone.vainilla, CircleShape, seaGlassNight())
                .clip(CircleShape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .semantics { contentDescription = "Cuenta" },
        contentAlignment = Alignment.Center,
    ) {
        // Sin nombre no hay iniciales: la persona como ícono, nunca un "?" que se lee como ayuda.
        if (initials.isBlank() || initials == "?") {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint = seaGlassInk(SeaGlassTone.vainilla),
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(initials, color = seaGlassInk(SeaGlassTone.vainilla), fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

/**
 * Una opción que se elige (categoría, segmento, filtro). La elegida se rellena de tinta, no de lima: el lima es solo de
 * la acción principal de la pantalla. El cambio se funde con un resorte. [seaGlass]: las sin elegir son piezas de
 * vidrio vainilla (filas de categorías y filtros, como iOS); sin él, planas (las opciones dentro de una hoja).
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
    shape: Shape = RoundedCornerShape(percent = 50),
    contentPadding: Dp = 16.dp,
    seaGlass: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    val night = seaGlassNight()
    val restInk = if (seaGlass) seaGlassInk(SeaGlassTone.vainilla) else colors.ink
    val fill by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (reducedMotion()) snap() else spring(dampingRatio = 0.8f, stiffness = 520f),
        label = "choice-chip-fill",
    )
    Box(
        modifier =
            modifier
                .heightIn(min = height)
                .then(if (seaGlass) Modifier.seaGlass(SeaGlassTone.vainilla, shape, night) else Modifier)
                .clip(shape)
                .background(lerp(if (seaGlass) Color.Transparent else colors.paper2, colors.ink, fill))
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = lerp(restInk, colors.onInk, fill),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = contentPadding, vertical = 8.dp),
        )
    }
}
