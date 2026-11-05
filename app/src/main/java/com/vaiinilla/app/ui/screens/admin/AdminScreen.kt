package com.vaiinilla.app.ui.screens.admin

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.ui.admin.AdminImage
import com.vaiinilla.app.ui.admin.AdminSpacesViewModel
import com.vaiinilla.app.ui.admin.AdminStaffViewModel
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.ToastPill
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

    /** Tomar un pedido para una mesa o para el mostrador, con el flujo del mesero. */
    TAKE_ORDER("Pedir", Icons.Outlined.AddCircleOutline),
    ORDERS("Pedidos", Icons.Outlined.ReceiptLong),
    BUSINESS("Negocio", Icons.Outlined.Storefront),
}

/** Las secciones de "Negocio", en el orden del panel web. */
enum class AdminSection(
    val label: String,
    private val baseDescription: String,
    val icon: ImageVector,
) {
    STORE("Flujo de mi tienda", "Pagos con tarjeta, directorio, logo, videos y cómo se pide", Icons.Outlined.Tune),
    SCHOOL(
        "Escuela",
        "Código para unirse, recarga con tarjeta, apartar y menú de cada día",
        Icons.Outlined.School,
    ),
    SPACES("Mesas y espacios", "QR y código de cada mesa, canchas y reservas", Icons.Outlined.TableRestaurant),
    STAFF("Personal", "Invitaciones y accesos del equipo", Icons.Outlined.Group),
    CASHBACK("Cashback y wallet", "La regla de cashback y la cartera de clientes", Icons.Outlined.Savings),
    CASH("Caja", "Abrir y cerrar la caja", Icons.Outlined.PointOfSale),
    ;

    /** En un boliche dice «pistas» ([Venue]). */
    val description: String
        get() = if (this == SPACES) "QR y código de cada mesa, ${Venue.courts} y reservas" else baseDescription
}

/** Lo que las secciones de "Negocio" necesitan: sus ViewModels y lo de Caja que vive en el resto de la app. */
class AdminSectionsHost(
    val staff: AdminStaffViewModel,
    val store: AdminStoreViewModel,
    val spaces: AdminSpacesViewModel,
    val cash: AdminCashActions,
    /** El tablero del mesero para Pedidos → Mesas; null si no hay con qué cargarlo y queda solo el historial. */
    val tablesBoard: AdminOrdersTables? = null,
    /** Negocio › Escuela; null la esconde. */
    val school: com.vaiinilla.app.ui.admin.school.AdminSchoolViewModel? = null,
    /** La pestaña Pedir; null si no hay con qué tomar pedidos. */
    val orderTaker: (@Composable (AdminHeader) -> Unit)? = null,
)

/** Lo que comparten las cabeceras de las pestañas: el negocio, sus imágenes y el botón de la cuenta. */
class AdminHeader(
    val placeName: String,
    val photoUrl: String?,
    val logoUrl: String?,
    val avatar: @Composable () -> Unit,
)

