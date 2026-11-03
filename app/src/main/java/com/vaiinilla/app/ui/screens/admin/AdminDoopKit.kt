package com.vaiinilla.app.ui.screens.admin

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.WholeWordTitle
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.theme.Yolk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// Las piezas de las pantallas del admin como las dibuja el lienzo de Doop «Demo Pastelería Vaini» (-ayTrn375p):
// la primera vista con la foto y el módulo en grande, los subtítulos, las tarjetas, los mosaicos y el aviso centrado.
// Son las mismas de iOS (AdminDoopKit.swift), con los colores de OperationalColors.

/** El coral para texto: más oscuro en el tema claro para que se lea sobre el papel. */
internal val OperationalColors.coralText: Color get() = if (isDark) Color(0xFFF57670) else Color(0xFFB8332D)

/**
 * La primera vista de cada pestaña, como el marco 03: la foto del negocio de fondo detrás de la barra de estado, una
 * línea chica y el nombre del módulo en grande; debajo, el contenido. Al bajar, la reemplaza [AdminCompactBar].
 */
@Composable
internal fun AdminHeroPage(
    cover: String?,
    caption: String,
    title: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    compactTitle: String = caption,
    compactLogo: String? = null,
    padsContent: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
    floating: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val heroGone by rememberHeroGone(state)
    Box(modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item(key = "admin-hero") { AdminHeroHeader(cover, caption, title, colors, trailing) }
            if (padsContent) {
                adminPadded(content)
            } else {
                content()
            }
        }
        AdminCompactBar(
            visible = heroGone,
            title = compactTitle,
            logoUrl = compactLogo,
            colors = colors,
            trailing = trailing,
        )
        floating()
    }
}

/** Envuelve cada renglón de la lista con el margen de 16 de la pantalla. */
private fun LazyListScope.adminPadded(content: LazyListScope.() -> Unit) {
    val inner = this
    object : LazyListScope by inner {
        override fun item(
            key: Any?,
            contentType: Any?,
            content: @Composable androidx.compose.foundation.lazy.LazyItemScope.() -> Unit,
        ) {
            inner.item(key, contentType) { Box(Modifier.padding(horizontal = 16.dp)) { content() } }
        }

        override fun items(
            count: Int,
            key: ((index: Int) -> Any)?,
            contentType: (index: Int) -> Any?,
            itemContent: @Composable androidx.compose.foundation.lazy.LazyItemScope.(index: Int) -> Unit,
        ) {
            inner.items(count, key, contentType) { i -> Box(Modifier.padding(horizontal = 16.dp)) { itemContent(i) } }
        }
    }.content()
}

/** La foto con el degradado hacia el fondo, la línea chica y el título en mayúsculas; arriba a la derecha, los botones. */
@Composable
internal fun AdminHeroHeader(
    cover: String?,
    caption: String,
    title: String,
    colors: OperationalColors,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val topInset = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val fade =
        remember(colors.background) {
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.3f),
                0.3f to Color.Transparent,
                0.72f to colors.background.copy(alpha = 0.75f),
                1f to colors.background,
            )
        }
    Box(Modifier.fillMaxWidth().height(250.dp + topInset).background(colors.cardInner)) {
        if (cover != null) {
            ProductImage(
                imageUrl = cover,
                contentDescription = "",
                modifier = Modifier.fillMaxSize(),
                placeholder = { m -> Box(m.background(colors.cardInner)) },
            )
        }
        Box(Modifier.fillMaxSize().background(fade))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, end = 22.dp, bottom = 14.dp)) {
            Text(
                caption,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            WholeWordTitle(
                text = title.uppercase(Locale("es", "MX")),
                color = colors.textPrimary,
                fontSize = 52.sp,
                lineHeight = 52.sp,
                letterSpacing = (-2).sp,
                maxLines = 1,
                modifier = Modifier.semantics { heading() },
            )
        }
        Row(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 12.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = trailing,
        )
    }
}

/** El botón redondo sobre la foto (buscar, avisos). */
@Composable
internal fun AdminHeroButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
            .size(46.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Una pestaña sin foto: el negocio en chico y el título grande, y debajo el contenido con margen de 16. */
@Composable
internal fun AdminPage(
    caption: String,
    title: String,
    colors: OperationalColors,
    trailing: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
    ) {
        item(key = "admin-page-header") { AdminPageHeader(caption, title, colors, trailing) }
        content()
    }
}

/** «ADMINISTRACIÓN · MI NEGOCIO» en chico y el título de la pestaña en grande. */
@Composable
internal fun AdminPageHeader(
    caption: String,
    title: String,
    colors: OperationalColors,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().adminRise(0), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AdminCapsLabel(caption, colors, inset = false)
            Text(
                title,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        trailing()
    }
}

