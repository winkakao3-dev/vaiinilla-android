package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.parentmode.ChildrenState
import com.vaiinilla.app.ui.parentmode.FakeGuardianship
import com.vaiinilla.app.ui.parentmode.ParentFixtures
import com.vaiinilla.app.ui.parentmode.WalletFamilySection
import com.vaiinilla.app.ui.parentmode.WalletSchoolEntry
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.DiscoveryScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screens.PickupJourneyScreen
import com.vaiinilla.app.ui.screens.ReceiptStickerScreen
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import com.vaiinilla.app.ui.screens.WalletScreen
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Cómo se ve cada caso además de sus datos: el ancho lo pone la clase; el tema y la letra, aquí. */
internal enum class BreakUiLook(
    val suffix: String,
) {
    NORMAL(""),
    NOCHE("-noche"),
    LETRA_2X("-letra2x"),
}

internal data class BreakUiCase(
    val screen: String,
    val data: BreakUiData,
    val look: BreakUiLook = BreakUiLook.NORMAL,
) {
    val fileName: String get() = "break-ui/$screen/${data.slug}${look.suffix}"

    override fun toString() = "$screen ${data.slug}${look.suffix}"
}

/**
 * break-ui: cada pantalla con datos de demo, el peor caso creíble, vacía, con uno solo y con muchos.
 * No corre en CI; se pide con `BREAK_UI=1 ./gradlew :app:testDevAlumnoDebugUnitTest --tests '*BreakUi*'
 * -Proborazzi.test.record=true` y deja las capturas en `src/test/roborazzi/break-ui/`.
 */
internal object BreakUiCases {
    private val all = BreakUiData.entries
    private val single = listOf(BreakUiData.DEMO, BreakUiData.PEOR, BreakUiData.UNO, BreakUiData.MUCHOS)

    val screens: Map<String, List<BreakUiData>> =
        linkedMapOf(
            "descubrir" to all,
            "menu" to all,
            "ficha" to listOf(BreakUiData.DEMO, BreakUiData.PEOR),
            "carrito" to all,
            "confirmacion" to single,
            "pedidos" to all,
            "pedido" to single,
            "recogida" to single,
            "ticket" to single,
            "cartera-tutor" to all,
            "cartera-alumno" to all,
            "caja" to all,
            "cocina" to all,
            "mesero" to all,
        )

    fun wide(): List<Array<Any>> =
        screens
            .flatMap { (screen, datas) ->
                datas.map { BreakUiCase(screen, it) } +
                    BreakUiCase(screen, BreakUiData.PEOR, BreakUiLook.NOCHE) +
                    BreakUiCase(screen, BreakUiData.PEOR, BreakUiLook.LETRA_2X)
            }.map { arrayOf(it) }

    fun narrow(): List<Array<Any>> = screens.keys.map { arrayOf(BreakUiCase(it, BreakUiData.PEOR)) }

    fun enabled() = System.getenv("BREAK_UI") == "1"

    private fun tracking(
        data: BreakUiData,
        selected: Boolean,
    ): OperationalUiState {
        val orders =
            if (selected) {
                listOf(
                    BreakUiFixtures.order(data, OrderState.PREPARING),
                )
            } else {
                BreakUiFixtures.orders(data)
            }
        return OperationalUiState(
            role = OperationalRole.CLIENT,
            orders = orders,
            selectedOrderId = if (selected) orders.first().summary.id else null,
        )
    }

