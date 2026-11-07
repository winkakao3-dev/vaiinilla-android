package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaChallenge
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaFactor
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaOperation
import com.vaiinilla.app.domain.mode.MySchool
import com.vaiinilla.app.domain.model.LastOrderedPlace
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.repository.PendingRefund
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.operational.CloseCashUi
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.operational.PendingRefundsUiState
import com.vaiinilla.app.ui.order.PurchaseCelebrationKind
import com.vaiinilla.app.ui.screens.CashCloseContent
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CashierRefundsPage
import com.vaiinilla.app.ui.screens.DiscoveryScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.KitchenRejectContent
import com.vaiinilla.app.ui.screens.PurchaseSuccessScreen
import com.vaiinilla.app.ui.screens.StudentForgotPasswordScreen
import com.vaiinilla.app.ui.screens.StudentLoginScreen
import com.vaiinilla.app.ui.screens.StudentRegisterScreen
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import com.vaiinilla.app.ui.screens.WalletReloadCodeSheet
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** Acceso, personal y cliente dibujados con datos armados, para compararlos contra el lienzo de Doop. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp-normal-long-notround-any-xxhdpi", sdk = [33])
class DoopGaleriaPersonalClienteTest {
    @get:Rule
    val rule = createComposeRule()

    private fun draw(content: @Composable () -> Unit) {
        rule.setContent { ScreenshotTheme(mode = VaiinillaThemeMode.Dark, content = content) }
        settle()
    }

    private fun settle() {
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
    }

    private fun press(node: SemanticsNodeInteraction) {
        try {
            node.performClick()
        } catch (_: AssertionError) {
            node.performSemanticsAction(SemanticsActions.OnClick)
        }
        settle()
    }

    private fun tap(text: String) =
        press(
            rule.onAllNodes(hasText(text, substring = true) or hasContentDescription(text, substring = true)).onFirst(),
        )

    private fun scrollTap(text: String) {
        val target = hasText(text, substring = true) or hasContentDescription(text, substring = true)
        runCatching { rule.onAllNodes(hasScrollAction()).onLast().performScrollToNode(target) }
        settle()
        press(rule.onAllNodes(target).onFirst())
    }

    private fun shot(name: String) = captureScreenRoboImage("galeria/$name.png")

    @Composable
    private fun Login(state: StudentAuthUiState) =
        StudentLoginScreen(
            state = state,
            onBack = {},
            onEmailChange = {},
            onPasswordChange = {},
            onLogin = {},
            onForgotPassword = {},
            onRegister = {},
        )

    @Composable
    private fun Register(state: StudentAuthUiState) =
        StudentRegisterScreen(
            state = state,
            onBack = {},
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onTermsChange = {},
            onRegister = {},
            onLogin = {},
            onForgotPassword = {},
        )

    private val datos =
        StudentAuthUiState(
            email = "dani@correo.com",
            password = "pastel2026",
            passwordConfirm = "pastel2026",
            name = "Daniela Ruiz",
        )

    @Test fun ca02_login_error() {
        draw {
            Login(
                StudentAuthUiState(
                    email = "dani@correo.com",
                    password = "abcdefghij",
                    errorMessage = "Correo o contraseña incorrectos.",
                ),
            )
        }
        shot("cA02_login_error")
    }

    @Test fun ca05_mfa() {
        draw {
            Login(
                StudentAuthUiState(
                    mfaChallenge =
                        StudentAuthMfaChallenge(
                            "c1",
                            StudentAuthMfaOperation.LOGIN,
                            listOf(StudentAuthMfaFactor("f1", "Mi app de códigos")),
                        ),
                    mfaFactorUid = "f1",
                ),
            )
        }
        shot("cA05_mfa")
    }

    @Test fun ca07_enlace() {
        draw {
            StudentForgotPasswordScreen(
                state =
                    StudentAuthUiState(
                        email = "dani@correo.com",
                        passwordResetSent = true,
                        resetLockedUntilMs =
                            System.currentTimeMillis() + 42_000,
                    ),
                onBack = {},
                onEmailChange = {},
                onSendReset = {},
            )
        }
        shot("cA07_enlace")
    }

    @Test fun ca09_contrasena() {
        draw { Register(datos) }
        tap("Continuar")
        shot("cA09_contrasena")
    }