/** Subtítulo de un bloque («Lo más vendido»), con una acción opcional a la derecha. */
@Composable
internal fun AdminH2(
    title: String,
    colors: OperationalColors,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (action != null && onAction != null) {
            Text(
                action,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textSecondary,
                modifier =
                    Modifier
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onAction)
                        .heightIn(min = 32.dp)
                        .padding(vertical = 6.dp),
            )
        }
    }
}

/** La tarjeta del lienzo: fondo de tarjeta, esquinas de 24 y 14×16 de margen; sin línea en la noche. */
@Composable
internal fun AdminPanel(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val border =
        when {
            highlighted -> Modifier.border(1.5.dp, colors.accentLime, shape)
            !colors.isDark -> Modifier.border(1.dp, colors.cardBorder, shape)
            else -> Modifier
        }
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardBackground)
            .then(border)
            .padding(padding),
        content = content,
    )
}

/** Una fila de tarjeta: lo de la izquierda (foto o ícono), el título con su detalle y lo de la derecha. */
@Composable
internal fun AdminRow(
    title: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    detail: String? = null,
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.physicalPress(onClick = onClick) else Modifier)
            .padding(vertical = 11.dp)
            .graphicsLayer { alpha = if (dimmed) 0.55f else 1f },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detail.isNullOrBlank()) {
                Text(
                    detail,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    lineHeight = 16.sp,
                )
            }
        }
        trailing()
    }
}

/** El monto de la derecha de una fila. */
@Composable
internal fun AdminRowValue(
    text: String,
    colors: OperationalColors,
    muted: Boolean = false,
) {
    Text(
        text,
        fontSize = if (muted) 13.sp else 15.sp,
        fontWeight = if (muted) FontWeight.Bold else FontWeight.ExtraBold,
        color = if (muted) colors.textMuted else colors.textPrimary,
        maxLines = 1,
        textAlign = TextAlign.End,
    )
}

/** La línea fina entre filas de una tarjeta. */
@Composable
internal fun AdminRowDivider(colors: OperationalColors) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardBorder))
}

/** Foto cuadrada de 44 de un producto en una fila. */
@Composable
internal fun AdminThumb(
    url: String?,
    name: String,
    colors: OperationalColors,
    size: Dp = 44.dp,
) {
    val box = Modifier.size(size).clip(RoundedCornerShape(12.dp))
    if (url != null) {
        ProductImage(imageUrl = url, contentDescription = name, modifier = box, card = true)
    } else {
        Box(box.background(colors.cardInner), contentAlignment = Alignment.Center) {
            Text(adminInitials(name), fontSize = 14.sp, fontWeight = FontWeight.Black, color = colors.textSecondary)
        }
    }
}

/** La barra de progreso del lienzo (8 de alto); se llena con un resorte al aparecer. */
@Composable
internal fun AdminBar(
    fraction: Float,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    fill: Color? = null,
) {
    val reduced = reducedMotion()
    val grow = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay(150)
            grow.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 120f))
        }
    }
    val target by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(dampingRatio = 0.85f), label = "admin-bar")
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(
                8.dp,
            ).clip(CircleShape)
            .background(colors.textPrimary.copy(alpha = if (colors.isDark) 0.08f else 0.1f)),
    ) {
        val width = (maxWidth * target * grow.value).coerceAtLeast(8.dp)
        Box(
            Modifier
                .width(width)
                .fillMaxSize()
                .clip(CircleShape)
                .background(fill ?: colors.accentLime),
        )
    }
}

/** Un número grande con su etiqueta en un mosaico («1 · Por aceptar»). */
@Composable
internal fun AdminCountTile(
    value: String,
    label: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    AdminPanel(
        colors = colors,
        modifier =
            modifier
                .then(if (onClick != null) Modifier.physicalPress(onClick = onClick) else Modifier)
                .semantics(mergeDescendants = true) {},
        padding = PaddingValues(12.dp),
    ) {
        Text(
            value,
            fontWeight = FontWeight.Black,
            color = tint ?: colors.textPrimary,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 26.sp),
        )
        Text(
            label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
            maxLines = 2,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Un dato con su etiqueta arriba, como los mosaicos de dos en dos del lienzo («Fondo inicial · $500»). */
@Composable
internal fun AdminStatPanel(
    label: String,
    value: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    AdminPanel(colors = colors, modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMuted, maxLines = 1)
        Text(
            value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Un campo con su etiqueta adentro: fondo del papel, borde fino y lima al escribir. */
@Composable
internal fun AdminTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    prefix: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        if (focused) colors.accentLime else colors.cardBorder,
        spring(),
        label = "admin-field",
    )
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        maxLines = if (singleLine) 1 else 6,
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        cursorBrush = SolidColor(colors.textPrimary),
        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary),
        modifier = modifier.fillMaxWidth().graphicsLayer { alpha = if (enabled) 1f else 0.6f },
        decorationBox = { inner ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.background)
                    .border(if (focused) 1.5.dp else 1.dp, borderColor, shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    prefix?.let {
                        Text(
                            it,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textMuted,
                        )
                    }
                    Box {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                placeholder,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textMuted.copy(alpha = 0.6f),
                            )
                        }
                        inner()
                    }
                }
            }
        },
    )
}

