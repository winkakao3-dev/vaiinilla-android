package com.vaiinilla.app.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** Lo que va al final de una fila: ir a otra pantalla, una opción marcada o nada. */
enum class SettingsTrailing { Chevron, Check, None }

/** Un grupo de ajustes con su título corto, como las listas agrupadas de iOS/Android. */
@Composable
fun SettingsGroup(
    title: String?,
    modifier: Modifier = Modifier,
    rows: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title,
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.paper2),
            content = rows,
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: SettingsTrailing = SettingsTrailing.Chevron,
    checked: Boolean = false,
    titleColor: Color = LocalVaiinillaColors.current.ink,
    showDivider: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .physicalPress(enabled = enabled, scale = PhysicalPressScale.Nav, onClick = onClick)
                    .then(
                        if (trailing == SettingsTrailing.Check) {
                            Modifier.semantics { selected = checked }
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = if (icon != null) 14.dp else 0.dp)) {
                Text(title, color = titleColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            when (trailing) {
                SettingsTrailing.Chevron ->
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.muted,
                    )
                SettingsTrailing.Check ->
                    androidx.compose.animation.AnimatedVisibility(
                        visible = checked,
                        enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 600f)) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.ink)
                    }
                SettingsTrailing.None -> Unit
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = colors.line,
                thickness = 1.dp,
                modifier = Modifier.padding(start = if (icon != null) 52.dp else 16.dp),
            )
        }
    }
}