/** Lo que la pantalla puede pedirle al ViewModel; agrupado para no pasar veinte lambdas sueltas. */
class AdminActions(
    val onRefreshHome: () -> Unit,
    val onRefreshCatalog: () -> Unit,
    val onSelectPeriod: (com.vaiinilla.app.ui.admin.AdminPeriodPreset) -> Unit,
    val onSelectCustomPeriod: (com.vaiinilla.app.domain.admin.AdminPeriod) -> Unit,
    val onCloseWithoutCharging: (com.vaiinilla.app.domain.admin.AdminUnpaidAccount, String, (Boolean) -> Unit) -> Unit,
    val onSaveCategory: (Int?, com.vaiinilla.app.domain.admin.AdminCategoryInput, () -> Unit) -> Unit,
    val onSaveProduct: (Int?, com.vaiinilla.app.domain.admin.AdminProductInput, AdminImage?, () -> Unit) -> Unit,
    val onSetAvailable: (com.vaiinilla.app.domain.model.Product, Boolean) -> Unit,
    val onRemoveImage: (com.vaiinilla.app.domain.model.Product) -> Unit,
    val onSelectOrderFilter: (com.vaiinilla.app.ui.admin.AdminOrderFilter) -> Unit,
    val onRefreshOrders: () -> Unit,
    val onLoadMoreOrders: () -> Unit,
    val onSetArchived: (com.vaiinilla.app.domain.model.Product, Boolean) -> Unit = { _, _ -> },
    /** Repostería (CASA-09): la agenda y las acciones de un pedido a domicilio o encargo. */
    val onRefreshAgenda: () -> Unit = {},
    val onHomeAction: (
        com.vaiinilla.app.domain.model.OrderDetail,
        com.vaiinilla.app.domain.model.HomeOrderAction,
        (com.vaiinilla.app.domain.model.OrderDetail) -> Unit,
    ) -> Unit = { _, _, _ -> },
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
    /** Escuela solo si el negocio dijo que lo es (Flujo de mi tienda › Escuela). */
    isSchool: Boolean = false,
    /** Repostería: los productos llevan días de preparación y tope por día (CASA-04). */
    takesPreorders: Boolean = false,
    /** La foto y el logo del negocio (Flujo de mi tienda) para las cabeceras. */
    photoUrl: String? = null,
    logoUrl: String? = null,
    /** La bienvenida del admin nuevo (marco AS1); apagada en las capturas. */
    offerWelcome: Boolean = false,
) {
    val colors = rememberOperationalColors()
    var tab by rememberSaveable { mutableStateOf(AdminTab.HOME) }
    var section by rememberSaveable { mutableStateOf<AdminSection?>(null) }
    // El asistente de configuración (marcos AS2 y AS3) va encima de todo, como una sección.
    var assistantOpen by rememberSaveable { mutableStateOf(false) }
    val reduced = reducedMotion()
    val readOnly = restrictedMode == RestrictedMode.READ_ONLY
    val welcome = rememberStoreWelcome(scope)
    val setup = adminSetupSteps(state, sections.store.uiState.value, sections.staff.uiState.value)
    // El dueño o Vaiinilla pueden apagar el asistente de la tienda (asistente_modo «ninguno»).
    val assistantForStaff = sections.store.uiState.value.config?.assistantMode?.forStaff != false

    AdminDialogHost(colors) {
        if (assistantOpen) {
            com.vaiinilla.app.ui.components.ResizeForKeyboard()
            StoreAssistantScreen(sections.store, colors, readOnly) { assistantOpen = false }
            return@AdminDialogHost
        }
        // Bienvenida (marco AS1): la primera vez que entra a una tienda que todavía no está lista.
        if (offerWelcome &&
            !readOnly &&
            assistantForStaff &&
            !welcome.seen &&
            sections.store.uiState.value.config != null &&
            showsStoreSetup(setup)
        ) {
            StoreWelcome(
                placeName = placeName,
                colors = colors,
                onAssistant = {
                    welcome.markSeen()
                    assistantOpen = true
                },
                onManual = {
                    welcome.markSeen()
                    section = AdminSection.STORE
                },
                onLater = welcome::markSeen,
            )
            return@AdminDialogHost
        }
        val openSection = section
        if (openSection != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                AdminSectionContent(
                    openSection,
                    state,
                    sections,
                    scope,
                    colors,
                    readOnly,
                    onOpenAssistant = { assistantOpen = true },
                    assistantForStaff = assistantForStaff,
                ) { section = null }
            }
            return@AdminDialogHost
        }

        val header =
            AdminHeader(
                placeName = placeName.ifBlank { "Vaiinilla" },
                photoUrl = photoUrl,
                logoUrl = logoUrl,
                avatar = { AdminAvatar(colors = colors, onChangeMode = onChangeMode) },
            )
        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (restrictedMode != null && !(readOnly && tab == AdminTab.BUSINESS)) {
                    Box(Modifier.statusBarsPadding()) { RestrictedBanner(restrictedMode, colors) }
                }
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
                                header = header,
                                setup = setup,
                                onOpenOrders = { tab = AdminTab.ORDERS },
                                onOpenAssistant =
                                    if (readOnly || !assistantForStaff) null else ({ assistantOpen = true }),
                                onSeeAsCustomer = onChangeMode,
                                onOpenSection = { section = it },
                            )
                        AdminTab.MENU ->
                            AdminMenuTab(
                                state = state,
                                colors = colors,
                                readOnly = readOnly,
                                actions = actions,
                                header = header,
                                rentsSpaces = rentsSpaces,
                                cardFeeToCustomer = cardFeeToCustomer,
                                isSchool = isSchool,
                                takesPreorders = takesPreorders,
                            )
                        AdminTab.TAKE_ORDER ->
                            sections.orderTaker?.invoke(header)
                                ?: Box(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
                                    Text("No se pudo abrir la toma de pedidos.", color = colors.textSecondary)
                                }
                        AdminTab.ORDERS ->
                            AdminOrdersTab(
                                state = state,
                                colors = colors,
                                actions = actions,
                                header = header,
                                tables = sections.tablesBoard,
                                onTakeOrder = sections.orderTaker?.let { { tab = AdminTab.TAKE_ORDER } },
                            )
                        AdminTab.BUSINESS ->
                            AdminBusinessTab(
                                state = state,
                                colors = colors,
                                header = header,
                                sections =
                                    AdminSection.entries.filter {
                                        it != AdminSection.SCHOOL || (isSchool && sections.school != null)
                                    },
                                host = sections,
                                readOnly = readOnly,
                            ) { section = it }
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
}

