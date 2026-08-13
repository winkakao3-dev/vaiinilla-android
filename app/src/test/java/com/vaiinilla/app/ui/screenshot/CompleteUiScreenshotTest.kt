package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.discovery.QrScannerDialog
import com.vaiinilla.app.ui.mode.AuthorizedAccessUiState
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.screens.AuthorizedModeScreen
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.InvitationAcceptanceScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screens.ReceiptStickerScreen
import com.vaiinilla.app.ui.screens.StudentAuthLandingScreen
import com.vaiinilla.app.ui.screens.StudentForgotPasswordScreen
import com.vaiinilla.app.ui.screens.StudentLoginScreen
import com.vaiinilla.app.ui.screens.StudentRegisterScreen
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.StudentVerifyEmailScreen
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import com.vaiinilla.app.ui.screens.WalletAccountScreen
import com.vaiinilla.app.ui.screens.WalletAddCardScreen
import com.vaiinilla.app.ui.screens.WalletAddMoneyScreen
import com.vaiinilla.app.ui.screens.WalletPaymentMethodsScreen
import com.vaiinilla.app.ui.wallet.WalletUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class CompleteUiScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val session =
        StudentAuthSession(
            uid = "demo-student",
            email = "ana@vaiinilla.test",
            displayName = "Ana López",
            emailVerified = true,
        )

    private val walletState = WalletUiState()

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme(content = content)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(name)
    }

    @Test
    fun `31_auth_landing`() {
        capture("31_auth_landing.png") {
            StudentAuthLandingScreen(
                state = StudentAuthUiState(),
                onBack = {},
                onRegister = {},
                onLogin = {},
            )
        }
    }

    @Test
    fun `32_auth_login`() {
        capture("32_auth_login.png") {
            StudentLoginScreen(
                state =
                    StudentAuthUiState(
                        email = session.email,
                        password = "secreto123",
                        contextualId = "A-1042",
                        clientIdLabel = "Matrícula",
                        clientIdRequired = true,
                    ),
                onBack = {},
                onEmailChange = {},
                onPasswordChange = {},
                onContextualIdChange = {},
                onLogin = {},
                onForgotPassword = {},
                onRegister = {},
            )
        }
    }

    @Test
    fun `33_auth_register`() {
        capture("33_auth_register.png") {
            StudentRegisterScreen(
                state =
                    StudentAuthUiState(
                        name = session.displayName,
                        email = session.email,
                        password = "secreto123",
                        contextualId = "A-1042",
                        termsAccepted = true,
                        clientIdLabel = "Matrícula",
                        clientIdRequired = true,
                    ),
                onBack = {},
                onNameChange = {},
                onEmailChange = {},
                onPasswordChange = {},
                onContextualIdChange = {},
                onTermsChange = {},
                onRegister = {},
                onLogin = {},
                onForgotPassword = {},
            )
        }
    }

    @Test
    fun `34_auth_verify_email`() {
        capture("34_auth_verify_email.png") {
            StudentVerifyEmailScreen(
                state =
                    StudentAuthUiState(
                        email = session.email,
                        session = session,
                        verificationSent = true,
                    ),
                onBack = {},
                onResend = {},
                onCheckVerified = {},
            )
        }
    }

    @Test
    fun `35_auth_forgot_password`() {
        capture("35_auth_forgot_password.png") {
            StudentForgotPasswordScreen(
                state = StudentAuthUiState(email = session.email),
                onBack = {},
                onEmailChange = {},
                onSendReset = {},
            )
        }
    }

    @Test
    fun `36_wallet_payment_methods`() {
        capture("36_wallet_payment_methods.png") {
            WalletPaymentMethodsScreen(
                walletState = walletState,
                onBack = {},
                onAddCard = {},
            )
        }
    }

    @Test
    fun `37_wallet_add_card`() {
        capture("37_wallet_add_card.png") {
            WalletAddCardScreen(
                walletState = walletState,
                onBack = {},
                onSaved = {},
            )
        }
    }

    @Test
    fun `38_wallet_account`() {
        capture("38_wallet_account.png") {
            WalletAccountScreen(
                onBack = {},
                displayName = "David",
                email = "david@vaiinilla.test",
                userId = "u-preview",
            )
        }
    }

    @Test
    fun `39_product_detail_sheet`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        val product = state.catalog!!.products.first()
        capture("39_product_detail_sheet.png") {
            ScreenshotWithStudentNav(activeTab = StudentTab.MENU) {
                CatalogScreen(
                    state = state.copy(selectedProductId = product.id),
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

    @Test
    fun `40_invitation_acceptance`() {
        capture("40_invitation_acceptance.png") {
            InvitationAcceptanceScreen(
                state =
                    AuthorizedAccessUiState(
                        session = session,
                        invitationToken = "vai27-valid-cashier",
                        invitation =
                            AuthorizedInvitation(
                                token = "vai27-valid-cashier",
                                establishmentId = "est-centro",
                                establishmentName = "Cafetería Centro",
                                invitedEmail = session.email,
                                role = OperationalRole.CASHIER,
                                expiresAt = Instant.parse("2026-08-04T18:00:00Z"),
                            ),
                    ),
                onBack = {},
                onAccept = {},
                onAuthenticate = {},
            )
        }
    }

    @Test
    fun `41_authorized_modes`() {
        val modes =
            listOf(
                AuthorizedMode(OperationalRole.CASHIER, "est-centro", "Cafetería Centro", "membership-caja"),
                AuthorizedMode(OperationalRole.KITCHEN, "est-centro", "Cafetería Centro", "membership-cocina"),
                AuthorizedMode(OperationalRole.WAITER, "est-centro", "Cafetería Centro", "membership-mesero"),
            )
        capture("41_authorized_modes.png") {
            AuthorizedModeScreen(
                state =
                    AuthorizedAccessUiState(
                        session = session,
                        modes = modes,
                        activeContext =
                            AuthorizedModeContext(
                                role = OperationalRole.CASHIER,
                                establishmentId = "est-centro",
                                establishmentName = "Cafetería Centro",
                                membershipId = "membership-caja",
                                accessToken = "mock-access-token",
                            ),
                    ),
                onBack = {},
                onSelectMode = {},
                onReturnToClient = {},
            )
        }
    }

    @Test
    fun `42_cashier`() {
        val pending = ScreenshotFixtures.sampleOrder(state = OrderState.PENDING_PAYMENT)
        val ready =
            ScreenshotFixtures
                .sampleOrder(state = OrderState.READY)
                .copy(summary = pending.summary.copy(id = "demo-ready-order"))
        capture("42_cashier.png") {
            CashierOperationalScreen(
                state =
                    OperationalUiState(
                        role = OperationalRole.CASHIER,
                        orders = listOf(pending, ready),
                        cashSessionOpen = false,
                    ),
                onBack = {},
                onOpenCashSession = {},
                onCollect = { _, _, _ -> },
                onDeliver = { _, _ -> },
                onChangeMode = {},
            )
        }
    }

    @Test
    fun `43_kitchen`() {
        val paid = ScreenshotFixtures.sampleOrder(state = OrderState.PAID)
        val preparing =
            ScreenshotFixtures
                .sampleOrder(state = OrderState.PREPARING)
                .copy(summary = paid.summary.copy(id = "demo-preparing-order"))
        capture("43_kitchen.png") {
            KitchenOperationalScreen(
                state =
                    OperationalUiState(
                        role = OperationalRole.KITCHEN,
                        orders = listOf(paid, preparing),
                    ),
                onBack = {},
                onStart = { _, _ -> },
                onReady = { _, _ -> },
                onChangeMode = {},
            )
        }
    }

    @Test
    fun `44_waiter`() {
        val ready =
            ScreenshotFixtures.sampleOrder(
                state = OrderState.READY,
                destination = OrderDestination.IN_SPACE,
            )
        capture("44_waiter.png") {
            WaiterOperationalScreen(
                state = OperationalUiState(role = OperationalRole.WAITER, orders = listOf(ready)),
                onBack = {},
                onDeliver = { _, _ -> },
                onChangeMode = {},
            )
        }
    }

    @Test
    fun `45_qr_scanner`() {
        capture("45_qr_scanner.png") {
            QrScannerDialog(
                onClose = {},
                onPayload = {},
            )
        }
    }

    @Test
    fun `46_catalog_active_order`() {
        capture("46_catalog_active_order.png") {
            ScreenshotWithStudentNav(activeTab = StudentTab.MENU) {
                CatalogScreen(
                    state = ScreenshotFixtures.catalogLoadedState(),
                    activeOrder = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING),
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

    @Test
    fun `47_confirm_card`() {
        capture("47_confirm_card.png") {
            OrderConfirmationScreen(
                order = ScreenshotFixtures.sampleOrder(paymentMethod = PaymentMethod.CARD, state = OrderState.PAID),
                onReturnToMenu = {},
                screenshotPrinted = true,
            )
        }
    }

    @Test
    fun `48_tracking_ready`() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.READY)
        capture("48_tracking_ready.png") {
            ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS) {
                StudentTrackingScreen(
                    state = ScreenshotFixtures.trackingState(order),
                    orderState = ScreenshotFixtures.catalogLoadedState(),
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

    @Test
    fun `49_tracking_delivered`() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.DELIVERED)
        capture("49_tracking_delivered.png") {
            ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS) {
                StudentTrackingScreen(
                    state = ScreenshotFixtures.trackingState(order),
                    orderState = ScreenshotFixtures.catalogLoadedState(),
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

    @Test
    fun `50_wallet_add_money_spei`() {
        capture("50_wallet_add_money_spei.png") {
            WalletAddMoneyScreen(
                walletState = walletState,
                initialMethod = "spei",
                onBack = {},
                onCreditBalance = {},
            )
        }
    }

    @Test
    fun `51_sticker_core`() {
        capture("51_sticker_core.png") {
            ReceiptStickerScreen(ScreenshotFixtures.sampleOrder(), onBack = {}, initialStyleIndex = 1)
        }
    }

    @Test
    fun `52_sticker_limited`() {
        capture("52_sticker_limited.png") {
            ReceiptStickerScreen(ScreenshotFixtures.sampleOrder(), onBack = {}, initialStyleIndex = 2)
        }
    }

    @Test
    fun `53_sticker_breakfast`() {
        capture("53_sticker_breakfast.png") {
            ReceiptStickerScreen(ScreenshotFixtures.sampleOrder(), onBack = {}, initialStyleIndex = 3)
        }
    }

    @Test
    fun `54_sticker_qr_live`() {
        capture("54_sticker_qr_live.png") {
            ReceiptStickerScreen(ScreenshotFixtures.sampleOrder(), onBack = {}, initialStyleIndex = 4)
        }
    }

    @Test
    fun `55_sticker_thermal`() {
        capture("55_sticker_thermal.png") {
            ReceiptStickerScreen(ScreenshotFixtures.sampleOrder(), onBack = {}, initialStyleIndex = 5)
        }
    }
}
