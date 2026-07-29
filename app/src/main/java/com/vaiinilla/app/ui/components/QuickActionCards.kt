package com.vaiinilla.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.Yolk

data class QuickAction(
    val icon: String,
    val title: String,
    val subtitle: String,
    val background: Color,
    val contentColor: Color,
)

/** Demo screen 02 — horizontal HOT shortcuts (Para gratinar / Sin picante / Llenador). */
@Composable
fun menuHotQuickActions(): List<QuickAction> {
    val colors = LocalVaiinillaColors.current
    return listOf(
        QuickAction(
            icon = "◉",
            title = "Para gratinar",
            subtitle = "Con queso gratinado",
            background = colors.paper2,
            contentColor = colors.ink,
        ),
        QuickAction(
            icon = "✦",
            title = "Sin picante",
            subtitle = "Sin chiles",
            background = Yolk,
            contentColor = Color(0xFF29200B),
        ),
        QuickAction(
            icon = "⌁",
            title = "Llenador",
            subtitle = "Combo burrito + bebida",
            background = Coral,
            contentColor = Color(0xFF2D1210),
        ),
    )
}

@Composable
private fun defaultQuickActions(): List<QuickAction> = menuHotQuickActions()

@Composable
fun QuickActionCards(
    modifier: Modifier = Modifier,
    actions: List<QuickAction>? = null,
    onActionClick: (QuickAction) -> Unit = {},
) {
    val resolvedActions = actions ?: defaultQuickActions()
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(resolvedActions, key = QuickAction::title) { action ->
            QuickActionCard(action = action, onClick = { onActionClick(action) })
        }
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier =
            Modifier
                .width(156.dp)
                .height(150.dp)
                .physicalPress(scale = PhysicalPressScale.Default, onClick = onClick),
        color = action.background,
        shape = RoundedCornerShape(27.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.size(45.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = colors.ink,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(45.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = action.icon,
                            color = colors.paper,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Column {
                Text(
                    text = action.title,
                    color = action.contentColor,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = action.subtitle,
                    color = action.contentColor.copy(alpha = 0.66f),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
    }
}