/** "Para dejar lista tu tienda": lo que falta para que la tienda reciba pedidos, como en iOS. */
private fun adminSetupSteps(
    state: AdminUiState,
    store: com.vaiinilla.app.ui.admin.AdminStoreUiState,
    staff: com.vaiinilla.app.ui.admin.AdminStaffUiState,
): List<AdminSetupStep> =
    buildList {
        val config = store.config
        if (config != null) {
            add(
                AdminSetupStep(
                    "Logo y foto del negocio",
                    null,
                    done = config.logoUrl != null && config.imageUrl != null,
                    section = AdminSection.STORE,
                ),
            )
            config.homeDelivery?.let { delivery ->
                add(
                    AdminSetupStep(
                        "Entrega a domicilio",
                        if (delivery.enabled) {
                            delivery.radiusKm.takeIf { it.isNotBlank() }?.let { "$it km" }
                        } else {
                            "Opcional"
                        },
                        done = delivery.enabled,
                        section = AdminSection.STORE,
                        optional = true,
                    ),
                )
            }
        }
        if (store.stripeLoaded) {
            val stripeReady =
                com.vaiinilla.app.domain.admin
                    .stripeStep(store.stripe) ==
                    com.vaiinilla.app.domain.admin.StripeStep.READY
            add(
                AdminSetupStep(
                    "Pagos con tarjeta",
                    if (stripeReady) "Stripe" else "Opcional",
                    done = stripeReady,
                    section = AdminSection.STORE,
                    optional = true,
                ),
            )
        }
        state.catalog?.let { catalog ->
            val count = catalog.products.size
            add(AdminSetupStep("Productos en el menú", "$count", done = count > 0))
        }
        if (!staff.loading || staff.memberships.isNotEmpty()) {
            val kitchen = com.vaiinilla.app.domain.admin.StaffRole.KITCHEN
            val invited =
                staff.memberships.any { it.role == kitchen } ||
                    staff.invitations.any {
                        it.role == kitchen &&
                            it.state in
                            setOf(
                                com.vaiinilla.app.domain.admin.InvitationState.PENDING,
                                com.vaiinilla.app.domain.admin.InvitationState.ACCEPTED,
                            )
                    }
            add(AdminSetupStep("Cocina invitada", null, done = invited, section = AdminSection.STAFF))
        }
        state.live?.cashOpen?.let { open ->
            add(
                AdminSetupStep(
                    "Caja abierta",
                    state.cashOpeningAmount?.takeIf { open }?.let {
                        "Fondo ${com.vaiinilla.app.ui.components.moneyLabel(
                            it,
                        )}"
                    },
                    done = open,
                    section = AdminSection.CASH,
                ),
            )
        }
    }