/** Un interruptor en una fila de tarjeta, con su explicación debajo. */
@Composable
internal fun AdminToggleRow(
    title: String,
    checked: Boolean,
    colors: OperationalColors,
    onChange: (Boolean) -> Unit,
    detail: String? = null,
    enabled: Boolean = true,
) {
    val haptics = rememberVaiinillaHaptics()
    AdminRow(title = title, colors = colors, detail = detail) {
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = {
                haptics.selection()
                onChange(it)
            },
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = colors.accentLime,
                    checkedThumbColor = if (colors.isDark) colors.textPrimary else colors.accentInk,
                    uncheckedTrackColor = colors.cardInner,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedBorderColor = colors.cardBorder,
                ),
        )
    }
}

/** Una opción con su círculo de elegir, como «Elige el periodo» y «Cambiar rol». */
@Composable
internal fun AdminRadioRow(
    title: String,
    selected: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
    detail: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val haptics = rememberVaiinillaHaptics()
    val shape = RoundedCornerShape(20.dp)
    val border by animateColorAsState(
        when {
            selected -> colors.accentLime
            colors.isDark -> Color.Transparent
            else -> colors.cardBorder
        },
        spring(),
        label = "admin-radio",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .physicalPress(scale = PhysicalPressScale.Small) {
                haptics.selection()
                onClick()
            }.semantics {
                role = Role.RadioButton
                this.selected = selected
            }.clip(shape)
            .background(colors.cardBackground)
            .border(if (selected) 1.5.dp else 1.dp, border, shape)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).border(2.dp, if (selected) colors.accentLime else colors.textMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val dot by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.6f), label = "radio-dot")
            Box(
                Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = dot
                        scaleY = dot
                    }.clip(CircleShape)
                    .background(colors.accentLime),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
            detail?.let { Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted) }
        }
        trailing()
    }
}

/**
 * Un aviso para confirmar algo que no se deshace fácil (cerrar sin cobrar, archivar, quitar un acceso): sale centrado
 * sobre todo el admin. Con [fieldPlaceholder] pide un texto (un motivo) y solo deja confirmar si [accepts] lo da por
 * bueno. [run] devuelve si salió bien; si no, el aviso se queda abierto para reintentar.
 */
internal class AdminDialogRequest(
    val title: String,
    val message: String,
    val confirm: String,
    val icon: ImageVector = Icons.Outlined.ErrorOutline,
    val destructive: Boolean = true,
    val fieldPlaceholder: String? = null,
    val accepts: (String) -> Boolean = { true },
    /** La foto del producto en lugar del ícono, como en el marco 29. */
    val image: String? = null,
    val run: suspend (String) -> Boolean,
)

/** Pide el aviso centrado del admin; lo dibuja [AdminDialogHost] encima de la barra. */
internal val LocalAdminDialog = staticCompositionLocalOf<(AdminDialogRequest) -> Unit> { {} }

/** Da [LocalAdminDialog] a [content] y dibuja el aviso que se pida encima de todo. */
@Composable
internal fun AdminDialogHost(
    colors: OperationalColors,
    content: @Composable () -> Unit,
) {
    var request by remember { mutableStateOf<AdminDialogRequest?>(null) }
    CompositionLocalProvider(LocalAdminDialog provides { request = it }) {
        Box(Modifier.fillMaxSize()) {
            content()
            AdminDialogOverlay(request, colors) { request = null }
        }
    }
}

