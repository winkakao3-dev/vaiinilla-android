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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.vaiinilla.app.ui.admin.AdminSpacesViewModel
import com.vaiinilla.app.ui.admin.AdminStaffViewModel
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
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

/** Las pestañas del modo Administración; "Negocio" abre el resto de las secciones del panel web. */
enum class AdminTab(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Inicio", Icons.Outlined.Home),
    MENU("Menú", Icons.Outlined.RestaurantMenu),
    ORDERS("Pedidos", Icons.Outlined.ReceiptLong),
    BUSINESS("Negocio", Icons.Outlined.Storefront),
}

/** Las secciones de "Negocio", en el orden del panel web. */
enum class AdminSection(
    val label: String,
    val description: String,
    val icon: ImageVector,
) {
    STORE("Flujo de mi tienda", "Pagos con tarjeta, directorio, logo, videos y cómo se pide", Icons.Outlined.Tune),
    SPACES("Mesas y espacios", "QR y código de cada mesa, canchas y reservas", Icons.Outlined.TableRestaurant),
    STAFF("Personal", "Invitaciones y accesos del equipo", Icons.Outlined.Group),
    CASHBACK("Cashback y wallet", "La regla de cashback y la cartera de clientes", Icons.Outlined.Savings),
    CASH("Caja", "Abrir y cerrar la caja", Icons.Outlined.PointOfSale),
}

/** Lo que las secciones de "Negocio" necesitan: sus ViewModels y lo de Caja que vive en el resto de la app. */
class AdminSectionsHost(
    val staff: AdminStaffViewModel,
    val store: AdminStoreViewModel,
    val spaces: AdminSpacesViewModel,
    val cash: AdminCashActions,
    /** El tablero del mesero para Pedidos → Mesas; null si no hay con qué cargarlo y queda solo el historial. */
    val tablesBoard: (@Composable () -> Unit)? = null,
)

/** Lo que la pantalla puede pedirle al ViewModel; agrupado para no pasar veinte lambdas sueltas. */
class AdminActions(
    val onRefreshHome: () -> Unit,
    val onRefreshCatalog: () -> Unit,
    val onSelectPeriod: (com.vaiinilla.app.ui.admin.AdminPeriodPreset) -> Unit,
    val onSelectCustomPeriod: (com.vaiinilla.app.domain.admin.AdminPeriod) -> Unit,
    val onCloseWithoutCharging: (com.vaiinilla.app.domain.admin.AdminUnpaidAccount, String, () -> Unit) -> Unit,
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
    sections: AdminSectionsHost,
    scope: String,
    onChangeMode: () -> Unit,
    rentsSpaces: Boolean = false,
    cardFeeToCustomer: Boolean = false,
) {
    val colors = rememberOperationalColors()
    var tab by rememberSaveable { mutableStateOf(AdminTab.HOME) }
    var section by rememberSaveable { mutableStateOf<AdminSection?>(null) }
    val reduced = reducedMotion()
    val readOnly = restrictedMode == RestrictedMode.READ_ONLY

    val openSection = section
    if (openSection != null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AdminSectionContent(openSection, state, sections, scope, colors, readOnly) { section = null }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            AdminTopBar(placeName = placeName, colors = colors, onChangeMode = onChangeMode)
            if (restrictedMode != null) RestrictedBanner(restrictedMode, colors)
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
                    AdminTab.HOME ->
                        AdminHomeTab(
                            state = state,
                            colors = colors,
                            readOnly = readOnly,
                            actions = actions,
                        )
                    AdminTab.MENU ->
                        AdminMenuTab(
                            state = state,
                            colors = colors,
                            readOnly = readOnly,
                            actions = actions,
                            rentsSpaces = rentsSpaces,
                            cardFeeToCustomer = cardFeeToCustomer,
                        )
                    AdminTab.ORDERS ->
                        AdminOrdersTab(state = state, colors = colors, actions = actions, tables = sections.tablesBoard)
                    AdminTab.BUSINESS -> AdminBusinessTab(colors = colors) { section = it }
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

@Composable
private fun AdminSectionContent(
    section: AdminSection,
    state: AdminUiState,
    sections: AdminSectionsHost,
    scope: String,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    when (section) {
        AdminSection.STAFF -> {
            LaunchedEffect(scope) { sections.staff.onVisible(scope) }
            AdminStaffSection(sections.staff, colors, readOnly, onBack)
        }
        AdminSection.CASHBACK -> {
            LaunchedEffect(scope) { sections.store.onVisible(scope) }
            AdminCashbackSection(sections.store, colors, readOnly, onBack)
        }
        AdminSection.STORE -> {
            LaunchedEffect(scope) { sections.store.onVisible(scope) }
            AdminStoreSection(sections.store, colors, readOnly, onBack)
        }
        AdminSection.SPACES -> {
            LaunchedEffect(scope) { sections.spaces.onVisible(scope) }
            AdminSpacesSection(sections.spaces, colors, readOnly, onBack)
        }
        AdminSection.CASH ->
            AdminCashSection(
                state = state,
                colors = colors,
                readOnly = readOnly,
                actions = sections.cash,
                onBack = onBack,
            )
    }
}

/** "Negocio": el resto del panel web, una fila por sección. */
@Composable
private fun AdminBusinessTab(
    colors: OperationalColors,
    onOpen: (AdminSection) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(AdminSection.entries.size) { index ->
            val item = AdminSection.entries[index]
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .physicalPress { onOpen(item) }
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(item.icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(item.description, fontSize = 12.sp, color = colors.textSecondary)
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSecondary,
                )
            }
        }
    }
}

/** Con el negocio suspendido solo se consulta, o solo se permite el cierre. */
@Composable
private fun RestrictedBanner(
    mode: RestrictedMode,
    colors: OperationalColors,
) {
    Column(
        modifier =
            Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AdminCoral.copy(alpha = 0.14f))
                .padding(12.dp),
    ) {
        Text(
            if (mode == RestrictedMode.READ_ONLY) "Solo consulta" else "Cierre operativo",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Text(
            "El negocio está suspendido: solo se permiten las acciones de cierre compatibles con la suspensión.",
            fontSize = 12.sp,
            color = colors.textSecondary,
        )
    }
}

internal val AdminCoral = Color(0xFFE5645A)