/** La cuenta: tema y cambiar de modo. Va arriba a la derecha de cada cabecera. */
@Composable
private fun AdminAvatar(
    colors: OperationalColors,
    onChangeMode: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    StaffAvatarMenu(
        initials = "AD",
        modeLabel = "Administración",
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
    onOpenAssistant: () -> Unit,
    assistantForStaff: Boolean = true,
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
            AdminStoreSection(
                sections.store,
                colors,
                readOnly,
                onBack,
                onOpenAssistant = onOpenAssistant.takeUnless { readOnly || !assistantForStaff },
            )
        }
        AdminSection.SCHOOL ->
            sections.school?.let { school ->
                LaunchedEffect(scope) { school.onVisible(scope) }
                com.vaiinilla.app.ui.screens.admin.school.AdminSchoolSection(
                    viewModel = school,
                    products = state.catalog?.products,
                    colors = colors,
                    readOnly = readOnly,
                    onBack = onBack,
                )
            }
        AdminSection.SPACES -> {
            LaunchedEffect(scope) { sections.spaces.onVisible(scope) }
            AdminSpacesSection(sections.spaces, colors, readOnly, onBack, sections.tablesBoard?.tables.orEmpty())
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

/**
 * «Negocio» como el marco 01 del lienzo: la foto del negocio con NEGOCIO en grande, las cuatro secciones del día a
 * día en tarjetas y las largas (Flujo de mi tienda, Escuela) a todo lo ancho.
 */
@Composable
private fun AdminBusinessTab(
    state: AdminUiState,
    colors: OperationalColors,
    header: AdminHeader,
    sections: List<AdminSection>,
    host: AdminSectionsHost,
    readOnly: Boolean,
    onOpen: (AdminSection) -> Unit,
) {
    val store by host.store.uiState
    val spaces by host.spaces.uiState
    val detail: (AdminSection) -> String = { businessDetail(it, state, store, spaces, readOnly) }
    val tiles = BusinessTiles.filter { it in sections }
    val wide = listOf(AdminSection.STORE, AdminSection.SCHOOL).filter { it in sections }
    if (readOnly) {
        ReadOnlyBusiness(sections, colors, header, detail, onOpen)
        return
    }
    AdminHeroPage(
        cover = header.photoUrl,
        caption = "Administración · ${header.placeName}",
        title = "Negocio",
        colors = colors,
        compactTitle = header.placeName,
        compactLogo = header.logoUrl,
        trailing = { header.avatar() },
    ) {
        item(key = "tiles") {
            Column(Modifier.padding(top = 4.dp).adminRise(1), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tiles.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { item ->
                            BusinessTile(
                                section = item,
                                detail = detail(item),
                                locked = readOnly && item != AdminSection.CASH,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                            ) { onOpen(item) }
                        }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }
        }
        wide.forEach { item ->
            item(key = "wide-${item.name}") {
                BusinessWideRow(item, detail(item), readOnly, colors, Modifier.padding(top = 12.dp).adminRise(2)) {
                    onOpen(item)
                }
            }
        }
    }
}

/** Con el negocio suspendido (marco 38): el aviso y una sola lista, porque aquí solo se consulta. */
@Composable
private fun ReadOnlyBusiness(
    sections: List<AdminSection>,
    colors: OperationalColors,
    header: AdminHeader,
    detail: (AdminSection) -> String,
    onOpen: (AdminSection) -> Unit,
) {
    AdminPage(caption = "Administración", title = "Mi negocio", colors = colors, trailing = { header.avatar() }) {
        item(key = "aviso") {
            Box(Modifier.padding(top = 10.dp, bottom = 10.dp).adminRise(1)) {
                RestrictedBanner(RestrictedMode.READ_ONLY, colors, inset = false)
            }
        }
        item(key = "lista") {
            AdminPanel(colors, Modifier.adminRise(2), padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                val order =
                    listOf(
                        AdminSection.CASH,
                        AdminSection.STORE,
                        AdminSection.SPACES,
                        AdminSection.STAFF,
                        AdminSection.CASHBACK,
                        AdminSection.SCHOOL,
                    ).filter { it in sections }
                order.forEachIndexed { index, section ->
                    if (index > 0) AdminRowDivider(colors)
                    AdminRow(
                        title = section.label,
                        colors = colors,
                        detail = detail(section),
                        onClick = { onOpen(section) },
                        leading = { AdminIconBox(section.icon, colors) },
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = "Solo lectura",
                            tint = colors.textMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

private val BusinessTiles = listOf(AdminSection.CASH, AdminSection.SPACES, AdminSection.STAFF, AdminSection.CASHBACK)

private fun businessDetail(
    section: AdminSection,
    state: AdminUiState,
    store: com.vaiinilla.app.ui.admin.AdminStoreUiState,
    spaces: com.vaiinilla.app.ui.admin.AdminSpacesUiState,
    readOnly: Boolean,
): String =
    when {
        section == AdminSection.CASH ->
            when (state.live?.cashOpen) {
                true -> "Abierta"
                false -> "Cerrada"
                null -> "Abrir y cerrar"
            }
        readOnly -> "Solo lectura"
        section == AdminSection.STORE -> "Pagos, logo, entrega y cómo se pide"
        section == AdminSection.SPACES -> {
            val count = spaces.spaces?.size ?: 0
            if (count ==
                0
            ) {
                "QR, ${Venue.courts} y reservas"
            } else {
                (if (count == 1) "1 espacio" else "$count espacios") +
                    " · QR y reservas"
            }
        }
        section == AdminSection.STAFF -> {
            val pending = state.live?.pendingInvitations ?: 0
            when {
                pending <= 0 -> "Equipo y accesos"
                pending == 1 -> "1 invitación pendiente"
                else -> "$pending invitaciones pendientes"
            }
        }
        section == AdminSection.CASHBACK ->
            store.cashback?.takeIf { it.active }?.let { "${it.percent.removeSuffix(".00")}% en cada compra" }
                ?: "Regla y cartera"
        else -> "Unirse, recargas y menú de cada día"
    }

@Composable
private fun BusinessTile(
    section: AdminSection,
    detail: String,
    locked: Boolean,
    colors: OperationalColors,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier =
            modifier
                .heightIn(min = 128.dp)
                .physicalPress(onClick = onClick)
                .semantics(mergeDescendants = true) {}
                .graphicsLayer { alpha = if (locked) 0.6f else 1f }
                .clip(shape)
                .background(colors.cardBackground)
                .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            AdminIconBox(section.icon, colors)
            Spacer(Modifier.weight(1f))
            if (locked) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Solo lectura",
                    tint = colors.textMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(section.label, fontSize = 17.sp, fontWeight = FontWeight.Black, color = colors.textPrimary, maxLines = 2)
        AnimatedContent(
            targetState = detail,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tile-detail",
        ) {
            Text(
                it,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun BusinessWideRow(
    section: AdminSection,
    detail: String,
    locked: Boolean,
    colors: OperationalColors,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .physicalPress(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .graphicsLayer { alpha = if (locked) 0.6f else 1f }
            .clip(RoundedCornerShape(26.dp))
            .background(colors.cardBackground)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdminIconBox(section.icon, colors)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(section.label, fontSize = 17.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
            Text(detail, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
        }
        Icon(
            if (locked) Icons.Outlined.Lock else Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(if (locked) 14.dp else 22.dp),
        )
    }
}

/** Con el negocio suspendido solo se consulta, o solo se permite el cierre. */
@Composable
private fun RestrictedBanner(
    mode: RestrictedMode,
    colors: OperationalColors,
    inset: Boolean = true,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier =
            Modifier
                .then(if (inset) Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp) else Modifier)
                .fillMaxWidth()
                .clip(shape)
                .background(AdminCoral.copy(alpha = 0.1f))
                .border(1.dp, AdminCoral.copy(alpha = 0.4f), shape)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.coralText, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                if (mode == RestrictedMode.READ_ONLY) "Solo consulta" else "Cierre operativo",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
            )
            Text(
                if (mode == RestrictedMode.READ_ONLY) {
                    "Tu negocio está suspendido. Puedes ver tus datos y ventas, pero no cambiar nada ni recibir pedidos."
                } else {
                    "El negocio está suspendido: solo se permiten las acciones de cierre compatibles con la suspensión."
                },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
            )
        }
    }
}

internal val AdminCoral = Color(0xFFE5645A)

/** Si ya se vio la bienvenida de esta tienda; se guarda en el teléfono para no repetirla. */
internal class StoreWelcomeMemory(
    private val prefs: SharedPreferences,
    private val key: String,
) {
    var seen by mutableStateOf(prefs.getBoolean(key, false))
        private set

    fun markSeen() {
        seen = true
        prefs.edit().putBoolean(key, true).apply()
    }
}

@Composable
private fun rememberStoreWelcome(scope: String): StoreWelcomeMemory {
    val context = LocalContext.current
    return remember(scope) {
        val prefs = context.applicationContext.getSharedPreferences("vaiinilla_admin_bienvenida", Context.MODE_PRIVATE)
        StoreWelcomeMemory(prefs, "vista_$scope")
    }
}
