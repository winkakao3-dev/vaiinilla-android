package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.ChoiceChip
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.WholeWordTitle
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors

/**
 * La cabecera de cada pestaña, como la del menú del alumno: la foto (o el logo) de borde a borde que se funde con
 * el fondo, controles de vidrio arriba y el título grande abajo. Va como primer renglón de la lista: se va con el
 * scroll y la reemplaza [AdminCompactBar].
 */
@Composable
internal fun AdminHero(
    title: String,
    subtitle: String?,
    colors: OperationalColors,
    height: Dp,
    photoUrl: String? = null,
    logoUrl: String? = null,
    showLogo: Boolean = false,
    logoName: String = title,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
) {
    val fade =
        remember(colors.background) {
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.35f),
                0.22f to Color.Black.copy(alpha = 0f),
                0.48f to colors.background.copy(alpha = 0f),
                0.8f to colors.background.copy(alpha = 0.9f),
                1f to colors.background,
            )
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .background(colors.cardInner),
    ) {
        if (photoUrl != null) {
            ProductImage(
                imageUrl = photoUrl,
                contentDescription = "",
                modifier = Modifier.fillMaxSize(),
                placeholder = { m -> HeroLogo(logoUrl, logoName, colors, showLogo, m) },
            )
        } else {
            HeroLogo(logoUrl, logoName, colors, showLogo, Modifier.fillMaxSize())
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
            if (leading != null) leading() else Spacer(Modifier.size(1.dp))
            trailing?.invoke()
        }
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                subtitle?.let {
                    Text(
                        it,
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                WholeWordTitle(
                    text = title.uppercase(),
                    color = colors.textPrimary,
                    fontSize = 46.sp,
                    lineHeight = 46.sp,
                    letterSpacing = (-1.2).sp,
                    maxLines = 2,
                )
            }
            titleTrailing?.let { Box(Modifier.padding(bottom = 8.dp)) { it() } }
        }
    }
}

/** Sin foto: el logo del negocio al centro (si [show]) sobre el papel de la tarjeta. */
@Composable
private fun HeroLogo(
    logoUrl: String?,
    name: String,
    colors: OperationalColors,
    show: Boolean,
    modifier: Modifier,
) {
    Box(modifier.background(colors.cardInner), contentAlignment = Alignment.Center) {
        if (show) AdminVenueLogo(logoUrl, name, colors, size = 116.dp, modifier = Modifier.padding(bottom = 70.dp))
    }
}

/** El logo del negocio, o sus iniciales sobre tinta si no tiene. */
@Composable
internal fun AdminVenueLogo(
    logoUrl: String?,
    name: String,
    colors: OperationalColors,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(size * 0.28f)
    val initials = remember(name) { adminInitials(name) }
    val fallback: @Composable (Modifier) -> Unit = { m ->
        Box(m.background(colors.textPrimary), contentAlignment = Alignment.Center) {
            Text(initials, color = colors.background, fontWeight = FontWeight.Black, fontSize = (size.value * 0.4f).sp)
        }
    }
    val box = modifier.size(size).clip(shape)
    if (logoUrl != null) {
        ProductImage(
            imageUrl = logoUrl,
            contentDescription = "Logo de $name",
            modifier = box,
            placeholder = fallback,
            card = true,
        )
    } else {
        fallback(box)
    }
}

internal fun adminInitials(name: String): String =
    name
        .split(' ')
        .filter { it.firstOrNull()?.isLetterOrDigit() == true }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "V" }

/** Pastilla de vidrio oscuro sobre la foto: se lee igual sobre cualquier imagen. Sin [onClick] es solo un aviso. */
@Composable
internal fun AdminHeroPill(
    label: String,
    dot: Color? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier =
            Modifier
                .height(40.dp)
                .then(
                    if (onClick != null) {
                        Modifier.physicalPress(scale = PhysicalPressScale.Small) {
                            haptics.click()
                            onClick()
                        }
                    } else {
                        Modifier
                    },
                ).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        dot?.let { Box(Modifier.size(8.dp).clip(CircleShape).background(it)) }
        icon?.let { Icon(it, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

/** Arriba al bajar: el negocio en chico, lo de la derecha y, si hay, los chips de la pestaña. */
@Composable
internal fun BoxScope.AdminCompactBar(
    visible: Boolean,
    title: String,
    logoUrl: String?,
    colors: OperationalColors,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.TopCenter),
        enter =
            fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                slideInVertically(spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)) { -it / 3 },
        exit =
            fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                slideOutVertically(spring(stiffness = Spring.StiffnessMedium)) { -it / 3 },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.background.copy(alpha = 0.97f))
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AdminVenueLogo(logoUrl, title, colors, size = 32.dp)
                Text(
                    title.uppercase(),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke(this)
            }
            below?.invoke()
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardBorder.copy(alpha = 0.5f)))
        }
    }
}

/** La cabecera ya salió de la pantalla: es cuando aparece la barra compacta. */
@Composable
internal fun rememberHeroGone(state: LazyListState): State<Boolean> =
    remember(state) { derivedStateOf { state.firstVisibleItemIndex > 0 } }

@Composable
internal fun rememberHeroGone(state: LazyGridState): State<Boolean> =
    remember(state) { derivedStateOf { state.firstVisibleItemIndex > 0 } }

/** Una fila de chips de vidrio que se desliza de lado, con el margen de la pantalla. */
@Composable
internal fun AdminChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
internal fun AdminGlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    ChoiceChip(
        label = label,
        selected = selected,
        onClick = {
            if (!selected) haptics.selection()
            onClick()
        },
        height = 44.dp,
        seaGlass = true,
    )
}

/** Un dato suelto en un mosaico: etiqueta chica arriba y el valor grande; con [dot], un estado en vivo. */
@Composable
internal fun AdminStatTile(
    label: String,
    value: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    dot: Color? = null,
    muted: Boolean = false,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier =
            modifier
                .clip(shape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, shape)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dot?.let { Box(Modifier.size(9.dp).clip(CircleShape).background(it)) }
            Text(
                value,
                fontSize = if (dot != null) 17.sp else 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = if (dot != null) 0.sp else (-0.8).sp,
                color = if (muted) colors.textMuted else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Título de sección con su dato al lado y, si hay, una acción a la derecha. */
@Composable
internal fun AdminCremeSectionTitle(
    title: String,
    colors: OperationalColors,
    detail: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
        detail?.let {
            Text(
                it,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                "$action ›",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier =
                    Modifier
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onAction)
                        .padding(vertical = 4.dp),
            )
        }
    }
}
