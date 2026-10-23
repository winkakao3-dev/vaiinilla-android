package com.vaiinilla.app.ui.order

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B02, C03: la ficha abre sin opción elegida en un grupo obligatorio y Agregar espera a que se escoja. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProductRequiredOptionUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `add stays off until the required size is chosen`() {
        // Como queda el estado al abrir la ficha (openProduct): el agua lleva Tamaño obligatorio.
        val state = mutableStateOf(ScreenshotFixtures.catalogLoadedState().copy(selectedProductId = 101))
        composeTestRule.setContent {
            val current = state.value
            val product = requireNotNull(current.selectedProduct)
            ScreenshotTheme {
                ProductDetailSheet(
                    product = product,
                    categoryName = "Bebidas",
                    selectedOptionIds = current.selectedOptionIds,
                    quantity = 1,
                    previewPrice = current.selectedProductPreviewPrice,
                    previewTotal = current.selectedProductPreviewTotal,
                    canAdd = current.isSelectedProductValid,
                    errorMessage = null,
                    onDismiss = {},
                    onToggleOption = { _, optionId ->
                        state.value = state.value.copy(selectedOptionIds = setOf(optionId))
                    },
                    onClearOptionalGroup = {},
                    onQuantityChange = {},
                    onAdd = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Agregar", substring = true).assertIsNotEnabled()

        state.value = state.value.copy(selectedOptionIds = setOf(301))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Agregar", substring = true).assertIsEnabled()
    }
}
