package com.vaiinilla.app.ui.screens.admin

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** J02: si el negocio renta canchas, el producto "Renta cancha" avisa que las rentas no son productos (como iOS). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AdminMenuRentalWarningUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val rental =
        Product(
            id = 1,
            categoryId = 1,
            preparationStation = PreparationStation.CASHIER,
            name = "Renta cancha 1 h",
            description = "",
            ingredients = "",
            allergens = "{}",
            estimatedTimeMinutes = 1,
            counterPrice = "300.00",
            digitalPrice = "300.00",
            available = true,
            imageUrl = "",
            optionGroups = emptyList(),
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
            catalog = Catalog(categories = listOf(Category(1, "Canchas", 1)), products = listOf(rental), cursor = null),
        )

    @Test
    fun `nombrar un producto Renta cancha avisa que se cobra desde Mesas y espacios`() {
        composeTestRule.setContent {
            AdminMenuTab(state, rememberOperationalColors(), readOnly = false, actions = actions, rentsSpaces = true)
        }
        composeTestRule.onAllNodesWithText("Renta cancha 1 h").onFirst().performClick()
        composeTestRule
            .onNodeWithText(
                "Esto parece una renta de cancha. Las rentas se cobran desde Mesas y espacios, con su precio por hora.",
            ).assertExists()
    }
}
