package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** G09: con un nombre de negocio largo, las iniciales de Caja (y con ellas «Cerrar caja») siguen a la vista. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-normal-long-notround-any-xxhdpi", sdk = [33])
class CashierHeaderLongNameUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `con un nombre largo las iniciales siguen visibles`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierOperationalScreen(
                    state = OperationalUiState(role = OperationalRole.CASHIER, cashSessionOpen = true),
                    onBack = {},
                    onOpenCashSession = {},
                    onCollect = { _, _, _, _ -> },
                    onChangeMode = {},
                    placeName = "Cafetería Autoservicio Campus Norte Edificio B",
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("CJ").assertIsDisplayed()
    }
}
