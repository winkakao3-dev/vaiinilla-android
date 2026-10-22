package com.vaiinilla.app.ui.screens.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AdminMenuAllergensUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun product(
        id: Int,
        name: String,
        reviewed: Boolean,
    ) = Product(
        id = id,
        categoryId = 1,
        preparationStation = PreparationStation.CASHIER,
        name = name,
        description = "",
        ingredients = "",
        allergens = "{}",
        estimatedTimeMinutes = 1,
        counterPrice = "20.00",
        digitalPrice = "20.00",
        available = true,
        imageUrl = "",
        optionGroups = emptyList(),
        allergensReviewed = reviewed,
    )

    private val actions =
        AdminActions(
            onRefreshHome = {},
            onRefreshCatalog = {},
            onSelectPeriod = {},
            onSelectCustomPeriod = {},
            onCloseWithoutCharging = { _, _, _ -> },
            onSaveCategory = { _, _, _ -> },
            onSaveProduct = { _, _, _, _ -> },
            onSetAvailable = { _, _ -> },
            onRemoveImage = {},
            onSelectOrderFilter = {},
            onRefreshOrders = {},
            onLoadMoreOrders = {},
        )

    private val state =
        AdminUiState(
            catalog =
                Catalog(
                    categories = listOf(Category(1, "Snacks", 1)),
                    products = listOf(product(1, "Galleta", reviewed = false), product(2, "Agua", reviewed = true)),
                    cursor = null,
                ),
        )

    @Test
    fun `a school menu counts and marks products without reviewed allergens`() {
        composeTestRule.setContent {
            AdminMenuTab(state, rememberOperationalColors(), readOnly = false, actions = actions, isSchool = true)
        }
        composeTestRule.onNodeWithText("1 producto sin alérgenos revisados").assertExists()
        composeTestRule
            .onNodeWithText(
                "Un alumno con alergias no puede pedirlos hasta que marques qué tienen.",
            ).assertExists()
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Galleta"))
        composeTestRule.onNodeWithText("Alérgenos sin revisar", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `a menu that is not a school says nothing about allergens`() {
        composeTestRule.setContent {
            AdminMenuTab(state, rememberOperationalColors(), readOnly = false, actions = actions, isSchool = false)
        }
        composeTestRule.onNodeWithText("1 producto sin alérgenos revisados").assertDoesNotExist()
        composeTestRule.onNodeWithText("Alérgenos sin revisar").assertDoesNotExist()
    }

    @Test
    fun `allergen chips toggle codes and "No tiene ninguno" clears them`() {
        var codes by mutableStateOf(emptySet<String>())
        var reviewed by mutableStateOf(false)
        composeTestRule.setContent {
            AllergenReview(codes, reviewed, rememberOperationalColors(), enabled = true) {
                codes = it
                reviewed = true
            }
        }
        composeTestRule
            .onNodeWithText(
                "Sin revisar: un alumno con alergias no lo puede pedir. Marca lo que tiene o \"No tiene ninguno\".",
            ).assertExists()
        composeTestRule.onNodeWithText("Gluten").performClick()
        composeTestRule.onNodeWithText("Lácteos").performClick()
        assertEquals(setOf("gluten", "lacteos"), codes)
        composeTestRule.onNodeWithText("Un alumno con alguna de estas alergias no lo podrá pedir.").assertExists()
        composeTestRule.onNodeWithText("Gluten").performClick()
        assertEquals(setOf("lacteos"), codes)
        composeTestRule.onNodeWithText("No tiene ninguno").performClick()
        assertEquals(emptySet<String>(), codes)
    }

    @Test
    fun `the old allergen text hides an empty set`() {
        assertEquals("", legacyAllergensText("{}"))
        assertEquals("", legacyAllergensText(" [] "))
        assertEquals("", legacyAllergensText(null))
        assertEquals("Lácteos, nuez", legacyAllergensText("Lácteos, nuez"))
    }
}
