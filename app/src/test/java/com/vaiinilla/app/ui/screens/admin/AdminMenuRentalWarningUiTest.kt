package com.vaiinilla.app.ui.screens.admin

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.vaiinilla.app.domain.admin.AdminProductInput
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

/** EQUIPO-14 y EQUIPO-22 (J02): si el negocio renta canchas, el producto "Renta cancha" avisa que las rentas no son productos (como iOS). */
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

    private var saved: AdminProductInput? = null

    private val actions =
        AdminActions(
            onRefreshHome = {},
            onRefreshCatalog = {},
            onSelectPeriod = {},
            onSelectCustomPeriod = {},
            onCloseWithoutCharging = { _, _, _ -> },
            onSaveCategory = { _, _, _ -> },
            onSaveProduct = { _, input, _, _ -> saved = input },
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
            AdminMenuTab(
                state,
                rememberOperationalColors(),
                readOnly = false,
                actions = actions,
                rentsSpaces = true,
                header = testHeader,
            )
        }
        openProduct("Renta cancha 1 h")
        composeTestRule
            .onNodeWithText(
                "Esto parece una renta de cancha. Las rentas se cobran desde Mesas y espacios, con su precio por hora.",
            ).assertExists()
    }

    private fun openProduct(name: String) {
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(name))
        composeTestRule.onAllNodesWithText(name).onFirst().performClick()
    }

    @Test
    fun `aunque el negocio no rente espacios, un nombre de renta avisa`() {
        composeTestRule.setContent {
            AdminMenuTab(
                state,
                rememberOperationalColors(),
                readOnly = false,
                actions = actions,
                rentsSpaces = false,
                header = testHeader,
            )
        }
        openProduct("Renta cancha 1 h")
        composeTestRule
            .onNodeWithText("Esto parece una renta. Si cobras un espacio por hora, dalo de alta en Mesas y espacios.")
            .assertExists()
    }

    @Test
    fun `un nombre nuevo de renta pide confirmar antes de guardar`() {
        composeTestRule.setContent {
            AdminMenuTab(
                state,
                rememberOperationalColors(),
                readOnly = false,
                actions = actions,
                rentsSpaces = false,
                header = testHeader,
            )
        }
        openProduct("Renta cancha 1 h")
        composeTestRule
            .onNode(
                hasSetTextAction() and hasText("Renta cancha 1 h"),
            ).performTextReplacement("Renta cancha 2 h")
        composeTestRule
            .onNodeWithText(
                "Guardar cambios",
            ).performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithText("¿Guardar como producto?").assertExists()
        assert(saved == null)
        composeTestRule.onNodeWithText("Guardar").performClick()
        assert(saved?.name == "Renta cancha 2 h")
    }

    @Test
    fun `el interruptor Disponible del formulario cambia y se guarda apagado`() {
        composeTestRule.setContent {
            AdminMenuTab(
                state,
                rememberOperationalColors(),
                readOnly = false,
                actions = actions,
                rentsSpaces = false,
                header = testHeader,
            )
        }
        openProduct("Renta cancha 1 h")
        // En Robolectric los toques no llegan a la hoja modal; se usa la acción de clic de la fila.
        val switch =
            composeTestRule.onNode(isToggleable() and hasText("Apagado, sale como agotado y no se puede pedir."))
        switch.performScrollTo().assertIsOn()
        switch.performSemanticsAction(SemanticsActions.OnClick)
        switch.assertIsOff()
        composeTestRule
            .onNodeWithText("Guardar cambios")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        assert(saved?.available == false)
    }

    @Test
    fun `la busqueda encuentra por pedazo y sin acentos`() {
        val products =
            listOf(
                rental.copy(id = 2, name = "Chocolate caliente"),
                rental.copy(id = 3, name = "Café de olla"),
            )
        val menu = AdminUiState(catalog = state.catalog!!.copy(products = products))
        composeTestRule.setContent {
            AdminMenuTab(menu, rememberOperationalColors(), readOnly = false, actions = actions, header = testHeader)
        }
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasSetTextAction())
        val search = composeTestRule.onNode(hasSetTextAction())
        search.performTextInput("cafe")
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Café de olla"))
        composeTestRule.onAllNodesWithText("Chocolate caliente").assertCountEquals(0)
        search.performTextReplacement("Chocolate")
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Chocolate caliente"))
        composeTestRule.onAllNodesWithText("Café de olla").assertCountEquals(0)
    }
}

private val testHeader = AdminHeader(placeName = "Comedor", photoUrl = null, logoUrl = null) {}