@Composable
private fun AdminDialogOverlay(
    request: AdminDialogRequest?,
    colors: OperationalColors,
    onDismiss: () -> Unit,
) {
    val reduced = reducedMotion()
    var shown by remember { mutableStateOf<AdminDialogRequest?>(null) }
    if (request != null) shown = request
    AnimatedVisibility(request != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        val current = shown ?: return@AnimatedVisibility
        var busy by remember(current) { mutableStateOf(false) }
        var text by remember(current) { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        val haptics = rememberVaiinillaHaptics()
        val ready = current.accepts(text)
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (!busy) onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            val pop = remember(current) { Animatable(if (reduced) 1f else 0.88f) }
            LaunchedEffect(current) { pop.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 420f)) }
            Column(
                Modifier
                    .padding(horizontal = 34.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }.clip(RoundedCornerShape(28.dp))
                    .background(colors.cardBackground)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (current.image != null) {
                    AdminThumb(current.image, current.title, colors, size = 64.dp)
                    Spacer(Modifier.height(16.dp))
                } else if (current.fieldPlaceholder == null) {
                    val tone = if (current.destructive) AdminCoral else colors.textPrimary
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(tone.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            current.icon,
                            contentDescription = null,
                            tint = if (current.destructive) colors.coralText else colors.textPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    current.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    current.message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                current.fieldPlaceholder?.let { placeholder ->
                    Spacer(Modifier.height(16.dp))
                    AdminTextField(
                        label = placeholder,
                        value = text,
                        onValueChange = { text = it },
                        colors = colors,
                        singleLine = false,
                        enabled = !busy,
                    )
                }
                Spacer(Modifier.height(if (current.fieldPlaceholder == null) 20.dp else 12.dp))
                AdminPrimaryButton(
                    title = current.confirm,
                    colors = colors,
                    busy = busy,
                    enabled = ready,
                    fill = if (current.destructive) AdminCoral else null,
                    contentColor = if (current.destructive) Color.White else null,
                ) {
                    if (busy || !ready) return@AdminPrimaryButton
                    haptics.impact()
                    busy = true
                    scope.launch {
                        val done = current.run(text)
                        busy = false
                        if (done) onDismiss()
                    }
                }
                Spacer(Modifier.height(10.dp))
                AdminSecondaryButton(title = "Cancelar", colors = colors, enabled = !busy, onClick = onDismiss)
            }
        }
    }
}

/** Un estado vacío del lienzo: ícono grande, título y una línea; con acción, el botón de tinta debajo. */
@Composable
internal fun AdminEmptyState(
    icon: ImageVector?,
    title: String,
    message: String,
    colors: OperationalColors,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(
                Modifier.size(84.dp).border(1.dp, colors.cardBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(colors.cardBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (colors.isDark) colors.accentLime else colors.textPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            message,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            AdminPrimaryButton(title = action, colors = colors, icon = Icons.Outlined.Add, fullWidth = false, onClick = onAction)
        }
    }
}

/** El botón redondo de 44 de las cabeceras (volver, cerrar, avisos). */
@Composable
internal fun AdminRoundButton(
    icon: ImageVector,
    label: String,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.cardBackground),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = label, tint = colors.textPrimary, modifier = Modifier.size(19.dp)) }
}

/** Chip de filtro del lienzo: cápsula de 36 con borde; la elegida, de tinta. */
@Composable
internal fun AdminFilterChip(
    label: String,
    selected: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
    count: Int? = null,
    height: Dp = 36.dp,
) {
    val haptics = rememberVaiinillaHaptics()
    val bg by animateColorAsState(
        if (selected) colors.textPrimary else colors.cardBackground,
        spring(),
        label = "chip-bg",
    )
    val fg by animateColorAsState(
        if (selected) colors.background else colors.textSecondary,
        spring(),
        label = "chip-fg",
    )
    Row(
        Modifier
            .physicalPress(scale = PhysicalPressScale.Small) {
                if (!selected) haptics.selection()
                onClick()
            }.semantics { this.selected = selected }
            .height(height)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) colors.textPrimary else colors.cardBorder, CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
        count?.let { Text("$it", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg.copy(alpha = 0.7f)) }
    }
}

/** Etiqueta de sección en mayúsculas espaciadas, encima de una tarjeta. */
@Composable
internal fun AdminCapsLabel(
    text: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    inset: Boolean = true,
) {
    Text(
        text.uppercase(Locale("es", "MX")),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        color = colors.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = if (inset) 4.dp else 0.dp).semantics { heading() },
    )
}

internal enum class AdminBadgeTone { OK, WARN, DANGER, NEUTRAL }

