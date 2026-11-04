package com.vaiinilla.app.ui.screens.admin

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
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
class AdminMenuArchiveUiTest {
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

    private val calls = mutableListOf<String>()

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
            onSetArchived = { product, archived -> calls += "${product.name}:$archived" },
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
    fun `archivar pide confirmacion y lo archivado se puede desarchivar`() {
        composeTestRule.setContent {
            val colors = rememberOperationalColors()
            AdminDialogHost(colors) {
                AdminMenuTab(
                    state.copy(
                        archivedProducts = listOf(product(3, "Té viejo", reviewed = true).copy(archived = true)),
                    ),
                    colors,
                    readOnly = false,
                    actions = actions,
                    header = testHeader,
                )
            }
        }
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Agua"))
        composeTestRule.onAllNodesWithText("Agua").onFirst().performClick()
        composeTestRule
            .onAllNodes(
                hasScrollAction(),
            ).onLast()
            .performScrollToNode(hasText("Opciones y extras", substring = true))
        composeTestRule.onNodeWithText("Opciones y extras", substring = true).performClick()
        composeTestRule.onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText("Archivar"))
        composeTestRule.onNodeWithText("Archivar").performClick()
        composeTestRule.onNodeWithText("¿Archivar Agua?").assertExists()
        assertEquals(emptyList<String>(), calls)
        composeTestRule.onAllNodesWithText("Archivar").onLast().performClick()
        assertEquals(listOf("Agua:true"), calls)

        composeTestRule.onAllNodesWithText("Archivados · 1").onFirst().performClick()
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Té viejo"))
        composeTestRule.onNodeWithText("Restaurar").performClick()
        composeTestRule.onNodeWithText("¿Regresar Té viejo al menú?").assertExists()
        assertEquals(listOf("Agua:true"), calls)
        composeTestRule.onNodeWithText("Regresar al menú").performClick()
        assertEquals(listOf("Agua:true", "Té viejo:false"), calls)
    }

    @Test
    fun `el producto lee archivado y sin el campo no esta archivado`() {
        val parser =
            com.vaiinilla.app.data.contract
                .ContractResponseParser()

        fun read(extra: String) =
            parser.parseProduct(
                """{"data":{"id":4,"categoria_id":3,"estacion_preparacion":"caja","nombre":"Galleta",
                "tiempo_estimado_min":1,"precio_mostrador":"20.00","precio_digital":"20.00","disponible":true$extra}}""",
            )
        org.junit.Assert.assertTrue(read(""","archivado":true""").archived)
        org.junit.Assert.assertFalse(read("").archived)
    }
}

private val testHeader = AdminHeader(placeName = "Comedor", photoUrl = null, logoUrl = null) {}