    /** Lo que se prepara antes de dibujar: la lista de hijos se carga como en la app, por su repositorio. */
    fun prepare(case: BreakUiCase): @Composable () -> Unit {
        val data = case.data
        return when (case.screen) {
            "descubrir" -> {
                val state = DiscoveryUiState(establishments = BreakUiFixtures.establishments(data))
                (
                    {
                        DiscoveryScreen(
                            state = state,
                            onQueryChange = {},
                            onSpaceTokenChange = {},
                            onSelectEstablishment = {},
                            onResolveSpace = {},
                            onContinueSelected = {},
                            profileInitials = "MG",
                        )
                    }
                )
            }
            "menu", "ficha" -> {
                val base = BreakUiFixtures.catalogState(data)
                val state =
                    if (case.screen == "ficha") {
                        val product = base.catalog!!.products.first()
                        base.copy(
                            selectedProductId = product.id,
                            selectedOptionIds =
                                product.optionGroups
                                    .flatMap { group ->
                                        group.options.take(group.maximumSelections).map { it.id }
                                    }.toSet(),
                            selectedQuantity = if (data == BreakUiData.PEOR) 12 else 1,
                        )
                    } else {
                        base
                    }
                // El aviso del pedido en curso: con uno de un solo artículo se ve cómo cuenta.
                val activeOrder =
                    if (case.screen == "menu" &&
                        data != BreakUiData.VACIO
                    ) {
                        BreakUiFixtures.order(data, OrderState.PREPARING)
                    } else {
                        null
                    }
                (
                    {
                        ScreenshotWithStudentNav(
                            activeTab = StudentTab.MENU,
                            cartCount =
                                if (data ==
                                    BreakUiData.PEOR
                                ) {
                                    128
                                } else {
                                    0
                                },
                        ) {
                            CatalogScreen(
                                state = state,
                                activeOrder = activeOrder,
                                onRetry = {},
                                onSearchChange = {},
                                onCategorySelected = {},
                                onProductSelected = {},
                                onDismissProduct = {},
                                onToggleOption = { _, _ -> },
                                onClearOptionalGroup = {},
                                onQuantityChange = {},
                                onAddProduct = {},
                            )
                        }
                    }
                )
            }
            "carrito" -> {
                val state = BreakUiFixtures.cartState(data)
                (
                    {
                        ScreenshotWithStudentNav(
                            activeTab = StudentTab.CART,
                            cartCount = state.cartLines.sumOf { it.quantity },
                        ) {
                            CartScreen(
                                state = state,
                                onMenu = {},
                                onQuantityChange = { _, _ -> },
                                onNotesChange = {},
                                onDestinationChange = {},
                                onPaymentChange = {},
                                onConfirm = {},
                                walletBalance =
                                    BreakUiFixtures
                                        .wallet(data)
                                        .data
                                        ?.wallet
                                        ?.visibleBalance,
                            )
                        }
                    }
                )
            }
            "confirmacion" -> {
                val order = BreakUiFixtures.order(data)
                ({ OrderConfirmationScreen(order = order, onReturnToMenu = {}, screenshotPrinted = true) })
            }
            "pedidos", "pedido" -> {
                val tracking = tracking(data, selected = case.screen == "pedido")
                val orderState =
                    BreakUiFixtures.catalogState(
                        if (data ==
                            BreakUiData.DEMO
                        ) {
                            BreakUiData.DEMO
                        } else {
                            BreakUiData.PEOR
                        },
                    )
                (
                    {
                        ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS) {
                            StudentTrackingScreen(
                                state = tracking,
                                orderState = orderState,
                                onMenu = {},
                                onAssistant = {},
                                onWallet = {},
                                onCart = {},
                                onOpenCatalog = {},
                                onSelectOrder = {},
                            )
                        }
                    }
                )
            }
            "recogida" -> {
                val order = BreakUiFixtures.order(data, OrderState.READY)
                ({ PickupJourneyScreen(order = order, qrPayload = null, onBack = {}, onViewTracking = {}) })
            }
            "ticket" -> {
                val order = BreakUiFixtures.order(data, OrderState.DELIVERED)
                ({ ReceiptStickerScreen(order = order, onBack = {}) })
            }
            "cartera-tutor" -> {
                val children = ChildrenState(ParentFixtures.services(FakeGuardianship(BreakUiFixtures.children(data))))
                runBlocking { children.load() }
                val entry = if (children.children.isEmpty()) WalletSchoolEntry.UNKNOWN else WalletSchoolEntry.PARENT
                val wallet = BreakUiFixtures.wallet(data)
                (
                    {
                        ScreenshotWithStudentNav(activeTab = StudentTab.WALLET) {
                            WalletScreen(
                                remoteState = wallet,
                                userId = "u-tutor",
                                onMenu = {},
                                familySection = {
                                    WalletFamilySection(
                                        children = children,
                                        onOpen = {},
                                        onAdd = {},
                                        onConsent = {},
                                        entry = entry,
                                        onRole = {},
                                        onLinkCode = {},
                                    )
                                },
                            )
                        }
                    }
                )
            }
            "cartera-alumno" -> {
                val controls = BreakUiFixtures.caretaker(data)
                val wallet = BreakUiFixtures.wallet(data)
                (
                    {
                        ScreenshotWithStudentNav(activeTab = StudentTab.WALLET) {
                            WalletScreen(
                                remoteState = wallet,
                                userId = "u-alumno",
                                onMenu = {},
                                caretakerLine = controls?.caretakerLine,
                                caretakerDetail = controls?.walletDetail,
                            )
                        }
                    }
                )
            }
            "caja" -> {
                val state =
                    OperationalUiState(
                        role = OperationalRole.CASHIER,
                        orders = BreakUiFixtures.orders(data, muchos = 120),
                        cashSessionOpen = true,
                        lastRefreshOk = true,
                        catalog =
                            BreakUiFixtures.catalog(
                                if (data ==
                                    BreakUiData.DEMO
                                ) {
                                    BreakUiData.DEMO
                                } else {
                                    BreakUiData.PEOR
                                },
                            ),
                    )
                (
                    {
                        CashierOperationalScreen(
                            state = state,
                            onBack = {},
                            onOpenCashSession = {},
                            onCollect = { _, _, _, _ -> },
                            onChangeMode = {},
                        )
                    }
                )
            }
            "cocina" -> {
                val state =
                    OperationalUiState(
                        role = OperationalRole.KITCHEN,
                        orders = BreakUiFixtures.orders(data, muchos = 120),
                        lastRefreshOk = true,
                    )
                (
                    {
                        KitchenOperationalScreen(
                            state = state,
                            onBack = {},
                            onStart = { _, _ -> },
                            onReady = { _, _ -> },
                            onChangeMode = {},
                        )
                    }
                )
            }
            "mesero" -> {
                val state = BreakUiFixtures.waiterBoard(data)
                (
                    {
                        WaiterOperationalScreen(state = state, onBack = {
                        }, placeName = "Cafetería y Tienda Escolar del Instituto Tecnológico")
                    }
                )
            }
            else -> error("Pantalla sin caso: ${case.screen}")
        }
    }
}

