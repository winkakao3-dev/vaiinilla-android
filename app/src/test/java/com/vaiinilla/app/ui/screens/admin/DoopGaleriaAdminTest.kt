package com.vaiinilla.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.domain.admin.AdminCount
import com.vaiinilla.app.domain.admin.AdminDailySales
import com.vaiinilla.app.domain.admin.AdminLiveCounts
import com.vaiinilla.app.domain.admin.AdminMetric
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.admin.AdminWalletSummary
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.HomeDeliverySettings
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.PreorderSettings
import com.vaiinilla.app.domain.admin.ReservationSchedule
import com.vaiinilla.app.domain.admin.StaffInvitation
import com.vaiinilla.app.domain.admin.StaffMembership
import com.vaiinilla.app.domain.admin.StoreConfig
import com.vaiinilla.app.domain.admin.StoreVideo
import com.vaiinilla.app.domain.admin.StripeSetup
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.DeliveryAddress
import com.vaiinilla.app.domain.model.HomeDeliveryOffer
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderHomeDelivery
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderWindow
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.AdminSpacesRepository
import com.vaiinilla.app.domain.repository.AdminStaffRepository
import com.vaiinilla.app.domain.repository.AdminStoreRepository
import com.vaiinilla.app.domain.repository.CashLastClose
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.ReservationRepository
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.domain.school.GuardianRequest
import com.vaiinilla.app.domain.school.MenuCalendar
import com.vaiinilla.app.domain.school.NoServiceDay
import com.vaiinilla.app.domain.school.RefundMethod
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.SchoolAdminRepository
import com.vaiinilla.app.domain.school.SchoolGuardianship
import com.vaiinilla.app.domain.school.SchoolSettings
import com.vaiinilla.app.domain.school.SchoolStudentRequest
import com.vaiinilla.app.domain.school.StudentRequestKind
import com.vaiinilla.app.ui.admin.AdminSpacesUiState
import com.vaiinilla.app.ui.admin.AdminSpacesViewModel
import com.vaiinilla.app.ui.admin.AdminStaffUiState
import com.vaiinilla.app.ui.admin.AdminStaffViewModel
import com.vaiinilla.app.ui.admin.AdminStoreUiState
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.admin.school.AdminSchoolUiState
import com.vaiinilla.app.ui.admin.school.AdminSchoolViewModel
import com.vaiinilla.app.ui.operational.WaiterUiState
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.admin.school.AdminSchoolSection
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

