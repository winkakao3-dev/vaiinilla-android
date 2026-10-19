package com.vaiinilla.app.ui.order

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.screens.MenuFeedScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** En Desliza, deslizar de lado cambia de categoría; hacia arriba sigue siendo el siguiente platillo. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class MenuFeedSwipeUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun product(
        id: Int,
        categoryId: Int,
        name: String,
    ) = Product(
        id = id,
        categoryId = categoryId,
        preparationStation = PreparationStation.KITCHEN,
        name = name,
        description = "",
        ingredients = "",
        allergens = "",
        estimatedTimeMinutes = 10,
        counterPrice = "50.00",
        digitalPrice = "50.00",
        available = true,
        imageUrl = "",
        optionGroups = emptyList(),
    )

    private fun showFeed() {
        val catalog =
            Catalog(
                categories = listOf(Category(1, "Tacos", order = 1), Category(2, "Aguas", order = 2)),
                products =
                    listOf(
                        product(10, 1, "Taco al pastor"),
                        product(11, 1, "Taco de suadero"),
                        product(20, 2, "Agua de jamaica"),
                    ),
                cursor = null,
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                MenuFeedScreen(
                    state = OrderFlowUiState(loading = false, catalog = catalog),
                    onBack = {},
                    onQuickAdd = {},
                    onQuickRemove = {},
                    onOpenProduct = {},
                    onDismissProduct = {},
                    onToggleOption = { _, _ -> },
                    onClearOptionalGroup = {},
                    onSelectedNotesChange = {},
                    onQuantityChange = {},
                    onAddSelected = {},
                    onOpenCart = {},
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `a la izquierda va a la siguiente categoría y a la derecha regresa`() {
        showFeed()
        composeTestRule.onNodeWithText("Taco al pastor").assertIsDisplayed()

        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Agua de jamaica").assertIsDisplayed()

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Taco al pastor").assertIsDisplayed()
    }

    @Test
    fun `en la primera categoría deslizar a la derecha no cambia nada`() {
        showFeed()
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Taco al pastor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agua de jamaica").assertDoesNotExistOrHidden()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertDoesNotExistOrHidden() {
        if (fetchSemanticsNodes().isNotEmpty()) assertIsNotDisplayed()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.fetchSemanticsNodes() =
        runCatching { fetchSemanticsNode() }.map { listOf(it) }.getOrDefault(emptyList())
}
