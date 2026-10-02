package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.TestFixtureSource
import com.vaiinilla.app.data.catalog.FixtureCatalogRepository
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.fixture.FixtureSource
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableCallTaker
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.prefetchProductImages
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screens for the sales video (vaiinilla-reel, src/ventas): the current UI in dark mode,
 * all telling one story with the real Venecia menu (names and photos from the dev catalog):
 * order #128, Tacos de Cochinita Pibil, Mesa 4. Fixtures: video_catalog.json, video_order.json.
 * Record with: ./gradlew :app:recordRoborazziDevAlumnoDebug --tests "*VideoScreenshotTest"
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h830dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class VideoScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val source =
        object : FixtureSource {
            private val base = TestFixtureSource()

            override fun read(path: String): String =
                base.read(
                    when {
                        path.endsWith("catalog.json") -> "fixtures/video_catalog.json"
                        path.endsWith("created_order.json") -> "fixtures/video_order.json"
                        else -> path
                    },
                )
        }
    private val repository = FixtureCatalogRepository(source, ContractResponseParser())
    private val catalogState =
        OrderFlowUiState(
            loading = false,
            catalog = repository.getCatalog().getOrThrow(),
            operationalStatus = repository.getOperationalStatus().getOrThrow(),
        )

    @Before
    fun prefetchPhotos() {
        runBlocking { prefetchProductImages(catalogState.catalog!!.products.map { it.imageUrl }) }
    }

    private fun sampleOrder(
        state: OrderState,
        destination: OrderDestination = OrderDestination.IN_SPACE,
    ) = OrderContractJson().parseOrderDetail(source.read("fixtures/created_order.json")).let { order ->
        order.copy(
            summary =
                order.summary.copy(
                    state = state,
                    paymentMethod = PaymentMethod.CASH,
                    destination = destination,
                    space =
                        if (destination ==
                            OrderDestination.IN_SPACE
                        ) {
                            OrderSpace(id = 4, name = "Mesa 4", type = "mesa")
                        } else {
                            null
                        },
                ),
        )
    }

    private fun set(content: @Composable () -> Unit) {
        composeTestRule.setContent { ScreenshotTheme(mode = VaiinillaThemeMode.Dark, content = content) }
        composeTestRule.waitForIdle()
    }

    private fun shot(name: String) {
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("video/$name.png")
    }

    private fun firstProductState() = catalogState to catalogState.catalog!!.products.first()

    private fun tableOrder(state: OrderState) = sampleOrder(state)

    private fun call(status: CallStatus): TableCall =
        TableCall(
            id = "call-4",
            space = TableSpace(id = 4, name = "Mesa 4", type = "mesa"),
            orderId = null,
            reason = CallReason.UTENSILIOS,
            status = status,
            clientName = "Ana",
            takenBy = if (status == CallStatus.EN_CAMINO) TableCallTaker(userId = "u-2", name = "Luis") else null,
            createdAt =
                java.time.Instant
                    .now()
                    .minusSeconds(8)
                    .toString(),
            takenAt = null,
            closedAt = null,
            version = 1,
        )

    @Composable
    private fun Catalog(selected: Boolean) {
        val (state, tacos) = firstProductState()
        ScreenshotWithStudentNav(activeTab = StudentTab.MENU, cartCount = 0) {
            CatalogScreen(
                state = if (selected) state.copy(selectedProductId = tacos.id) else state,
                onRetry = {},
                onSearchChange = {},
                onCategorySelected = {},
                onProductSelected = {},
                onDismissProduct = {},
                onToggleOption = { _, _ -> },
                onClearOptionalGroup = {},
                onQuantityChange = {},
                onAddProduct = {},
                onOpenCart = {},
            )
        }
    }

    @Test
    fun menu() {
        set { Catalog(selected = false) }
        shot("menu")
    }

    @Test
    fun producto() {
        set { Catalog(selected = true) }
        shot("producto")
    }

    @Test
    fun carrito() {
        val (state, tacos) = firstProductState()
        val cart =
            state.copy(
                cartLines = listOf(CartLine(product = tacos, quantity = 1, selectedOptionIds = emptySet())),
                checkoutPayment = PaymentMethod.CASH,
                checkoutDestination = OrderDestination.IN_SPACE,
                selectedSpaceId = 4,
            )
        set {
            ScreenshotWithStudentNav(activeTab = StudentTab.CART, cartCount = 1) {
                CartScreen(
                    state = cart,
                    onMenu = {},
                    onQuantityChange = { _, _ -> },
                    onNotesChange = {},
                    onDestinationChange = {},
                    onPaymentChange = {},
                    onConfirm = {},
                )
            }
        }
        shot("carrito")
    }

    @Composable
    private fun Tracking(current: TableCall?) {
        val order = tableOrder(OrderState.PREPARING)
        ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS, cartCount = 0) {
            StudentTrackingScreen(
                state = ScreenshotFixtures.trackingState(order, selected = false),
                orderState = catalogState,
                onMenu = {},
                onAssistant = {},
                onWallet = {},
                onCart = {},
                onOpenCatalog = {},
                onSelectOrder = {},
                canCallWaiter = { true },
                onCurrentWaiterCall = { Result.success(current) },
                onCallWaiter = { _, _, _ -> Result.success(call(CallStatus.PENDIENTE)) },
                onCancelWaiter = { Result.success(it) },
            )
        }
    }

    @Test
    fun pedido() {
        set { Tracking(current = null) }
        shot("pedido")
    }

    @Test
    fun llamar() {
        set { Tracking(current = null) }
        composeTestRule.onNodeWithText("Llamar al mesero").performClick()
        shot("llamar")
    }

    @Test
    fun llamando() {
        set { Tracking(current = call(CallStatus.PENDIENTE)) }
        shot("llamando")
    }

    @Test
    fun en_camino() {
        set { Tracking(current = call(CallStatus.EN_CAMINO)) }
        shot("en-camino")
    }

    @Test
    fun caja() {
        val pending = tableOrder(OrderState.PENDING_PAYMENT)
        val ready =
            sampleOrder(OrderState.READY, OrderDestination.TAKE_AWAY)
                .let { it.copy(summary = it.summary.copy(id = "demo-ready-order", folio = 127)) }
        set {
            CashierOperationalScreen(
                state =
                    OperationalUiState(
                        role = OperationalRole.CASHIER,
                        orders = listOf(pending, ready),
                        cashSessionOpen = true,
                        catalog = catalogState.catalog,
                    ),
                onBack = {},
                onOpenCashSession = {},
                onCollect = { _, _, _, _ -> },
                onChangeMode = {},
            )
        }
        shot("caja")
    }

    @Test
    fun cocina() {
        val paid = tableOrder(OrderState.PAID)
        val preparing =
            sampleOrder(OrderState.PREPARING, OrderDestination.TAKE_AWAY)
                .let { it.copy(summary = it.summary.copy(id = "demo-preparing-order", folio = 127)) }
        set {
            KitchenOperationalScreen(
                state = OperationalUiState(role = OperationalRole.KITCHEN, orders = listOf(paid, preparing)),
                onBack = {},
                onStart = { _, _ -> },
                onReady = { _, _ -> },
                onChangeMode = {},
            )
        }
        shot("cocina")
    }

    @Test
    fun mesero() {
        set {
            WaiterOperationalScreen(
                state = ScreenshotFixtures.waiterBoardState(),
                onBack = {},
                placeName = "Venecia",
            )
        }
        shot("mesero")
    }

    @Test
    fun mesero_mesa4() {
        set {
            WaiterOperationalScreen(
                state = ScreenshotFixtures.waiterBoardState(),
                onBack = {},
                placeName = "Venecia",
            )
        }
        composeTestRule.onAllNodesWithText("Mesa 4").onFirst().performClick()
        shot("mesero-mesa4")
    }
}
