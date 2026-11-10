package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.ui.operational.CashierUnpaidUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Android recrea la pantalla al cambiar el tema o el tamaño de letra: el cobro a medias no se pierde. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CashierUnpaidAccountsRestoreUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mesa =
        AdminUnpaidAccount(
            spaceId = 4,
            spaceName = "Mesa 4",
            orders = 2,
            total = "300.00",
            hours = 1,
            abandoned = false,
        )

    @Test
    fun `la hoja de cobro sigue abierta con la propina y lo recibido cuando Android recrea la pantalla`() {
        val restoration = StateRestorationTester(composeTestRule)
        restoration.setContent {
            CashierUnpaidAccountsScreen(
                state = CashierUnpaidUiState(accounts = AdminUnpaidAccounts(listOf(mesa), "300.00", 0)),
                colors = rememberOperationalColors(),
                onBack = {},
                onRefresh = {},
                onCharge = { _, _, _, _ -> },
                onDismissCollected = {},
                onClearChargeError = {},
            )
        }
        composeTestRule.onAllNodesWithText("Efectivo").onFirst().performClick()
        composeTestRule.onAllNodesWithText("10%").onFirst().performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("500")
        composeTestRule.onAllNodesWithText("Cambio: $170").onFirst().assertExists()

        restoration.emulateSavedInstanceStateRestore()

        composeTestRule.onAllNodesWithText("Cobrar Mesa 4").onFirst().assertExists()
        composeTestRule.onAllNodesWithText("Cambio: $170").onFirst().assertExists()
    }
}
