package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.ui.admin.AdminImage
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.StaffAvatarMenu
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger

/** Las secciones del modo Administración; las del negocio y los espacios llegan en las siguientes fases. */
enum class AdminTab(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Inicio", Icons.Outlined.Home),
    MENU("Menú", Icons.Outlined.RestaurantMenu),
    ORDERS("Pedidos", Icons.Outlined.ReceiptLong),
}

/** Lo que la pantalla puede pedirle al ViewModel; agrupado para no pasar veinte lambdas sueltas. */
class AdminActions(
    val onRefreshHome: () -> Unit,
    val onRefreshCatalog: () -> Unit,
    val onSelectPeriod: (com.vaiinilla.app.ui.admin.AdminPeriodPreset) -> Unit,
    val onSaveCategory: (Int?, com.vaiinilla.app.domain.admin.AdminCategoryInput, () -> Unit) -> Unit,
    val onSaveProduct: (Int?, com.vaiinilla.app.domain.admin.AdminProductInput, AdminImage?, () -> Unit) -> Unit,
    val onSetAvailable: (com.vaiinilla.app.domain.model.Product, Boolean) -> Unit,
    val onRemoveImage: (com.vaiinilla.app.domain.model.Product) -> Unit,
    val onSelectOrderFilter: (com.vaiinilla.app.ui.admin.AdminOrderFilter) -> Unit,
    val onRefreshOrders: () -> Unit,
    val onLoadMoreOrders: () -> Unit,
)

@Composable
fun AdminScreen(
    state: AdminUiState,
    placeName: String,
    restrictedMode: RestrictedMode?,
    actions: AdminActions,
    onChangeMode: () -> Unit,
) {
    val colors = rememberOperationalColors()
    var tab by rememberSaveable { mutableStateOf(AdminTab.HOME) }
    val reduced = reducedMotion()
    val readOnly = restrictedMode == RestrictedMode.READ_ONLY

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            AdminTopBar(placeName = placeName, colors = colors, onChangeMode = onChangeMode)
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(spring(stiffness = 10_000f)) togetherWith fadeOut(spring(stiffness = 10_000f))
                    } else {
                        fadeIn(spring(stiffness = 400f)) togetherWith fadeOut(spring(stiffness = 600f))
                    }
                },
                modifier = Modifier.weight(1f),
                label = "admin-tab",
            ) { current ->
                when (current) {
                    AdminTab.HOME -> AdminHomeTab(state = state, colors = colors, actions = actions)
                    AdminTab.MENU ->
                        AdminMenuTab(
                            state = state,
                            colors = colors,
                            readOnly = readOnly,
                            actions = actions,
                        )
                    AdminTab.ORDERS -> AdminOrdersTab(state = state, colors = colors, actions = actions)
                }
            }
            AdminTabBar(selected = tab, colors = colors, onSelect = { tab = it })
        }
        ToastPill(
            message = state.notice,
            containerColor = colors.textPrimary,
            contentColor = colors.background,
            badgeColor = if (state.noticeIsError) AdminCoral else colors.accentLime,
            badgeContentColor = if (state.noticeIsError) colors.background else colors.accentInk,
            icon = if (state.noticeIsError) Icons.Rounded.PriorityHigh else Icons.Rounded.Check,
        )
    }
}

@Composable
private fun AdminTopBar(
    placeName: String,
    colors: OperationalColors,
    onChangeMode: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VaiinillaMark(modifier = Modifier.size(34.dp))
            Column {
                Text(
                    placeName.ifBlank { "Vaiinilla" },
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = colors.textPrimary,
                )
                Text("Administración", fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
        StaffAvatarMenu(
            initials = "AD",
            colors = colors,
            themeMode = themeMode,
            onThemeChange =
                themeChanger?.let { change ->
                    { mode ->
                        haptics.selection()
                        change(mode)
                    }
                },
            onChangeMode = {
                haptics.impact()
                onChangeMode()
            },
        )
    }
}

@Composable
private fun AdminTabBar(
    selected: AdminTab,
    colors: OperationalColors,
    onSelect: (AdminTab) -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
                .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AdminTab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) colors.textPrimary else Color.Transparent)
                        .physicalPress {
                            if (!active) haptics.selection()
                            onSelect(tab)
                        }.semantics {
                            role = Role.Tab
                            this.selected = active
                        }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    tab.icon,
                    contentDescription = null,
                    tint = if (active) colors.background else colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    tab.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) colors.background else colors.textSecondary,
                )
            }
        }
    }
}

internal val AdminCoral = Color(0xFFE5645A)