/** Una pastilla de estado («Abierta», «Pendiente»); el lima y el amarillo se oscurecen en el tema claro. */
@Composable
internal fun AdminBadge(
    text: String,
    colors: OperationalColors,
    tone: AdminBadgeTone = AdminBadgeTone.NEUTRAL,
    dot: Boolean = false,
    caps: Boolean = true,
) {
    val fg =
        when (tone) {
            AdminBadgeTone.OK -> if (colors.isDark) colors.accentLime else colors.accentInk
            AdminBadgeTone.WARN -> if (colors.isDark) Yolk else colors.textPrimary
            AdminBadgeTone.DANGER -> colors.coralText
            AdminBadgeTone.NEUTRAL -> colors.textSecondary
        }
    val bg =
        when (tone) {
            AdminBadgeTone.OK -> colors.accentLime.copy(alpha = if (colors.isDark) 0.16f else 0.55f)
            AdminBadgeTone.WARN -> Yolk.copy(alpha = if (colors.isDark) 0.14f else 0.5f)
            AdminBadgeTone.DANGER -> AdminCoral.copy(alpha = 0.14f)
            AdminBadgeTone.NEUTRAL -> colors.textPrimary.copy(alpha = 0.1f)
        }
    Row(
        Modifier
            .height(24.dp)
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (dot) Box(Modifier.size(6.dp).clip(CircleShape).background(fg))
        Text(
            if (caps) text.uppercase(Locale("es", "MX")) else text,
            fontSize = if (caps) 11.sp else 12.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = if (caps) 0.9.sp else 0.sp,
            color = fg,
            maxLines = 1,
        )
    }
}

/** La acción principal del admin: cápsula de tinta (el lienzo la pinta así, no de lima). */
@Composable
internal fun AdminPrimaryButton(
    title: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    busy: Boolean = false,
    enabled: Boolean = true,
    fill: Color? = null,
    contentColor: Color? = null,
    /** En una barra de arriba: solo el ancho de su texto y más bajo, como «+ Espacio» del marco 45. */
    fullWidth: Boolean = true,
    onClick: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val fg = contentColor ?: colors.background
    val alpha by animateFloatAsState(if (enabled || busy) 1f else 0.4f, spring(), label = "primary-alpha")
    Row(
        modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .physicalPress(enabled = enabled && !busy) {
                haptics.click()
                onClick()
            }.graphicsLayer { this.alpha = alpha }
            .height(if (fullWidth) 54.dp else 38.dp)
            .clip(CircleShape)
            .background(fill ?: colors.textPrimary)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
        Text(
            title,
            fontSize = if (fullWidth) 16.sp else 13.5.sp,
            fontWeight = FontWeight.Black,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Botón secundario: cápsula tenue sobre la tarjeta, con borde si va suelto o de tinta si es la acción de una fila. */
@Composable
internal fun AdminSecondaryButton(
    title: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    icon: ImageVector? = null,
    outlined: Boolean = false,
    filled: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val fg =
        when {
            filled -> colors.background
            destructive -> colors.coralText
            else -> colors.textPrimary
        }
    val bg =
        when {
            filled -> colors.textPrimary
            outlined -> Color.Transparent
            else -> colors.textPrimary.copy(alpha = 0.07f)
        }
    Row(
        modifier
            .fillMaxWidth()
            .physicalPress(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .heightIn(min = if (outlined) 54.dp else 46.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (outlined) Modifier.border(1.dp, colors.cardBorder, CircleShape) else Modifier)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp)) }
        Text(
            title,
            fontSize = if (outlined) 16.sp else 14.5.sp,
            fontWeight = if (outlined) FontWeight.Black else FontWeight.ExtraBold,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Dos opciones excluyentes como pastilla deslizante de tinta. */
@Composable
internal fun <T> AdminSegmented(
    options: List<Pair<T, String>>,
    selected: T?,
    colors: OperationalColors,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = rememberVaiinillaHaptics()
    SlidingSegments(
        labels = options.map { it.second },
        selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0),
        onSelect = { i ->
            if (options[i].first != selected) {
                haptics.selection()
                onSelect(options[i].first)
            }
        },
        trackColor = colors.background,
        indicatorColor = colors.textPrimary,
        selectedContentColor = colors.background,
        idleContentColor = colors.textMuted,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
    )
}

/** El cuadro chico con el ícono de una fila de navegación. */
@Composable
internal fun AdminIconBox(
    icon: ImageVector,
    colors: OperationalColors,
) {
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(colors.textPrimary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(19.dp)) }
}

/** Entrada de una pieza al abrir la pantalla: sube y aparece con un resorte, escalonada por [index]. Una sola vez. */
@Composable
internal fun Modifier.adminRise(index: Int = 0): Modifier {
    val reduced = reducedMotion()
    var shown by rememberSaveable { mutableStateOf(reduced) }
    val progress = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(index * 50L)
            progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 260f))
            shown = true
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 18.dp.toPx()
    }
}
