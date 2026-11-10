package com.vaiinilla.app.ui.screens.admin

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.data.inventory.InventorySupply
import com.vaiinilla.app.data.inventory.InventoryUnit
import com.vaiinilla.app.ui.admin.AdminInventoryViewModel
import com.vaiinilla.app.ui.admin.InventoryTab
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Android recrea la pantalla al cambiar el tema o el tamaño de letra: la hoja abierta de un insumo no se pierde. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AdminInventorySheetRestoreUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val harina = InventorySupply(1, "Harina", null, InventoryUnit.KILO, 10.0, 2.0, 30.0, 300.0, false, true, 0)

    @Test
    fun `la hoja de un insumo sigue abierta cuando Android recrea la pantalla`() {
        val viewModel = AdminInventoryViewModel(StubInventory(listOf(harina)))
        viewModel.onVisible("negocio-1")
        viewModel.selectTab(InventoryTab.STOCK)
        val restoration = StateRestorationTester(composeTestRule)
        restoration.setContent {
            AdminInventorySection(
                viewModel = viewModel,
                catalog = emptyList(),
                colors = rememberOperationalColors(),
                readOnly = false,
                onBack = {},
            )
        }
        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.supplies != null }
        composeTestRule.onAllNodesWithText("Harina").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Registrar merma").onFirst().assertExists()

        restoration.emulateSavedInstanceStateRestore()

        composeTestRule.onAllNodesWithText("Registrar merma").onFirst().assertExists()
    }
}