internal abstract class BreakUiCapture(
    private val case: BreakUiCase,
    private val widthSuffix: String,
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun capture() {
        assumeTrue("break-ui solo corre con BREAK_UI=1", BreakUiCases.enabled())
        val content = BreakUiCases.prepare(case)
        // Las escenas tienen vida pequeña para siempre: el reloj se adelanta a mano hasta que todo asienta.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val mode = if (case.look == BreakUiLook.NOCHE) VaiinillaThemeMode.Dark else VaiinillaThemeMode.Light
            ScreenshotTheme(mode) {
                if (case.look == BreakUiLook.LETRA_2X) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, fontScale = 2f),
                    ) { content() }
                } else {
                    content()
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onRoot().captureRoboImage("${case.fileName}$widthSuffix.png")
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
internal class BreakUiScreenshotTest(
    case: BreakUiCase,
) : BreakUiCapture(case, "") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = BreakUiCases.wide()
    }
}

/** El peor caso en una pantalla muy alta: lo que queda abajo del pliegue también se revisa. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h2400dp-normal-long-notround-any-xhdpi", sdk = [33])
internal class BreakUiTallScreenshotTest(
    case: BreakUiCase,
) : BreakUiCapture(case, "-alto") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = BreakUiCases.narrow()
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-normal-notlong-notround-any-xhdpi", sdk = [33])
internal class BreakUiNarrowScreenshotTest(
    case: BreakUiCase,
) : BreakUiCapture(case, "-320") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = BreakUiCases.narrow()
    }
}