    @Test fun ca10_nombre() {
        draw { Register(datos) }
        tap("Continuar")
        tap("Continuar")
        shot("cA10_nombre")
    }

    @Test fun ca11_ultimo_paso() {
        draw { Register(datos) }
        tap("Continuar")
        tap("Continuar")
        tap("Continuar")
        shot("cA11_ultimo_paso")
    }

    @Test fun ca12_correo_registrado() {
        draw { Register(datos.copy(emailExistsSuggestion = true, errorMessage = "Este correo ya está registrado.")) }
        shot("cA12_correo_registrado")
    }

    @Test
    @Config(qualifiers = "w390dp-h1300dp-normal-long-notround-any-xxhdpi")
    fun cc05_devoluciones() {
        draw {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                CashierRefundsPage(
                    state =
                        PendingRefundsUiState(
                            refunds =
                                listOf(
                                    PendingRefund(
                                        "r1",
                                        "cancelacion",
                                        null,
                                        "o1",
                                        "efectivo",
                                        "85.00",
                                        "pendiente",
                                        "2026-11-03T15:12:00Z",
                                    ),
                                    PendingRefund(
                                        "r2",
                                        "reserva_cancelada",
                                        1,
                                        null,
                                        "efectivo",
                                        "80.00",
                                        "pendiente",
                                        "2026-11-02T15:12:00Z",
                                    ),
                                ),
                        ),
                    canConfirm = true,
                    cashOpen = true,
                    colors = colors,
                    orders = emptyList(),
                    onConfirm = { _, _, _ -> },
                    onBack = {},
                )
            }
        }
        shot("cC05_devoluciones")
    }

    @Test fun cc06_abrir_caja() {
        draw {
            CashierOperationalScreen(
                staffName = "Usagi Tsukino",
                state =
                    OperationalUiState(
                        role = OperationalRole.CASHIER,
                        cashSessionOpen = false,
                        suggestedCashFloat = "500.00",
                        catalog = ScreenshotFixtures.catalogLoadedState().catalog,
                    ),
                onBack = {},
                onOpenCashSession = {},
                onCollect = { _, _, _, _ -> },
                onChangeMode = {},
            )
        }
        shot("cC06_abrir_caja")
    }

    private fun kitchenState(): OperationalUiState {
        val a = ScreenshotFixtures.sampleOrder(state = OrderState.PAID)
        val b =
            ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING).let {
                it.copy(summary = it.summary.copy(id = "b", folio = 13))
            }
        val c =
            ScreenshotFixtures.sampleOrder(state = OrderState.READY).let {
                it.copy(summary = it.summary.copy(id = "c", folio = 14))
            }
        return OperationalUiState(role = OperationalRole.KITCHEN, orders = listOf(a, b, c))
    }

    @Composable
    private fun Kitchen() =
        KitchenOperationalScreen(state = kitchenState(), onBack = {}, onStart = {
            _,
            _,
            ->
        }, onMarkReady = { _, _ -> })

    @Test fun cn01_por_producto() {
        draw { Kitchen() }
        tap("Por producto")
        shot("cN01_por_producto")
    }

    @Test fun cn04_contar_billetes() {
        draw {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                CashCloseContent(
                    CloseCashUi(),
                    colors,
                    onDismiss = {},
                    onConfirm = {},
                    onOpenAccounts = {},
                    expectedCash = "2330.00",
                )
            }
        }
        scrollTap("Contar billete")
        shot("cN04_contar_billetes")
    }

    @Test fun ce03_mi_codigo() {
        draw { WalletReloadCodeSheet(true, "vaiinilla://recarga/abc", "Kao Arreola", "Pastelería Vaini", {}) }
        shot("cE03_mi_codigo")
    }

    private val pasteleria =
        PublicEstablishment(
            id = "a1111111-0000-4000-8000-0000000000b1",
            name = "Pastelería Vaini",
            slug = "pasteleria-vaini",
            clientIdLabel = "",
            clientIdRequired = false,
        )

    @Composable
    private fun Donde(state: DiscoveryUiState) =
        DiscoveryScreen(
            state = state,
            onQueryChange = {},
            onSpaceTokenChange = {},
            onSelectEstablishment = {},
            onResolveSpace = {},
            onContinueSelected = {},
            profileInitials = "KA",
        )

    @Test fun cDP1_al_abrir() {
        draw {
            Donde(
                DiscoveryUiState(
                    establishments = listOf(pasteleria),
                    mySchools = listOf(MySchool("e1", "Escuela de pruebas", "escuela")),
                    lastPlace =
                        LastOrderedPlace(
                            pasteleria,
                            "Pastel Michi con fresas",
                            0,
                            Instant.now().minusSeconds(86_400),
                        ),
                ),
            )
        }
        shot("cDP1_al_abrir")
    }

    @Test fun cDP5_cerca() {
        draw {
            Donde(
                DiscoveryUiState(
                    establishments = listOf(pasteleria),
                    nearbyOrigin = com.vaiinilla.app.domain.model.GeoCoordinates(28.63, -106.07),
                ),
            )
        }
        shot("cDP5_cerca")
    }
    @Test fun cDP4_pedir_ubicacion() {
        draw { Donde(DiscoveryUiState()) }
        scrollTap("Cerca de ti")
        shot("cDP4_pedir_ubicacion")
    }

    private fun onTheWay(): com.vaiinilla.app.domain.model.OrderDetail {
        val base =
            ScreenshotFixtures.sampleOrder(
                state = OrderState.ON_THE_WAY,
                destination = com.vaiinilla.app.domain.model.OrderDestination.HOME_DELIVERY,
            )
        return base.copy(
            homeDelivery =
                com.vaiinilla.app.domain.model.OrderHomeDelivery(
                    address =
                        com.vaiinilla.app.domain.model.DeliveryAddress(
                            address = "Aldama 1408, Centro",
                            phone = "6140000000",
                        ),
                    deposit = "41.00",
                    dueOnArrival = "41.00",
                    code = "4827",
                    courierName = "Luis",
                ),
        )
    }

    @Composable
    private fun Journey() =
        com.vaiinilla.app.ui.screens.OrderJourneyPage(
            order = onTheWay(),
            qrPayload = null,
            onBack = {},
            venue =
                com.vaiinilla.app.ui.order.JourneyVenue(
                    name = "Pastelería Vaini",
                    whatsappUrl = "https://wa.me/5216140000000",
                ),
        )

    private fun pastOrder(
        folio: Int,
        state: OrderState,
        dest: OrderDestination,
        ago: Long,
    ): com.vaiinilla.app.domain.model.OrderDetail {
        val base =
            com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
                .sampleOrder(state = state, destination = dest)
        return base.copy(
            summary =
                base.summary.copy(
                    id = "p$folio",
                    folio = folio,
                    createdAt = Instant.now().minusSeconds(ago).toString(),
                ),
        )
    }

    @Test fun cL10_pedidos() {
        val base =
            com.vaiinilla.app.ui.screenshot.ScreenshotFixtures.sampleOrder(
                state = OrderState.PREPARING,
                destination = OrderDestination.IN_SPACE,
            )
        val now =
            base.copy(
                summary =
                    base.summary.copy(
                        id = "now",
                        folio = 221,
                        space =
                            com.vaiinilla.app.domain.model
                                .OrderSpace(4, "Mesa 4", "mesa"),
                    ),
            )
        val orders =
            listOf(
                now,
                pastOrder(214, OrderState.DELIVERED, OrderDestination.TAKE_AWAY, 3_600),
                pastOrder(198, OrderState.CANCELED, OrderDestination.TAKE_AWAY, 90_000),
                pastOrder(187, OrderState.DELIVERED, OrderDestination.TAKE_AWAY, 300_000),
            )
        draw {
            com.vaiinilla.app.ui.screens.StudentTrackingScreen(
                state = OperationalUiState(role = OperationalRole.CLIENT, orders = orders),
                orderState =
                    com.vaiinilla.app.ui.order
                        .OrderFlowUiState(loading = false, catalog = null),
                onMenu = {},
                onAssistant = {},
                onWallet = {},
                onCart = {},
                onOpenCatalog = {},
                onSelectOrder = {},
            )
        }
        shot("cL10_pedidos")
    }

    @Test fun c16_seguimiento() {
        val base =
            com.vaiinilla.app.ui.screenshot.ScreenshotFixtures.sampleOrder(
                state = OrderState.AWAITING_ACCEPTANCE,
                destination = OrderDestination.HOME_DELIVERY,
            )
        val order =
            base.copy(
                summary = base.summary.copy(id = "d1", folio = 1, total = "1070.29", deliveryDate = "2026-11-07"),
                homeDelivery =
                    com.vaiinilla.app.domain.model.OrderHomeDelivery(
                        address =
                            com.vaiinilla.app.domain.model.DeliveryAddress(
                                address = "Aldama 1408, Centro",
                                phone = "6140000000",
                            ),
                        deposit = "535.15",
                        dueOnArrival = "535.14",
                        code = null,
                    ),
            )
        draw {
            com.vaiinilla.app.ui.screens.StudentTrackingScreen(
                state = OperationalUiState(role = OperationalRole.CLIENT, orders = listOf(order)),
                orderState =
                    com.vaiinilla.app.ui.order
                        .OrderFlowUiState(loading = false, catalog = null),
                onMenu = {},
                onAssistant = {},
                onWallet = {},
                onCart = {},
                onOpenCatalog = {},
                onSelectOrder = {},
            )
        }
        shot("c16_seguimiento")
    }

    @Test fun cL03_compartes() {
        draw {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
            ) {
                com.vaiinilla.app.ui.screens.SharedTableInvite(
                    state =
                        com.vaiinilla.app.ui.sharedtable.SharedTableUiState(
                            loaded = true,
                            aliasInput = "Kao",
                            othersAtTable = listOf("Luis", "Mariana", "Andrés"),
                        ),
                    spaceName = "Mesa 4",
                    spaceType = "mesa",
                    onAliasChange = {},
                    onJoin = {},
                    onOrderAlone = {},
                )
            }
        }
        shot("cL03_compartes")
    }

    @Test fun cL03b_tarjeta_pedidos() {
        draw {
            com.vaiinilla.app.ui.screens.StudentTrackingScreen(
                state = OperationalUiState(role = OperationalRole.CLIENT, orders = emptyList()),
                orderState =
                    com.vaiinilla.app.ui.order
                        .OrderFlowUiState(loading = false, catalog = null),
                onMenu = {},
                onAssistant = {},
                onWallet = {},
                onCart = {},
                onOpenCatalog = {},
                onSelectOrder = {},
                sharedTable = {
                    com.vaiinilla.app.ui.screens.SharedTableCard(
                        state =
                            com.vaiinilla.app.ui.sharedtable.SharedTableUiState(
                                loaded = true,
                                aliasInput = "Kao",
                                othersAtTable = listOf("Luis", "Mariana", "Andrés"),
                            ),
                        qrToken = "qr",
                        onAliasChange = {},
                        onJoin = {},
                        onOrderAlone = {},
                        onOpen = {},
                    )
                },
            )
        }
        shot("cL03b_tarjeta_pedidos")
    }

    @Test fun cL07_rentar() {
        val zone = java.time.ZoneId.of("America/Chihuahua")
        val today = java.time.LocalDate.now(zone)

        fun at(hour: Int) = today.atTime(hour, 0).atZone(zone).toInstant()
        val day =
            com.vaiinilla.app.domain.model.CourtDay(
                date = today.toString(),
                today = today.toString(),
                timeZone = zone.id,
                opensAt = at(8),
                closesAt = at(22),
                blockMinutes = 60,
                durations = listOf(60, 90, 120),
                daysAhead = 14,
                holdMinutes = 10,
                now = at(16),
                courts =
                    listOf(
                        com.vaiinilla.app.domain.model.CourtSchedule(
                            id = 7,
                            name = "Cancha 1",
                            pricePerHour = "350.00",
                            customerPricePerHour =
                                com.vaiinilla.app.domain.model.CustomerPrice(
                                    card = "350.00",
                                    cashOrBalance = "350.00",
                                ),
                            rentable = true,
                            busy = emptyList(),
                            profile =
                                com.vaiinilla.app.domain.model.CourtProfile(
                                    description = "",
                                    imageUrl = "fruta",
                                    features = listOf("Techada"),
                                ),
                        ),
                        com.vaiinilla.app.domain.model.CourtSchedule(
                            id = 8,
                            name = "Cancha 2",
                            pricePerHour = "300.00",
                            customerPricePerHour =
                                com.vaiinilla.app.domain.model.CustomerPrice(
                                    card = "300.00",
                                    cashOrBalance = "300.00",
                                ),
                            rentable = true,
                            busy = emptyList(),
                        ),
                    ),
            )
        draw {
            com.vaiinilla.app.ui.reservations.ReservationsScreen(
                state =
                    com.vaiinilla.app.ui.reservations
                        .ReservationsUiState(loading = false, day = day),
                venueName = "Club Pádel Norte",
                onBack = {},
                onSelectDate = {},
                onSelectCourt = {},
                onRentNow = {},
                onSelectStart = {},
                onSelectDuration = {},
                onReserve = {},
                onPay = {},
                onDismissPayment = {},
                onResumePayment = {},
                onCancel = {},
                onDismissMessage = {},
            )
        }
        shot("cL07_rentar")
    }

    @Test fun cD4_pagar_resto() {
        draw { Journey() }
        scrollTap("Prefiero pagar con tarjeta")
        shot("cD4_pagar_resto")
    }

    @Test fun cD7_que_paso() {
        draw { Journey() }
        scrollTap("Necesito ayuda")
        shot("cD7_que_paso")
    }

    @Test fun cDP2_primera_vez() {
        draw { Donde(DiscoveryUiState()) }
        shot("cDP2_primera_vez")
    }

    @Test fun cN05_buscar() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.READY)
        draw {
            CashierOperationalScreen(
                state =
                    OperationalUiState(
                        role = OperationalRole.CASHIER,
                        cashSessionOpen = true,
                        orders = listOf(order),
                        catalog = ScreenshotFixtures.catalogLoadedState().catalog,
                    ),
                onBack = {},
                onOpenCashSession = {},
                onCollect = { _, _, _, _ -> },
                onChangeMode = {},
            )
        }
        shot("cN05_buscar")
    }

    @Composable
    private fun Waiter() =
        WaiterOperationalScreen(
            state = ScreenshotFixtures.waiterBoardState(),
            onBack = {},
            placeName = "Pastelería Vaini",
        )

    @Test fun cW04b_te_llaman() {
        draw { Waiter() }
        tap("Mesa 4")
        shot("cW04_te_llaman")
    }

    private fun accountState(): com.vaiinilla.app.ui.operational.WaiterUiState {
        val board = ScreenshotFixtures.waiterBoardState()
        val table = board.tables.first { it.space.name == "Mesa 12" }
        return board.copy(
            spaceDetail =
                com.vaiinilla.app.domain.repository.SpaceSessionDetail(
                    availability =
                        com.vaiinilla.app.domain.repository.SpaceAvailability(
                            space = table.space,
                            state =
                                com.vaiinilla.app.domain.repository.SpaceAvailabilityState.entries.first {
                                    it !=
                                        com.vaiinilla.app.domain.repository.SpaceAvailabilityState.LIBRE
                                },
                            settled = false,
                            endsAt = null,
                            releasesAt = null,
                            remainingSeconds = null,
                            graceMinutes = 10,
                        ),
                    session = null,
                    account =
                        com.vaiinilla.app.domain.repository.SpaceAccount(
                            orders =
                                listOf(
                                    com.vaiinilla.app.domain.repository.AccountOrder(
                                        "o1",
                                        12,
                                        "listo",
                                        "214.00",
                                        "Ana",
                                        "Cheesecake, 2 lattes con avena",
                                        true,
                                        true,
                                    ),
                                    com.vaiinilla.app.domain.repository.AccountOrder(
                                        "o2",
                                        13,
                                        "listo",
                                        "98.00",
                                        "Luis",
                                        "Crepa de Nutella",
                                        true,
                                        true,
                                    ),
                                    com.vaiinilla.app.domain.repository.AccountOrder(
                                        "o3",
                                        17,
                                        "en_preparacion",
                                        "95.00",
                                        "Ana",
                                        "Pastel de fresas, rebanada",
                                        true,
                                        true,
                                    ),
                                    com.vaiinilla.app.domain.repository.AccountOrder(
                                        "o4",
                                        11,
                                        "entregado",
                                        "60.00",
                                        "Mesero",
                                        "2 cafés de olla",
                                        true,
                                        true,
                                    ),
                                ),
                            total = "467.00",
                            pending = "467.00",
                            paid = "0.00",
                            settled = false,
                        ),
                ),
        )
    }

    @Test fun cW02_mesa() {
        draw { WaiterOperationalScreen(state = accountState(), onBack = {}, placeName = "Pastelería Vaini") }
        tap("Mesa 12")
        shot("cW02_mesa")
    }

    @Test fun cW03_cobrar_cuenta() {
        draw { WaiterOperationalScreen(state = accountState(), onBack = {}, placeName = "Pastelería Vaini") }
        tap("Mesa 12")
        scrollTap("Cobrar cuenta")
        shot("cW03_cobrar_cuenta")
    }

    @Test fun cN07_mi_turno() {
        draw {
            val colors = rememberOperationalColors()
            Column(Modifier.fillMaxSize().background(colors.background)) {
                com.vaiinilla.app.ui.screens.StaffShiftContent(
                    modeLabel = "Mesero",
                    placeName = "Pastelería Vaini",
                    today = java.time.LocalDate.of(2026, 11, 3),
                    colors = colors,
                )
            }
        }
        shot("cN07_mi_turno")
    }

    @Test fun cN11_cajon() {
        draw {
            val colors = rememberOperationalColors()
            Column(Modifier.fillMaxSize().background(colors.background)) {
                com.vaiinilla.app.ui.screens
                    .CashDrawerContent(expectedCash = "1430.00", colors = colors)
            }
        }
        shot("cN11_cajon")
    }

    @Test fun cK05b_pagina() {
        draw {
            val colors = rememberOperationalColors()
            val today = java.time.LocalDate.of(2026, 11, 3)
            val tomorrow =
                java.time.LocalDate
                    .of(2026, 11, 4)
                    .toString()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                com.vaiinilla.app.ui.counter.ReservedOrdersPage(
                    state =
                        com.vaiinilla.app.ui.counter.ReservedOrdersUiState(
                            today = today,
                            selectedDate = tomorrow,
                            days =
                                mapOf(
                                    tomorrow to
                                        com.vaiinilla.app.domain.counter.ReservedDay(
                                            tomorrow,
                                            23,
                                            null,
                                            listOf(
                                                com.vaiinilla.app.domain.counter.ReservedDay.Line(
                                                    1,
                                                    "Sándwich de jamón",
                                                    12,
                                                    12,
                                                ),
                                                com.vaiinilla.app.domain.counter.ReservedDay
                                                    .Line(2, "Molletes", 7, 7),
                                                com.vaiinilla.app.domain.counter.ReservedDay.Line(
                                                    3,
                                                    "Fruta picada",
                                                    4,
                                                    4,
                                                ),
                                            ),
                                        ),
                                ),
                        ),
                    colors = colors,
                    onSelect = {},
                    onRetry = {},
                    onBack = {},
                )
            }
        }
        shot("cK05_para_manana")
    }

    @Test fun cC08_escanear() {
        draw { Waiter() }
        tap("Escanear QR")
        shot("cC08_escanear")
    }

    private fun kitchenTicket(
        state: OrderState,
        vararg lines: Triple<String, Int, com.vaiinilla.app.domain.model.PreparationStation>,
    ) = ScreenshotFixtures.sampleOrder(state = state).let { base ->
        val item = base.items.first()
        base.copy(
            summary = base.summary.copy(id = "t", folio = 12),
            items =
                lines.mapIndexed {
                    i,
                    (name, qty, station),
                    ->
                    item.copy(
                        id = i + 1,
                        productName = name,
                        quantity = qty,
                        preparationStation = station,
                        options = emptyList(),
                        notes = "",
                    )
                },
        )
    }

    @Test fun cN02_palomear() {
        val order =
            kitchenTicket(
                OrderState.PREPARING,
                Triple("Cheesecake de maracuyá", 1, com.vaiinilla.app.domain.model.PreparationStation.KITCHEN),
                Triple("Latte con avena", 2, com.vaiinilla.app.domain.model.PreparationStation.KITCHEN),
                Triple("Crepa de Nutella", 1, com.vaiinilla.app.domain.model.PreparationStation.KITCHEN),
            )
        draw {
            KitchenOperationalScreen(
                state =
                    OperationalUiState(
                        role = OperationalRole.KITCHEN,
                        orders = listOf(order),
                    ),
                onBack = {
                },
                onStart = {
                    _,
                    _,
                    ->
                },
                onMarkReady = { _, _ -> },
            )
        }
        tap("Preparando")
        tap("Cheesecake de maracuyá")
        tap("Latte con avena")
        shot("cN02_palomear")
    }

    @Test fun cN09_solo_lo_suyo() {
        val order =
            kitchenTicket(
                OrderState.PAID,
                Triple("Crepa de cajeta", 2, com.vaiinilla.app.domain.model.PreparationStation.KITCHEN),
                Triple("Coca-Cola", 2, com.vaiinilla.app.domain.model.PreparationStation.CASHIER),
                Triple("Agua de jamaica", 1, com.vaiinilla.app.domain.model.PreparationStation.CASHIER),
            )
        draw {
            KitchenOperationalScreen(
                state =
                    OperationalUiState(
                        role = OperationalRole.KITCHEN,
                        orders = listOf(order),
                    ),
                onBack = {
                },
                onStart = {
                    _,
                    _,
                    ->
                },
                onMarkReady = { _, _ -> },
            )
        }
        shot("cN09_solo_lo_suyo")
    }

    @Test fun cDP3_buscar() {
        draw {
            Donde(DiscoveryUiState(query = "pa", establishments = listOf(pasteleria)))
        }
        shot("cDP3_buscar")
    }

    @Test fun cK02b_preparando() {
        draw { Kitchen() }
        tap("Preparando")
        shot("cK02_preparando")
    }

    @Test fun cK05_para_manana() {
        val tomorrow =
            java.time.LocalDate
                .now()
                .plusDays(1)
                .toString()
        draw {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                com.vaiinilla.app.ui.counter.ReservedOrdersSheet(
                    state =
                        com.vaiinilla.app.ui.counter.ReservedOrdersUiState(
                            isSchool = true,
                            selectedDate = tomorrow,
                            days =
                                mapOf(
                                    tomorrow to
                                        com.vaiinilla.app.domain.counter.ReservedDay(
                                            tomorrow,
                                            23,
                                            null,
                                            listOf(
                                                com.vaiinilla.app.domain.counter.ReservedDay
                                                    .Line(1, "Sándwich de jamón", 12, 12),
                                                com.vaiinilla.app.domain.counter.ReservedDay
                                                    .Line(2, "Molletes", 7, 7),
                                            ),
                                        ),
                                ),
                        ),
                    colors = colors,
                    onSelect = {},
                    onRetry = {},
                    onDismiss = {},
                )
            }
        }
        shot("cK05_para_manana")
    }

    private fun homeCart() =
        ScreenshotFixtures.cartState(destination = OrderDestination.HOME_DELIVERY).let {
            it.copy(
                operationalStatus =
                    it.operationalStatus?.copy(
                        homeDelivery =
                            com.vaiinilla.app.domain.model.HomeDeliveryOffer(
                                "50.00",
                                "200.00",
                                "10",
                                true,
                                null,
                            ),
                    ),
            )
        }

    @Test
    @Config(qualifiers = "w390dp-h1300dp-normal-long-notround-any-xxhdpi")
    fun cD9_domicilio_mapa() {
        val state = homeCart()
        draw {
            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxSize().background(rememberOperationalColors().background),
            ) {
                com.vaiinilla.app.ui.screens.HomeDeliverySection(
                    state,
                    com.vaiinilla.app.ui.screens
                        .HomeCheckoutActions(),
                )
            }
        }
        shot("cD9_domicilio_mapa")
    }

    @Test fun cD10_elige_mapa() {
        val state = homeCart()
        draw {
            com.vaiinilla.app.ui.screens.HomeMapPicker(
                state,
                com.vaiinilla.app.domain.model
                    .HomeDeliveryOffer("50.00", "200.00", "10", true, null),
                com.vaiinilla.app.ui.screens
                    .HomeCheckoutActions(),
                {},
            )
        }
        shot("cD10_elige_mapa")
    }

    private fun depositOrder(state: OrderState) =
        com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
            .sampleOrder(
                state = state,
                paymentMethod = PaymentMethod.STRIPE,
                destination = OrderDestination.HOME_DELIVERY,
            ).let { base ->
                base.copy(
                    summary =
                        base.summary.copy(
                            id = "d1",
                            folio = 1,
                            total = "1070.29",
                            deliveryDate = "2026-11-07",
                            deliverySlot = "10:00-12:00",
                        ),
                    homeDelivery =
                        com.vaiinilla.app.domain.model.OrderHomeDelivery(
                            address =
                                com.vaiinilla.app.domain.model.DeliveryAddress(
                                    address = "Aldama 1408, Centro",
                                    phone = "6140000000",
                                ),
                            deposit = "535.15",
                            dueOnArrival = "535.14",
                            code = null,
                        ),
                )
            }

    @Test fun c14_procesando() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                com.vaiinilla.app.ui.screens.StripePaymentPendingScreen(
                    order = depositOrder(OrderState.PENDING_PAYMENT),
                    phase = com.vaiinilla.app.ui.order.StripePaymentPhase.PROCESSING_CONFIRMATION,
                    message = null,
                    retrying = false,
                    onRetry = {},
                    onRefresh = {},
                    onReturnToCart = {},
                    onViewOrders = {},
                )
            }
        }
        repeat(12) { rule.mainClock.advanceTimeBy(100) }
        rule.onRoot().captureRoboImage("galeria/c14_procesando.png")
    }

    @Test fun c15c_compra_raiz() {
        draw {
            PurchaseSuccessScreen(
                order = depositOrder(OrderState.PAID),
                kind = PurchaseCelebrationKind.PAYMENT_CONFIRMED,
                onFollowOrder = {},
                onViewOrders = {},
                venue =
                    com.vaiinilla.app.ui.order
                        .JourneyVenue(name = "Mi negocio"),
            )
        }
        shot("c15_compra_confirmada")
    }

    @Test fun cN06b_por_atender() {
        draw {
            WaiterOperationalScreen(
                state = ScreenshotFixtures.waiterBoardState().copy(filterAttending = true),
                onBack = {},
                placeName = "Pastelería Vaini",
            )
        }
        shot("cN06_por_atender")
    }

    @Composable
    private fun Cashier(state: OperationalUiState) =
        CashierOperationalScreen(
            state = state,
            onBack = {},
            onOpenCashSession = {},
            onCollect = { _, _, _, _ -> },
            onChangeMode = {},
            onNewCounterOrder = {},
            onOpenAccounts = {},
        )

    private fun cashierState(): OperationalUiState {
        val pending = ScreenshotFixtures.sampleOrder(state = OrderState.PENDING_PAYMENT)
        val ready =
            ScreenshotFixtures.sampleOrder(state = OrderState.READY).let {
                it.copy(summary = it.summary.copy(id = "r", folio = 15))
            }
        return OperationalUiState(
            role = OperationalRole.CASHIER,
            orders = listOf(pending, ready),
            cashSessionOpen = true,
            catalog = ScreenshotFixtures.catalogLoadedState().catalog,
        )
    }

    @Test fun cC01_por_cobrar() {
        draw { Cashier(cashierState()) }
        shot("cC01_por_cobrar")
    }

    @Test fun cC02_cobrar() {
        val state = cashierState()
        draw { Cashier(state) }
        tap(
            "#" +
                state.orders
                    .first()
                    .summary.folio,
        )
        shot("cC02_cobrar")
    }

    @Test fun cC04_recargar() {
        draw { Cashier(cashierState()) }
        tap("Recargar saldo")
        shot("cC04_recargar")
    }

    @Test fun cC07_cerrar_caja() {
        draw {
            val colors = rememberOperationalColors()
            Column(Modifier.fillMaxSize().background(colors.background)) {
                CashCloseContent(
                    state = CloseCashUi(),
                    colors = colors,
                    onDismiss = {},
                    onConfirm = {},
                    onOpenAccounts = null,
                    expectedCash = "2330.00",
                    openingAmount = "500.00",
                )
            }
        }
        shot("cC07_cerrar_caja")
    }

    @Test fun cK03_rechazar() {
        draw {
            val colors = rememberOperationalColors()
            Column(Modifier.fillMaxSize().background(colors.background)) {
                KitchenRejectContent(folio = 14, colors = colors, onConfirm = {}, onCancel = {})
            }
        }
        shot("cK03_rechazar")
    }

    @Test fun cS01_menu_cuenta() {
        draw { Cashier(cashierState()) }
        tap("CJ")
        shot("cS01_menu_cuenta")
    }
}
