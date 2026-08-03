package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.screens.AssistantChatScreen
import com.vaiinilla.app.ui.screens.AssistantScreen
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.DemoGalleryScreen
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screens.ReceiptStickerScreen
import com.vaiinilla.app.ui.screens.RoleSelectorScreen
import com.vaiinilla.app.ui.screens.SplashScreen
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.WalletAddMoneyScreen
import com.vaiinilla.app.ui.screens.WalletScreen
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class AlumnoScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `01_splash`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                SplashScreen(onFinished = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("01_splash.png")
    }

    @Test
    fun `02_role_selector`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                RoleSelectorScreen(
                    testOnlyMode = true,
                    onTestOnlyModeChange = {},
                    onRoleSelected = {},
                    onOpenDemoGallery = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("02_role_selector.png")
    }

    @Test
    fun `03_catalog`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.MENU, cartCount = 0) {
                    CatalogScreen(
                        state = state,
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
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("03_catalog.png")
    }

    @Test
    fun `04_cart`() {
        val state = ScreenshotFixtures.cartState()
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.CART, cartCount = 3) {
                    CartScreen(
                        state = state,
                        onMenu = {},
                        onQuantityChange = { _, _ -> },
                        onNotesChange = {},
                        onDestinationChange = {},
                        onPaymentChange = {},
                        onConfirm = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("04_cart.png")
    }

    @Test
    fun `05_assistant_hub`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.ASSISTANT, cartCount = 0) {
                    AssistantScreen(
                        state = state,
                        onOpenChat = {},
                        onOpenProduct = {},
                        onMenu = {},
                        onOrders = {},
                        onWallet = {},
                        onCart = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("05_assistant_hub.png")
    }

    @Test
    fun `06_wallet`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.WALLET, cartCount = 0) {
                    WalletScreen(
                        state = state,
                        balance = 200,
                        onAddMoney = {},
                        onPaymentMethods = {},
                        onAccount = {},
                        onMenu = {},
                        onAssistant = {},
                        onOrders = {},
                        onCart = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("06_wallet.png")
    }

    @Test
    fun `07_catalog_empty_search`() {
        val state = ScreenshotFixtures.emptySearchState()
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.MENU, cartCount = 0) {
                    CatalogScreen(
                        state = state,
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
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("07_catalog_empty_search.png")
    }

    @Test
    fun `08_assistant_default`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.ASSISTANT, cartCount = 0) {
                    AssistantScreen(
                        state = state,
                        onOpenChat = {},
                        onOpenProduct = {},
                        onMenu = {},
                        onOrders = {},
                        onWallet = {},
                        onCart = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("08_assistant_default.png")
    }

    @Test
    fun `09_assistant_budget_chip`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.ASSISTANT, cartCount = 0) {
                    AssistantScreen(
                        state = state,
                        onOpenChat = {},
                        onOpenProduct = {},
                        onMenu = {},
                        onOrders = {},
                        onWallet = {},
                        onCart = {},
                        initialChip = "Menos de \$60",
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("09_assistant_budget_chip.png")
    }

    @Test
    fun `10_cart_empty`() {
        val state = ScreenshotFixtures.emptyCartState()
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.CART, cartCount = 0) {
                    CartScreen(
                        state = state,
                        onMenu = {},
                        onQuantityChange = { _, _ -> },
                        onNotesChange = {},
                        onDestinationChange = {},
                        onPaymentChange = {},
                        onConfirm = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("10_cart_empty.png")
    }

    @Test
    fun `11_cart_mesa_saldo`() {
        val state =
            ScreenshotFixtures.cartState(
                paymentMethod = PaymentMethod.BALANCE,
                destination = OrderDestination.IN_SPACE,
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.CART, cartCount = 3) {
                    CartScreen(
                        state = state,
                        onMenu = {},
                        onQuantityChange = { _, _ -> },
                        onNotesChange = {},
                        onDestinationChange = {},
                        onPaymentChange = {},
                        onConfirm = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("11_cart_mesa_saldo.png")
    }

    @Test
    fun `12_cart_tarjeta`() {
        val state = ScreenshotFixtures.cartState(paymentMethod = PaymentMethod.CARD)
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.CART, cartCount = 3) {
                    CartScreen(
                        state = state,
                        onMenu = {},
                        onQuantityChange = { _, _ -> },
                        onNotesChange = {},
                        onDestinationChange = {},
                        onPaymentChange = {},
                        onConfirm = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("12_cart_tarjeta.png")
    }

    @Test
    fun `13_confirm_cash`() {
        val order = ScreenshotFixtures.sampleOrder(paymentMethod = PaymentMethod.CASH)
        composeTestRule.setContent {
            ScreenshotTheme {
                OrderConfirmationScreen(
                    order = order,
                    onReturnToMenu = {},
                    screenshotPrinted = true,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("13_confirm_cash.png")
    }

    @Test
    fun `14_tracking_empty`() {
        val orderState = ScreenshotFixtures.catalogLoadedState()
        val trackingState = ScreenshotFixtures.emptyTrackingState()
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS, cartCount = 0) {
                    StudentTrackingScreen(
                        state = trackingState,
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
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("14_tracking_empty.png")
    }

    @Test
    fun `15_tracking_por_cobrar`() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.PENDING_PAYMENT)
        val orderState = ScreenshotFixtures.catalogLoadedState()
        val trackingState = ScreenshotFixtures.trackingState(order)
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS, cartCount = 0) {
                    StudentTrackingScreen(
                        state = trackingState,
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
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("15_tracking_por_cobrar.png")
    }

    @Test
    fun `16_tracking_preparando`() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING)
        val orderState = ScreenshotFixtures.catalogLoadedState()
        val trackingState = ScreenshotFixtures.trackingState(order)
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS, cartCount = 0) {
                    StudentTrackingScreen(
                        state = trackingState,
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
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("16_tracking_preparando.png")
    }

    @Test
    fun `17_wallet_add_money`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                WalletAddMoneyScreen(
                    walletState = ScreenshotFixtures.walletState(),
                    onBack = {},
                    onCreditBalance = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("17_wallet_add_money.png")
    }

    @Test
    fun `18_chat_welcome`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.ASSISTANT, cartCount = 0) {
                    AssistantChatScreen(
                        state = state,
                        onSendMessage = {},
                        onClearChat = {},
                        onClose = {},
                        onMenu = {},
                        onOrders = {},
                        onWallet = {},
                        onCart = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("18_chat_welcome.png")
    }

    @Test
    fun `19_catalog_dark`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                ScreenshotWithStudentNav(activeTab = StudentTab.MENU, cartCount = 0) {
                    CatalogScreen(
                        state = state,
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
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("19_catalog_dark.png")
    }

    @Test
    fun `20_sticker_receipt`() {
        val order = ScreenshotFixtures.sampleOrder()
        composeTestRule.setContent {
            ScreenshotTheme {
                ReceiptStickerScreen(
                    order = order,
                    onBack = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("20_sticker_receipt.png")
    }

    @Test
    fun `21_tracking_pagado`() {
        val order =
            ScreenshotFixtures.sampleOrder(
                state = OrderState.PAID,
                paymentMethod = PaymentMethod.BALANCE,
            )
        val orderState = ScreenshotFixtures.catalogLoadedState()
        val trackingState = ScreenshotFixtures.trackingState(order)
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS, cartCount = 0) {
                    StudentTrackingScreen(
                        state = trackingState,
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
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("21_tracking_pagado.png")
    }

    @Test
    fun `22_confirm_saldo`() {
        val order = ScreenshotFixtures.sampleOrder(paymentMethod = PaymentMethod.BALANCE)
        composeTestRule.setContent {
            ScreenshotTheme {
                OrderConfirmationScreen(
                    order = order,
                    onReturnToMenu = {},
                    screenshotPrinted = true,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("22_confirm_saldo.png")
    }

    @Test
    fun `23_demo_gallery`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                DemoGalleryScreen(
                    onBack = {},
                    onItemSelected = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("23_demo_gallery.png")
    }
}
