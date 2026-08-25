package com.vaiinilla.app.ui.order

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class CatalogSearchCopyTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `catalog search uses generic product copy`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                CatalogScreen(
                    state = ScreenshotFixtures.catalogLoadedState(),
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
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Buscar productos…").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Buscar burritos, bebidas…").assertCountEquals(0)
    }
}
