package com.vaiinilla.app.ui.screenshot

import com.vaiinilla.app.ui.screens.KitchenRejectContent
import com.vaiinilla.app.ui.screens.CashierRefundsPage
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaChallenge
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaFactor
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaOperation
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.mode.MySchool
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.repository.PendingRefund
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.operational.PendingRefundsUiState
import com.vaiinilla.app.ui.order.PurchaseCelebrationKind
import com.vaiinilla.app.ui.screens.CashBillCounter
import com.vaiinilla.app.ui.screens.CashCloseContent
import com.vaiinilla.app.ui.operational.CloseCashUi
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CashierRefundsSection
import com.vaiinilla.app.ui.screens.DiscoveryScreen
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.domain.model.LastOrderedPlace
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
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
        press(rule.onAllNodes(hasText(text, substring = true) or hasContentDescription(text, substring = true)).onFirst())

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
        draw { Login(StudentAuthUiState(email = "dani@correo.com", password = "abc", errorMessage = "Correo o contraseña incorrectos.")) }
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
                state = StudentAuthUiState(email = "dani@correo.com", passwordResetSent = true),
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

    @Test @Config(qualifiers = "w390dp-h1300dp-normal-long-notround-any-xxhdpi")
    fun cc05_devoluciones() {
        draw {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                CashierRefundsPage(
                    state =
                        PendingRefundsUiState(
                            refunds =
                                listOf(
                                    PendingRefund("r1", "cancelacion", null, "o1", "efectivo", "85.00", "pendiente", "2026-11-03T15:12:00Z"),
                                    PendingRefund("r2", "reserva_cancelada", 1, null, "efectivo", "80.00", "pendiente", "2026-11-02T15:12:00Z"),
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
        val b = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING).let { it.copy(summary = it.summary.copy(id = "b", folio = 13)) }
        val c = ScreenshotFixtures.sampleOrder(state = OrderState.READY).let { it.copy(summary = it.summary.copy(id = "c", folio = 14)) }
        return OperationalUiState(role = OperationalRole.KITCHEN, orders = listOf(a, b, c))
    }

    @Composable
    private fun Kitchen() = KitchenOperationalScreen(state = kitchenState(), onBack = {}, onStart = { _, _ -> }, onReady = { _, _ -> })


    @Test fun cn01_por_producto() {
        draw { Kitchen() }
        tap("Por producto")
        shot("cN01_por_producto")
    }

    @Test fun cn04_contar_billetes() {
        draw {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                CashCloseContent(CloseCashUi(), colors, onDismiss = {}, onConfirm = {}, onOpenAccounts = {}, expectedCash = "2330.00")
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
                    lastPlace = LastOrderedPlace(pasteleria, "Pastel Michi con fresas", 0, Instant.now().minusSeconds(86_400)),
                ),
            )
        }
        shot("cDP1_al_abrir")
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
        WaiterOperationalScreen(state = ScreenshotFixtures.waiterBoardState(), onBack = {}, placeName = "Pastelería Vaini")

    @Test fun cW04b_te_llaman() {
        draw { Waiter() }
        tap("Mesa 4")
        shot("cW04_te_llaman")
    }

    @Test fun cW02_mesa() {
        draw { Waiter() }
        tap("Mesa 12")
        shot("cW02_mesa")
    }

    @Test fun cC08_escanear() {
        draw { Waiter() }
        tap("Escanear QR")
        shot("cC08_escanear")
    }

    @Test fun cK02b_preparando() {
        draw { Kitchen() }
        tap("Preparando")
        shot("cK02_preparando")
    }



    @Test fun cK05_para_manana() {
        val tomorrow = java.time.LocalDate.now().plusDays(1).toString()
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
                        homeDelivery = com.vaiinilla.app.domain.model.HomeDeliveryOffer("50.00", "200.00", "10", true, null),
                    ),
            )
        }

    @Test @Config(qualifiers = "w390dp-h1300dp-normal-long-notround-any-xxhdpi")
    fun cD9_domicilio_mapa() {
        val state = homeCart()
        draw {
            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().background(rememberOperationalColors().background)) {
                com.vaiinilla.app.ui.screens.HomeDeliverySection(state, com.vaiinilla.app.ui.screens.HomeCheckoutActions())
            }
        }
        shot("cD9_domicilio_mapa")
    }

    @Test fun cD10_elige_mapa() {
        val state = homeCart()
        draw {
            com.vaiinilla.app.ui.screens.HomeMapPicker(
                state,
                com.vaiinilla.app.domain.model.HomeDeliveryOffer("50.00", "200.00", "10", true, null),
                com.vaiinilla.app.ui.screens.HomeCheckoutActions(),
                {},
            )
        }
        shot("cD10_elige_mapa")
    }

    @Test fun c15c_compra_raiz() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.PAID, paymentMethod = PaymentMethod.STRIPE, destination = OrderDestination.TAKE_AWAY)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                PurchaseSuccessScreen(order, PurchaseCelebrationKind.PAYMENT_CONFIRMED, {}, 600_000)
            }
        }
        repeat(30) {
            rule.mainClock.advanceTimeBy(100)
        }
        rule.onRoot().captureRoboImage("galeria/c15_compra_confirmada.png")
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
        val ready = ScreenshotFixtures.sampleOrder(state = OrderState.READY).let { it.copy(summary = it.summary.copy(id = "r", folio = 15)) }
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
        tap("#" + state.orders.first().summary.folio)
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
                KitchenRejectContent(folio = 14, colors = colors, onConfirm = {})
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