/**
 * El administrador dibujado con datos armados para compararlo contra el lienzo de Doop.
 * Los repositorios se quedan suspendidos para siempre: la pantalla solo ve el estado que se le pone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp-normal-long-notround-any-xxhdpi", sdk = [33])
class DoopGaleriaAdminTest {
    @get:Rule
    val rule = createComposeRule()

    private inline fun <reified R : Any> hung(): R =
        Proxy.newProxyInstance(R::class.java.classLoader, arrayOf(R::class.java)) { _, method, args ->
            when {
                method.name == "toString" -> "hung"
                method.name == "hashCode" -> 0
                method.name == "equals" -> false
                args?.lastOrNull() is Continuation<*> -> COROUTINE_SUSPENDED
                else ->
                    java.util.concurrent
                        .CountDownLatch(1)
                        .await()
            }
        } as R

    @Suppress("UNCHECKED_CAST")
    private fun <S> Any.put(state: S) {
        val field = javaClass.getDeclaredField("_uiState")
        field.isAccessible = true
        (field.get(this) as MutableState<S>).value = state
    }

    private val actions =
        AdminActions(
            onRefreshHome = {},
            onRefreshCatalog = {},
            onSelectPeriod = {},
            onSelectCustomPeriod = {},
            onCloseWithoutCharging = { _, _, _ -> },
            onSaveCategory = { _, _, _ -> },
            onSaveProduct = { _, _, _, _ -> },
            onSetAvailable = { _, _ -> },
            onRemoveImage = {},
            onSelectOrderFilter = {},
            onRefreshOrders = {},
            onLoadMoreOrders = {},
        )

    private val config =
        StoreConfig(
            typeWire = "cafeteria",
            deliveryRequiresQr = true,
            allowsPayAtEnd = false,
            allowsBalance = true,
            releaseGraceMinutes = 10,
            orderWindows = listOf(OrderWindow("08:00", "20:00")),
            videos = listOf(StoreVideo("Así preparamos", "https://example.com/v")),
            visibleInDirectory = true,
            storeUrl = "https://vaiinilla.app/p/pasteleria",
            imageUrl = null,
            logoUrl = null,
            homeDelivery = HomeDeliverySettings(true, "5", "35.00", "150.00", true, "CLABE 0123"),
            preorders = PreorderSettings(7, "18:00", true, 50, 120),
        )

    private fun storeVm() =
        AdminStoreViewModel(hung<AdminStoreRepository>(), hung<CatalogRepository>()).apply {
            put(
                AdminStoreUiState(
                    config = config,
                    stripe = StripeSetup(true, true, true, "complete", false),
                    stripeLoaded = true,
                    cashback = CashbackRule("Regla principal", "5", null, null, null, null, null, active = true),
                    cashbackLoaded = true,
                    wallet =
                        AdminWalletSummary(
                            "12400.00",
                            "8350.00",
                            "412.50",
                            "180.00",
                            2,
                            listOf(AdminMetric("Recargas", "12400.00", 31)),
                            84,
                            0,
                        ),
                ),
            )
        }

    private fun mesa(
        id: Int,
        name: String,
        type: String = "mesa",
        price: String? = null,
    ) = ManagedSpace(
        id,
        name,
        type,
        true,
        "https://vaiinilla.app/q/$id",
        "AB$id",
        price,
        null,
        null,
        listOf("Techada"),
        capacity = 4,
        minimumMinutes = if (price != null) 90 else null,
        sport = if (price != null) "Pádel" else null,
        surface = if (price != null) "Pasto sintético" else null,
    )

    private fun spacesVm(spaces: List<ManagedSpace>?) =
        AdminSpacesViewModel(hung<AdminSpacesRepository>(), hung<ReservationRepository>()).apply {
            put(
                AdminSpacesUiState(
                    spaces = spaces,
                    schedule = ReservationSchedule("08:00", "22:00", 7),
                ),
            )
        }

    private fun staffVm() =
        AdminStaffViewModel(hung<AdminStaffRepository>()).apply {
            put(
                AdminStaffUiState(
                    memberships =
                        listOf(
                            StaffMembership("m1", "Ana Torres", "ana@correo.mx", "cajero", "2026-10-01T10:00:00Z"),
                            StaffMembership("m2", "Luis Rangel", "luis@correo.mx", "mesero", "2026-10-02T10:00:00Z"),
                            StaffMembership("m3", "Marta Ruiz", "marta@correo.mx", "cocina", "2026-10-03T10:00:00Z"),
                        ),
                    invitations =
                        listOf(
                            StaffInvitation(
                                "i1",
                                "nuevo@correo.mx",
                                "cocina",
                                "pendiente",
                                "2026-11-10T10:00:00Z",
                                "2026-11-03T10:00:00Z",
                                null,
                            ),
                        ),
                ),
            )
        }

    private fun home(cashOpen: Boolean = true): AdminUiState {
        val catalog = ScreenshotFixtures.catalog()
        val a = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING)
        val b =
            ScreenshotFixtures
                .sampleOrder(
                    state = OrderState.READY,
                ).let { it.copy(summary = it.summary.copy(id = "b")) }
        return AdminUiState(
            live = AdminLiveCounts(1, cashOpen, 2, false),
            cashSessionId = if (cashOpen) "c1" else null,
            cashOpeningAmount = if (cashOpen) "500.00" else null,
            suggestedCashFloat = "500.00",
            cashOpenedAt = if (cashOpen) "2026-11-03T08:10:00Z" else null,
            cashLoaded = true,
            cashExpected = if (cashOpen) "1840.00" else null,
            cashLastClose = CashLastClose("1760.00", "2026-11-02T21:04:00Z", "500.00", "1760.00", closedByMe = true),
            cashOrders =
                if (cashOpen) {
                    listOf(
                        ScreenshotFixtures.sampleOrder(
                            OrderState.PREPARING,
                            PaymentMethod.CASH,
                            OrderDestination.IN_SPACE,
                            3,
                        ),
                        ScreenshotFixtures.sampleOrder(
                            OrderState.DELIVERED,
                            PaymentMethod.CASH,
                            OrderDestination.IN_SPACE,
                            1,
                        ),
                    )
                } else {
                    emptyList()
                },
            summary =
                AdminSummary(
                    AdminPeriod(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3)),
                    "4280.50",
                    37,
                    "115.69",
                    64,
                    "212.00",
                    "600.00",
                    2,
                    "98.40",
                    listOf(
                        AdminDailySales("2026-11-01", "3100.00", 28),
                        AdminDailySales("2026-11-02", "3900.00", 31),
                        AdminDailySales("2026-11-03", "4280.50", 37),
                    ),
                    listOf(
                        AdminMetric("Efectivo", "2100.00", 18),
                        AdminMetric("Tarjeta", "1680.50", 14),
                        AdminMetric("Saldo", "500.00", 5),
                    ),
                    listOf(AdminCount("Entregados", 30), AdminCount("Preparando", 5), AdminCount("Cancelados", 2)),
                    listOf(AdminCount("Pastel de fresas", 12, "1440.00"), AdminCount("Latte", 9, "405.00")),
                ),
            operationalStatus = ScreenshotFixtures.operationalStatus(),
            unpaid =
                AdminUnpaidAccounts(
                    listOf(
                        AdminUnpaidAccount(4, "Mesa 4", 2, "285.00", 1, false),
                        AdminUnpaidAccount(9, "Mesa 9", 1, "200.00", 26, true),
                    ),
                    "485.00",
                    1,
                ),
            catalog = catalog,
            orders = listOf(a, b),
        )
    }

    private fun host(state: WaiterUiState? = null) =
        AdminSectionsHost(
            staff = staffVm(),
            store = storeVm(),
            spaces = spacesVm(listOf(mesa(1, "Mesa 1"), mesa(2, "Cancha 1", "cancha", "350.00"))),
            cash = AdminCashActions({ _, _ -> }, { _, _ -> }),
            orderTaker =
                state?.let { s ->
                    { header ->
                        AdminTakeOrderTab(s, rememberOperationalColors(), header, {}, {}, { _, _ -> })
                    }
                },
        )

    private fun screen(
        state: AdminUiState,
        restricted: RestrictedMode? = null,
        waiter: WaiterUiState? = WaiterUiState(counterCatalog = ScreenshotFixtures.catalog()),
    ) {
        val sections = host(waiter)
        rule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                AdminScreen(
                    state = state,
                    placeName = "Pastelería Vaini",
                    restrictedMode = restricted,
                    actions = actions,
                    sections = sections,
                    scope = "s",
                    onChangeMode = {},
                )
            }
        }
        settle()
    }

    private fun section(content: @Composable (OperationalColors) -> Unit) {
        rule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                val colors = rememberOperationalColors()
                AdminDialogHost(colors) {
                    Box(Modifier.fillMaxSize().background(colors.background)) { content(colors) }
                }
            }
        }
        settle()
    }

    private fun settle() {
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
    }

    private fun matching(text: String) =
        rule.onAllNodes(
            hasText(text, substring = true) or hasContentDescription(text, substring = true),
        )

    private fun press(node: SemanticsNodeInteraction) {
        try {
            node.performClick()
        } catch (_: AssertionError) {
            node.performSemanticsAction(SemanticsActions.OnClick)
        }
        settle()
    }

    private fun tap(text: String) = press(matching(text).onFirst())

    private fun scrollToAndTap(text: String) {
        val target = hasText(text, substring = true) or hasContentDescription(text, substring = true)
        val scrollers = rule.onAllNodes(hasScrollAction())
        for (i in scrollers.fetchSemanticsNodes().indices) {
            if (runCatching { scrollers[i].performScrollToNode(target) }.isSuccess) break
        }
        settle()
        press(rule.onAllNodes(target).onLast())
    }

    private fun tapLast(text: String) = press(matching(text).onLast())

    private fun shot(name: String) = captureScreenRoboImage("galeria/$name.png")

    @Test fun a07_inicio() {
        screen(home())
        shot("a07_inicio")
    }

    @Test fun a21_periodo() {
        screen(home())
        rule.onNode(hasContentDescription("Periodo:", substring = true)).performClick()
        settle()
        shot("a21_periodo")
    }

    @Test fun a03_menu() {
        screen(home())
        tapLast("Menú")
        shot("a03_menu")
    }

    @Test fun a25_menu_vacio() {
        screen(
            home().copy(catalog = ScreenshotFixtures.catalog().copy(products = emptyList(), categories = emptyList())),
        )
        tapLast("Menú")
        shot("a25_menu_vacio")
    }

    @Test fun a27_nuevo_producto() {
        screen(home())
        tapLast("Menú")
        tapLast("Nuevo producto")
        shot("a27_nuevo_producto")
    }

    @Test fun a04_editar_producto() {
        val state = home()
        screen(state)
        tapLast("Menú")
        tap(
            state.catalog!!
                .products
                .first()
                .name,
        )
        shot("a04_editar_producto")
    }

    @Test fun a30_archivados() {
        val state = home()
        val products = state.catalog!!.products
        screen(state.copy(archivedProducts = products.take(2)))
        tapLast("Menú")
        tap("Archivados")
        shot("a30_archivados")
    }

    @Test fun a31_pedir() {
        screen(home())
        tapLast("Pedir")
        shot("a31_pedir")
    }

    @Test fun a33_pedir_mesa() {
        val waiter =
            WaiterUiState(
                counterCatalog = ScreenshotFixtures.catalog(),
                counterOrderSpace = TableSpace(4, "Mesa 4", "mesa"),
                allowsPayAtEnd = true,
            )
        screen(home(), waiter = waiter)
        tapLast("Pedir")
        shot("a33_pedir_mesa")
    }

    @Test fun a34_pedidos() {
        screen(home())
        tapLast("Pedidos")
        shot("a34_pedidos")
    }

    @Test fun a35_pedidos_vacio() {
        screen(home().copy(orders = emptyList()))
        tapLast("Pedidos")
        shot("a35_pedidos_vacio")
    }

    @Test fun a36_pedido_detalle() {
        val state = home()
        screen(state)
        tapLast("Pedidos")
        tap("#")
        shot("a36_pedido_detalle")
    }

    @Test fun a01_negocio() {
        screen(home())
        tapLast("Negocio")
        shot("a01_negocio")
    }

    @Test fun a38_solo_consulta() {
        screen(home(), restricted = RestrictedMode.READ_ONLY)
        tapLast("Negocio")
        shot("a38_solo_consulta")
    }

    @Test fun a39_caja_cerrada() {
        section { AdminCashSection(home(cashOpen = false), it, false, AdminCashActions({ _, _ -> }, { _, _ -> }), {}) }
        shot("a39_caja_cerrada")
    }

    @Test fun a06_abrir_caja() {
        section { AdminCashSection(home(cashOpen = false), it, false, AdminCashActions({ _, _ -> }, { _, _ -> }), {}) }
        tapLast("Abrir caja")
        shot("a06_abrir_caja")
    }

    @Test fun a40_caja_abierta() {
        section { AdminCashSection(home(), it, false, AdminCashActions({ _, _ -> }, { _, _ -> }), {}) }
        shot("a40_caja_abierta")
    }

    @Test fun a41_cerrar_caja() {
        section { AdminCashSection(home(), it, false, AdminCashActions({ _, _ -> }, { _, _ -> }), {}) }
        tapLast("Cerrar caja")
        shot("a41_cerrar_caja")
    }

    @Test fun a02a_flujo() {
        val vm = storeVm()
        section { AdminStoreSection(vm, it, false, {}) }
        shot("a02a_flujo")
    }

    @Test fun a42_pagos() {
        val vm = storeVm()
        section { AdminStoreSection(vm, it, false, {}) }
        tap("Pagos con tarjeta")
        shot("a42_pagos")
    }

    @Test
    @Config(qualifiers = "w390dp-h2400dp-normal-long-notround-any-xxhdpi")
    fun a43_como_funciona() {
        val vm = storeVm()
        section { AdminStoreSection(vm, it, false, {}) }
        tap("Cómo funciona tu tienda")
        shot("a43_como_funciona")
    }

    @Test fun a02b_domicilio() {
        val vm = storeVm()
        section { AdminStoreSection(vm, it, false, {}) }
        scrollToAndTap("Seguir: entrega a domicilio")
        shot("a02b_domicilio")
    }

    @Test fun a45_mesas() {
        val vm = spacesVm(listOf(mesa(1, "Mesa 1"), mesa(2, "Mesa 2"), mesa(3, "Cancha 1", "cancha", "350.00")))
        section { AdminSpacesSection(vm, it, false, {}) }
        shot("a45_mesas")
    }

    @Test fun a46_mesas_vacio() {
        val vm = spacesVm(emptyList())
        section { AdminSpacesSection(vm, it, false, {}) }
        shot("a46_mesas_vacio")
    }

    @Test
    @Config(qualifiers = "w390dp-h2400dp-normal-long-notround-any-xxhdpi")
    fun a48_cancha() {
        val vm = spacesVm(listOf(mesa(3, "Cancha 1", "cancha", "350.00")))
        section { AdminSpacesSection(vm, it, false, {}) }
        tap("Cancha 1")
        shot("a48_cancha")
    }

    @Test fun a49_mesa_qr() {
        val vm = spacesVm(listOf(mesa(1, "Mesa 1")))
        section { AdminSpacesSection(vm, it, false, {}) }
        tap("Mesa 1")
        shot("a49_mesa_qr")
    }

    @Test fun a50_reservas() {
        val vm = spacesVm(listOf(mesa(3, "Cancha 1", "cancha", "350.00")))
        section { AdminSpacesSection(vm, it, false, {}) }
        tap("Reservas")
        shot("a50_reservas")
    }

    @Test fun a52_personal() {
        val vm = staffVm()
        section { AdminStaffSection(vm, it, false, {}) }
        shot("a52_personal")
    }

    @Test fun a05_invitar() {
        val vm = staffVm()
        section { AdminStaffSection(vm, it, false, {}) }
        tap("Invitar")
        shot("a05_invitar")
    }

    @Test fun a05b_primera_invitacion() {
        val vm = AdminStaffViewModel(hung<AdminStaffRepository>()).apply { put(AdminStaffUiState(loading = false)) }
        section { AdminStaffSection(vm, it, false, {}) }
        shot("a05b_primera_invitacion")
    }

    @Test fun a53_cambiar_rol() {
        val vm = staffVm()
        section { AdminStaffSection(vm, it, false, {}) }
        tap("Ana Torres")
        shot("a53_cambiar_rol")
    }

    @Test
    @Config(qualifiers = "w390dp-h2400dp-normal-long-notround-any-xxhdpi")
    fun a55_cashback() {
        val vm = storeVm()
        section { AdminCashbackSection(vm, it, false, {}) }
        shot("a55_cashback")
    }

    private fun homeOrder(state: OrderState): OrderDetail {
        val base =
            ScreenshotFixtures.sampleOrder(
                state = state,
                paymentMethod = PaymentMethod.CASH,
                destination = OrderDestination.HOME_DELIVERY,
            )
        return base.copy(
            summary = base.summary.copy(destination = OrderDestination.HOME_DELIVERY, folio = 1, total = "535.14"),
            homeDelivery =
                OrderHomeDelivery(
                    address = DeliveryAddress(address = "Aldama 1408, Centro", phone = "6140000000"),
                    code = if (state == OrderState.ON_THE_WAY) "4827" else null,
                    deposit = "267.57",
                    dueOnArrival = if (state == OrderState.DELIVERED) "0.00" else "267.57",
                ),
        )
    }

    private fun orderSheet(state: OrderState) {
        screen(
            home().copy(orders = listOf(homeOrder(state)), operationalStatus = ScreenshotFixtures.operationalStatus()),
        )
        tapLast("Pedidos")
        tap("#1")
    }

    @Test fun a17_llega_encargo() {
        orderSheet(OrderState.AWAITING_ACCEPTANCE)
        shot("a17_llega_encargo")
    }

    @Test fun a18_entregar_codigo() {
        orderSheet(OrderState.ON_THE_WAY)
        shot("a18_entregar_codigo")
    }

    @Test fun a19_entregado_cobrado() {
        orderSheet(OrderState.DELIVERED)
        shot("a19_entregado_cobrado")
    }

    @Test fun a23_cerrar_sin_cobrar() {
        screen(home())
        scrollToAndTap("Cerrar sin cobrar")
        shot("a23_cerrar_sin_cobrar")
    }

    @Test
    @Config(qualifiers = "w390dp-h1900dp-normal-long-notround-any-xxhdpi")
    fun a22_estados_dias_cuentas() {
        screen(home())
        shot("a22_estados_dias_cuentas")
    }

    @Test fun a26_nueva_categoria() {
        screen(
            home().copy(catalog = ScreenshotFixtures.catalog().copy(products = emptyList(), categories = emptyList())),
        )
        tapLast("Menú")
        tapLast("Crear categoría")
        shot("a26_nueva_categoria")
    }

    @Test fun a28_opciones_alergenos() {
        val state = home()
        screen(state)
        tapLast("Menú")
        tap(
            state.catalog!!
                .products
                .first()
                .name,
        )
        scrollToAndTap("Opciones y extras")
        shot("a28_opciones_alergenos")
    }

    @Test fun a29_archivar() {
        val state = home()
        screen(state)
        tapLast("Menú")
        tap(
            state.catalog!!
                .products
                .first()
                .name,
        )
        scrollToAndTap("Opciones y extras")
        scrollToAndTap("Archivar")
        shot("a29_archivar")
    }

    @Test fun a32_opciones_producto() {
        val state = home()
        screen(state)
        tapLast("Pedir")
        val conOpciones = state.catalog!!.products.first { it.optionGroups.isNotEmpty() }
        tap(conOpciones.name)
        shot("a32_opciones_producto")
    }

    @Test fun a44_directorio() {
        val vm = storeVm()
        section { AdminStoreSection(vm, it, false, {}) }
        tap("Tu negocio")
        shot("a44_directorio")
    }

    @Test fun a47_nuevo_espacio() {
        val vm = spacesVm(emptyList())
        section { AdminSpacesSection(vm, it, false, {}) }
        tap("Crear varios")
        shot("a47_nuevo_espacio")
    }

    @Test fun a51_cobrar_renta() {
        val vm = spacesVm(listOf(mesa(3, "Cancha 1", "cancha", "350.00")))
        vm.put(
            vm.uiState.value.copy(
                reservations =
                    listOf(
                        Reservation(
                            "r1",
                            3,
                            "Cancha 1",
                            Instant.parse("2026-11-03T18:00:00Z"),
                            Instant.parse("2026-11-03T19:00:00Z"),
                            60,
                            "350.00",
                            null,
                            ReservationState.entries.first(),
                            null,
                            null,
                            "mostrador",
                            "Diego",
                            1,
                        ),
                    ),
            ),
        )
        section { AdminSpacesSection(vm, it, false, {}) }
        tap("Reservas")
        shot("a50b_reservas_con_renta")
        scrollToAndTap("Cobrar")
        shot("a51_cobrar_renta")
    }

    @Test fun a48b_editar_cancha() {
        val vm = spacesVm(listOf(mesa(3, "Cancha 1", "cancha", "350.00")))
        section { AdminSpacesSection(vm, it, false, {}) }
        tap("Cancha 1")
        tap("Editar Cancha 1")
        shot("a48b_editar_cancha")
    }

    @Test fun a17b_por_aceptar() {
        screen(
            home().copy(
                orders = listOf(homeOrder(OrderState.AWAITING_ACCEPTANCE)),
                operationalStatus =
                    ScreenshotFixtures.operationalStatus().copy(
                        homeDelivery = HomeDeliveryOffer("35.00", "150.00", "5", true, null),
                    ),
            ),
        )
        tapLast("Pedidos")
        shot("a17b_por_aceptar")
    }

    @Test fun a33b_a_la_cuenta() {
        val catalog = ScreenshotFixtures.catalog()
        val waiter =
            WaiterUiState(
                counterCatalog = catalog,
                counterOrderSpace = TableSpace(4, "Mesa 4", "mesa"),
                allowsPayAtEnd = true,
            )
        screen(home(), waiter = waiter)
        tapLast("Pedir")
        tap("Agregar " + catalog.products.first { it.optionGroups.isEmpty() }.name)
        tapLast("Mesa 4")
        shot("a33b_a_la_cuenta")
    }

    @Test fun a54_desactivar() {
        val vm = staffVm()
        section { AdminStaffSection(vm, it, false, {}) }
        tap("Ana Torres")
        scrollToAndTap("Desactivar acceso")
        shot("a54_desactivar")
    }

    @Test fun a54b_revocar() {
        val vm = staffVm()
        section { AdminStaffSection(vm, it, false, {}) }
        tap("nuevo@correo.mx")
        shot("a54b_revocar")
    }

    @Test fun a56_revisar_cambio() {
        val vm = storeVm()
        section { AdminCashbackSection(vm, it, false, {}) }
        tap("Subir porcentaje")
        scrollToAndTap("Revisar cambio")
        shot("a56_revisar_cambio")
    }

    private fun schoolVm(): AdminSchoolViewModel {
        val today = LocalDate.of(2026, 11, 3)
        val products = ScreenshotFixtures.catalog().products
        return AdminSchoolViewModel(hung<SchoolAdminRepository>()).apply {
            put(
                AdminSchoolUiState(
                    settings = SchoolSettings("ABC123", true, true, "50.00", "2000.00", 5, "09:00", "08:00"),
                    menu =
                        MenuCalendar(
                            today,
                            mapOf(
                                today to products.take(2).map { it.id },
                                today.plusDays(1) to products.take(3).map { it.id },
                            ),
                            listOf(NoServiceDay(today.plusDays(3), "Junta de maestros")),
                        ),
                    roster =
                        listOf(
                            RosterStudent("A-001", "Ana López", "3°B"),
                            RosterStudent("A-002", "Sofía Ruiz", "2°A"),
                        ),
                    requests =
                        listOf(
                            GuardianRequest(
                                "g1",
                                "Mariana Ruiz",
                                "m@correo.mx",
                                "Sofía Ruiz",
                                "A-002",
                                "2°A",
                                true,
                                true,
                                0,
                            ),
                        ),
                    guardianships =
                        listOf(
                            SchoolGuardianship(
                                "s1",
                                "Mariana Ruiz",
                                "m@correo.mx",
                                true,
                                "u1",
                                "Sofía Ruiz",
                                "A-002",
                                "2°A",
                            ),
                        ),
                    studentRequests =
                        listOf(
                            SchoolStudentRequest(
                                "q1",
                                StudentRequestKind.REFUND,
                                RefundMethod.CASH,
                                null,
                                "120.00",
                                "120.00",
                                "Mariana Ruiz",
                                "m@correo.mx",
                                "Sofía Ruiz",
                                "A-002",
                                "2°A",
                            ),
                            SchoolStudentRequest(
                                "q2",
                                StudentRequestKind.DATA_REMOVAL,
                                null,
                                "Cambio de escuela",
                                "0.00",
                                "0.00",
                                "Pablo Soto",
                                null,
                                "Ana López",
                                "A-001",
                                "3°B",
                            ),
                        ),
                ),
            )
        }
    }

    private fun school() {
        val vm = schoolVm()
        val products = ScreenshotFixtures.catalog().products
        section { AdminSchoolSection(vm, products, it, false, {}) }
    }

    @Test
    @Config(qualifiers = "w390dp-h4600dp-normal-long-notround-any-xxhdpi")
    fun a57_escuela_completa() {
        school()
        shot("a57_escuela_completa")
    }

    @Test fun a59_cerrar_dia() {
        school()
        val target = hasText("Cerrar este día", substring = true)
        val scrollers = rule.onAllNodes(hasScrollAction())
        for (i in scrollers.fetchSemanticsNodes().indices) {
            if (runCatching { scrollers[i].performScrollToNode(target) }.isSuccess) break
        }
        settle()
        // El campo del aviso toma el foco y su cursor parpadea: con el reloj automático nunca queda quieta.
        rule.mainClock.autoAdvance = false
        rule.onAllNodes(target).onLast().performClick()
        rule.mainClock.advanceTimeBy(1_500)
        shot("a59_cerrar_dia")
    }

    @Test fun a60_pegar_lista() {
        school()
        scrollToAndTap("Cambiar lista")
        shot("a60_pegar_lista")
    }

    @Test
    @Config(qualifiers = "w390dp-h1900dp-normal-long-notround-any-xxhdpi")
    fun a63_dia_sin_servicio() {
        school()
        val day = hasText("Vie") or hasContentDescription("Vie", substring = true)
        press(rule.onAllNodes(day).onFirst())
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        shot("a63_dia_sin_servicio")
    }
}
