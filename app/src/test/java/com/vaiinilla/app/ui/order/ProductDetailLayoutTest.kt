package com.vaiinilla.app.ui.order

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertTrue
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
class ProductDetailLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `product detail keeps add dock visible while content can scroll`() {
        val catalog = ScreenshotFixtures.catalog()
        val product = catalog.products.first()
        val defaultIds =
            setOf(
                product.optionGroups
                    .first()
                    .options
                    .first()
                    .id,
            )

        composeTestRule.setContent {
            ScreenshotTheme {
                ProductDetailSheet(
                    product = product,
                    categoryName = "Bebidas",
                    selectedOptionIds = defaultIds,
                    defaultOptionIds = defaultIds,
                    quantity = 1,
                    previewPrice = product.digitalPrice,
                    previewTotal = product.digitalPrice,
                    canAdd = true,
                    errorMessage = null,
                    onDismiss = {},
                    onToggleOption = { _, _ -> },
                    onClearOptionalGroup = {},
                    onQuantityChange = {},
                    onAdd = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Agregar · $20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tamaño", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Ingredientes", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar · $20").assertIsDisplayed()
    }

    @Test
    fun `dense product exposes all option groups without crushing the dock`() {
        val catalog = ScreenshotFixtures.catalog()
        val product = catalog.products.first { it.name == "Burrito norteño" }
        val selectedIds = setOf(310, 314)

        composeTestRule.setContent {
            ScreenshotTheme {
                ProductDetailSheet(
                    product = product,
                    categoryName = "Comida",
                    selectedOptionIds = selectedIds,
                    defaultOptionIds = selectedIds,
                    quantity = 1,
                    previewPrice = product.digitalPrice,
                    previewTotal = product.digitalPrice,
                    canAdd = true,
                    errorMessage = null,
                    onDismiss = {},
                    onToggleOption = { _, _ -> },
                    onClearOptionalGroup = {},
                    onQuantityChange = {},
                    onAdd = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Agregar · $70").assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-detail-scroll", useUnmergedTree = true).performScrollToIndex(4)
        composeTestRule.onNodeWithText("Extra", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Aguacate +$10", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar · $70").assertIsDisplayed()
    }

    @Test
    fun `product detail tolerates larger text`() {
        val catalog = ScreenshotFixtures.catalog()
        val product = catalog.products.first()
        val defaultIds =
            setOf(
                product.optionGroups
                    .first()
                    .options
                    .first()
                    .id,
            )

        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.25f),
            ) {
                ScreenshotTheme {
                    ProductDetailSheet(
                        product = product,
                        categoryName = "Bebidas",
                        selectedOptionIds = defaultIds,
                        defaultOptionIds = defaultIds,
                        quantity = 1,
                        previewPrice = product.digitalPrice,
                        previewTotal = product.digitalPrice,
                        canAdd = true,
                        errorMessage = null,
                        onDismiss = {},
                        onToggleOption = { _, _ -> },
                        onClearOptionalGroup = {},
                        onQuantityChange = {},
                        onAdd = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Agregar · $20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agua de jamaica").assertIsDisplayed()
    }

    @Test
    fun `product surface keeps breathing room from the top edge`() {
        val catalog = ScreenshotFixtures.catalog()
        val product = catalog.products.first()
        val defaultIds =
            setOf(
                product.optionGroups
                    .first()
                    .options
                    .first()
                    .id,
            )

        composeTestRule.setContent {
            ScreenshotTheme {
                ProductDetailSheet(
                    product = product,
                    categoryName = "Bebidas",
                    selectedOptionIds = defaultIds,
                    defaultOptionIds = defaultIds,
                    quantity = 1,
                    previewPrice = product.digitalPrice,
                    previewTotal = product.digitalPrice,
                    canAdd = true,
                    errorMessage = null,
                    onDismiss = {},
                    onToggleOption = { _, _ -> },
                    onClearOptionalGroup = {},
                    onQuantityChange = {},
                    onAdd = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        val top =
            composeTestRule
                .onNodeWithTag("product-detail-surface", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertTrue("Product detail surface must start below the root top edge", top > 0f)
    }
}
