package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.screens.OperationalColors

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
