package com.vaiinilla.app.ui.order

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
class CatalogBehindSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `talkback reaches the menu when no product is open`() {
        render(selected = false)

        composeTestRule.onAllNodesWithText("Buscar productos…").assertCountEquals(1)
    }

    @Test
    fun `talkback does not reach the menu hidden under an open product`() {
        render(selected = true)

        composeTestRule.onAllNodesWithText("Buscar productos…").assertCountEquals(0)
    }

    private fun render(selected: Boolean) {
        val state = ScreenshotFixtures.catalogLoadedState()
        val product = state.catalog!!.products.first()
        composeTestRule.setContent {
            ScreenshotTheme {
                CatalogScreen(
                    state = if (selected) state.copy(selectedProductId = product.id) else state,
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
        composeTestRule.waitForIdle()
    }
}
