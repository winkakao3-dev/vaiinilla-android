package com.vaiinilla.app.ui.order

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.parentmode.LocalChildControls
import com.vaiinilla.app.ui.screens.MenuFeedScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** En Desliza, lo que el tutor bloqueó sale con candado y ni el "+" ni el doble toque lo agregan. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class MenuFeedChildRestrictionUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val added = mutableListOf<Int>()
    private val opened = mutableListOf<Int>()

    @After
    fun tearDown() {
        LocalChildControls.value = null
    }

    private fun product(
        id: Int,
        name: String,
    ) = Product(
        id = id,
        categoryId = 1,
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

    private fun showFeed(
        blocked: Set<Int>,
        products: List<Product> = listOf(product(10, "Papas con chile"), product(11, "Manzana")),
        withControls: Boolean = true,
    ) {
        LocalChildControls.value =
            if (!withControls) {
                null
            } else {
                MyGuardianControls(
                    tutorNames = listOf("Ana"),
                    dailyLimit = null,
                    availableToday = null,
                    blockedProductIds = blocked,
                    allergenProductIds = emptySet(),
                    allergens = emptyList(),
                )
            }
        val catalog =
            Catalog(
                categories = listOf(Category(1, "Snacks", order = 1)),
                products = products,
                cursor = null,
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                MenuFeedScreen(
                    state = OrderFlowUiState(loading = false, catalog = catalog),
                    onBack = {},
                    onQuickAdd = { added += it },
                    onQuickRemove = {},
                    onOpenProduct = { opened += it },
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
    fun `lo bloqueado lleva el candado con su motivo y no tiene el +`() {
        showFeed(blocked = setOf(10))

        composeTestRule.onNodeWithText("Papas con chile").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu tutor lo bloqueó").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Agregar Papas con chile").assertDoesNotExist()
        // El siguiente, sin bloqueo, ya está compuesto debajo y sí tiene el suyo.
        composeTestRule.onNodeWithContentDescription("Agregar Manzana").assertExists()
    }

    @Test
    fun `el doble toque sobre lo bloqueado no lo agrega ni abre la ficha y dice por qué`() {
        showFeed(blocked = setOf(10))

        composeTestRule.onRoot().performTouchInput { doubleClick(center) }
        composeTestRule.waitForIdle()

        assertEquals(emptyList<Int>(), added)
        assertEquals(emptyList<Int>(), opened)
        // El motivo junto al nombre y el aviso de arriba.
        assertEquals(
            2,
            composeTestRule
                .onAllNodesWithText("Tu tutor lo bloqueó")
                .fetchSemanticsNodes()
                .size,
        )
    }

    @Test
    fun `sin bloqueo el + y el doble toque agregan como siempre`() {
        showFeed(blocked = emptySet())

        composeTestRule.onNodeWithText("Tu tutor lo bloqueó").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Agregar Papas con chile").assertIsDisplayed()
        composeTestRule.onRoot().performTouchInput { doubleClick(center) }
        composeTestRule.waitForIdle()

        assertEquals(listOf(10), added)
    }

    @Test
    fun `lo que el catalogo del alumno manda bloqueado se ve con su motivo aunque no haya controles`() {
        showFeed(
            blocked = emptySet(),
            products =
                listOf(
                    product(10, "Papas con chile").copy(blockReason = "alergeno"),
                    product(11, "Manzana"),
                ),
            withControls = false,
        )

        composeTestRule.onNodeWithText("Papas con chile").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tiene algo que no puedes comer").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Agregar Papas con chile").assertDoesNotExist()
    }
}
