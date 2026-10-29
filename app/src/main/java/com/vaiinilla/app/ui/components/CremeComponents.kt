package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.time.Instant
import java.time.OffsetDateTime

/*
 * Piezas del estilo CREME que comparten el menú, Cartera, Carrito y la ficha del hijo: la portada que se funde con
 * el papel, la fila de pills (la primera es la acción principal) y la fila de datos.
 */

/** Velo de arriba para los controles de vidrio y degradado que funde la foto con [pageColor] abajo. */
fun cremeHeroBrush(pageColor: Color): Brush =
    Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.35f),
        0.22f to Color.Black.copy(alpha = 0f),
        0.5f to pageColor.copy(alpha = 0f),
        0.82f to pageColor.copy(alpha = 0.88f),
        1f to pageColor,
    )

/**
 * La portada del lugar de borde a borde, igual a la del menú: la foto sin oscurecer, controles de vidrio arriba
 * ([leading] y [trailing]) y el [content] pegado abajo. Sin foto, el papel de tarjeta: nunca una ilustración. Va
 * de borde a borde y por detrás de la barra de estado; los controles bajan lo que mide.
 */
@Composable
fun CremeHero(
    height: Dp,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val fade = remember(colors.paper) { cremeHeroBrush(colors.paper) }
    Box(modifier = modifier.fillMaxWidth().height(height).background(colors.paper2)) {
        if (photoUrl != null) {
            ProductImage(
                imageUrl = photoUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                placeholder = { m -> Box(m.background(colors.paper2)) },
            )
        }
        Box(Modifier.fillMaxSize().background(fade))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // El control de la izquierda cede el ancho si el nombre del lugar es largo; el de la derecha no se achica.
            Box(Modifier.weight(1f, fill = false)) { leading() }
            Box(Modifier.padding(start = 8.dp)) { trailing() }
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            content = content,
        )
    }
}

/** Botón de vidrio oscuro sobre la foto: se lee igual sobre cualquier imagen. */
@Composable
fun HeroGlassPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier =
            modifier
                .height(44.dp)
                .physicalPress(onClick = {
                    haptics.click()
                    onClick()
                })
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun VenueLogo(
    logoUrl: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(size * 0.28f)
    val initials = remember(name) { venueInitials(name) }
    val fallback: @Composable (Modifier) -> Unit = { m ->
        Box(m.background(colors.ink), contentAlignment = Alignment.Center) {
            Text(initials, color = colors.onInk, fontWeight = FontWeight.Black, fontSize = (size.value * 0.36f).sp)
        }
    }
    val boxModifier = modifier.size(size).clip(shape)
    if (logoUrl != null) {
        ProductImage(
            imageUrl = logoUrl,
            contentDescription = "Logo de $name",
            modifier = boxModifier,
            placeholder = fallback,
        )
    } else {
        fallback(boxModifier)
    }
}

fun venueInitials(name: String): String =
    name
        .split(' ')
        .filter { word -> word.firstOrNull()?.isLetterOrDigit() == true }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "V" }

/** Una acción de la fila de pills. La primera de la fila es la principal de la pantalla. */
data class CremePill(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val testTag: String? = null,
    val enabled: Boolean = true,
)

/**
 * La fila de pills del estilo CREME: la primera en `accent`, las demás en `paper2` con borde e ícono `accent`.
 * Se desliza de lado si no caben.
 */
@Composable
fun CremePillRow(
    pills: List<CremePill>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        pills.forEachIndexed { index, pill -> CremePillButton(pill, primary = index == 0) }
    }
}

@Composable
private fun CremePillButton(
    pill: CremePill,
    primary: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val shape = RoundedCornerShape(27.dp)
    Row(
        modifier =
            Modifier
                .height(54.dp)
                .then(if (pill.testTag != null) Modifier.testTag(pill.testTag) else Modifier)
                .physicalPress(enabled = pill.enabled, scale = PhysicalPressScale.Small, onClick = {
                    haptics.click()
                    pill.onClick()
                })
                .clip(shape)
                .background(if (primary) colors.accent else colors.paper2)
                .then(if (primary) Modifier else Modifier.border(1.dp, colors.line, shape))
                .semantics { role = Role.Button }
                .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            pill.icon,
            contentDescription = null,
            tint = if (primary) colors.accentInk else colors.accent,
            modifier = Modifier.size(20.dp),
        )
        Text(
            pill.title,
            color = if (primary) colors.accentInk else colors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

/** Un dato de la fila de datos. Sin valor se muestra «—»: la columna no se esconde. */
data class CremeStat(
    val label: String,
    val value: String?,
    /** Los números buenos (lo que queda hoy, el total) van en `accent2`. */
    val highlighted: Boolean = false,
)

/** La fila de datos del estilo CREME: etiqueta arriba, valor abajo, en columnas iguales. */
@Composable
fun CremeStatRow(
    items: List<CremeStat>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    // Con la letra grande del sistema tres columnas parten las palabras («Recarga s»): se apilan (L01).
    if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.4f) {
        Column(
            modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.label,
                        color = colors.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        item.value ?: "—",
                        color =
                            when {
                                item.value == null -> colors.muted
                                item.highlighted -> colors.accent2
                                else -> colors.ink
                            },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                    )
                }
            }
        }
        return
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEach { item ->
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    item.label,
                    color = colors.muted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    item.value ?: "—",
                    color =
                        when {
                            item.value == null -> colors.muted
                            item.highlighted -> colors.accent2
                            else -> colors.ink
                        },
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** El lugar en una línea: su inicial (o un ícono) en un círculo de 22 dp y el nombre. */
@Composable
fun CremeVenueChip(
    label: String,
    modifier: Modifier = Modifier,
    initialOf: String = label,
    icon: ImageVector? = null,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(colors.ink),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.onInk, modifier = Modifier.size(13.dp))
            } else {
                Text(
                    initialOf.trim().take(1).uppercase(),
                    color = colors.onInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Text(label, color = colors.ink2, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
    }
}

/** El separador de 1 dp de las pantallas CREME, a 20 dp de los bordes. */
@Composable
fun CremeDivider(modifier: Modifier = Modifier) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(1.dp)
                .background(colors.line),
    )
}

/** Ocupa también el margen lateral de la lista que lo contiene: la fila se desliza hasta el borde de la pantalla. */
fun Modifier.cremeBleed(margin: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = (margin * 2).roundToPx()
        val width = constraints.maxWidth + extra
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
    }

private val MONTHS_ES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * "Hoy 1:05 pm", "Ayer 1:05 pm" o "28 oct": cuándo pasó algo, en la hora del negocio. `null` si la fecha no se lee.
 */
fun cremeMoment(
    iso: String,
    now: Instant = Instant.now(),
): String? {
    val instant = runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull() ?: return null
    val zone = BusinessTime.zone
    val local = BusinessTime.at(instant, zone)
    val today = BusinessTime.at(now, zone).toLocalDate()
    val hour = local.hour % 12
    val time = "${if (hour == 0) 12 else hour}:${"%02d".format(local.minute)} ${if (local.hour < 12) "am" else "pm"}"
    val day = local.toLocalDate()
    return when (day) {
        today -> "Hoy $time"
        today.minusDays(1) -> "Ayer $time"
        else -> "${day.dayOfMonth} ${MONTHS_ES[day.monthValue - 1]}"
    }
}
