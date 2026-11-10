package com.vaiinilla.app.ui.screens.admin

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.vaiinilla.app.data.inventory.InventoryMovement
import com.vaiinilla.app.data.inventory.InventoryMovementType
import com.vaiinilla.app.data.inventory.InventoryProduct
import com.vaiinilla.app.data.inventory.InventoryReport
import com.vaiinilla.app.data.inventory.InventoryRepository
import com.vaiinilla.app.data.inventory.InventorySupplies
import com.vaiinilla.app.data.inventory.InventorySupply
import com.vaiinilla.app.data.inventory.InventorySupplyInput
import com.vaiinilla.app.data.inventory.MovementResult
import com.vaiinilla.app.data.inventory.RecipeLine
import com.vaiinilla.app.data.inventory.SupplyProductLink
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.admin.AdminInventoryViewModel
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * V01 (QA de Miguel, 8 nov): al crear «Cubeta Corona» la hoja no traía «Receta». Ahora el renglón sale desde que se
 * crea el producto, explica que primero se guarda y, al guardar, la hoja se queda abierta con la receta lista.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AdminMenuNewProductRecipeUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val cubeta =
        Product(
            id = 7,
            categoryId = 1,
            preparationStation = PreparationStation.CASHIER,
            name = "Cubeta Corona",
            description = "",
            ingredients = "",
            allergens = "{}",
            estimatedTimeMinutes = 1,
            counterPrice = "250.00",
            digitalPrice = "250.00",
            available = true,
            imageUrl = "",
            optionGroups = emptyList(),
        )

    private val state =
        AdminUiState(
            catalog = Catalog(categories = listOf(Category(1, "Bebidas", 1)), products = emptyList(), cursor = null),
        )

    private val inventory = AdminInventoryViewModel(EmptyInventory())

    private fun actions(onSave: (() -> Unit)? = null) =
        AdminActions(
            onRefreshHome = {},
            onRefreshCatalog = {},
            onSelectPeriod = {},
            onSelectCustomPeriod = {},
            onCloseWithoutCharging = { _, _, _ -> },
            onSaveCategory = { _, _, _ -> },
            onSaveProduct = { _, _, _, done -> onSave?.invoke() ?: done(cubeta) },
            onSetAvailable = { _, _ -> },
            onRemoveImage = {},
            onSelectOrderFilter = {},
            onRefreshOrders = {},
            onLoadMoreOrders = {},
        )

    private fun show(actions: AdminActions) {
        composeTestRule.setContent {
            AdminMenuTab(
                state,
                rememberOperationalColors(),
                readOnly = false,
                actions = actions,
                header = AdminHeader(placeName = "Boliche", photoUrl = null, logoUrl = null) {},
                inventory = inventory,
                scope = "negocio-1",
            )
        }
        composeTestRule.onAllNodesWithText("Nuevo producto").onFirst().performClick()
        composeTestRule.waitForIdle()
    }

    private fun scrollTo(text: String) {
        // La hoja del producto va encima de la lista del menú: su scroll es el último.
        composeTestRule.onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText(text))
    }

    @Test
    fun `al crear un producto la hoja ya trae Receta y dice que primero se guarda`() {
        show(actions(onSave = {}))
        scrollTo(NEW_PRODUCT_RECIPE_HINT)
        composeTestRule.onAllNodesWithText("Receta").onFirst().assertExists()
        composeTestRule.onAllNodesWithText(NEW_PRODUCT_RECIPE_HINT).onFirst().assertExists()
    }

    @Test
    fun `al guardar un producto nuevo la hoja sigue abierta para ponerle receta`() {
        show(actions())
        // Nombre y precio son los dos primeros campos de la hoja; sin ellos «Guardar producto» no hace nada.
        val fields = composeTestRule.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("Cubeta Corona")
        fields[1].performTextReplacement("250")
        scrollTo("Guardar producto")
        composeTestRule.onAllNodesWithText("Guardar producto").onFirst().performClick()
        composeTestRule.waitForIdle()
        scrollTo("Guardar cambios")
        composeTestRule.onAllNodesWithText("Guardar cambios").onFirst().assertExists()
        composeTestRule.onAllNodesWithText(NEW_PRODUCT_RECIPE_HINT).assertCountEquals(0)
    }
}

private class EmptyInventory : InventoryRepository {
    override fun supplies(includeInactive: Boolean) =
        Result.success(InventorySupplies(emptyList<InventorySupply>(), 0, 0.0))

    override fun createSupply(input: InventorySupplyInput): Result<InventorySupply> =
        Result.failure(UnsupportedOperationException())

    override fun updateSupply(
        id: Long,
        input: InventorySupplyInput,
    ): Result<InventorySupply> = Result.failure(UnsupportedOperationException())

    override fun registerMovement(
        id: String,
        supplyId: Long,
        type: InventoryMovementType,
        quantity: Double,
        unitCost: Double?,
        note: String?,
    ) = Result.success(MovementResult(quantity, quantity, false))

    override fun registerCount(
        counts: List<Triple<String, Long, Double>>,
        note: String?,
    ) = Result.success(0)

    override fun products() = Result.success(emptyList<InventoryProduct>())

    override fun recipe(productId: Int) = Result.success(emptyList<RecipeLine>())

    override fun saveRecipe(
        productId: Int,
        lines: List<RecipeLine>,
    ) = Result.success(lines)

    override fun supplyProducts(supplyId: Long) = Result.success(emptyList<SupplyProductLink>())

    override fun saveSupplyProducts(
        supplyId: Long,
        products: Map<Int, Double>,
        categories: Map<Int, Double>,
    ) = Result.success(emptyList<SupplyProductLink>())

    override fun movements(
        fromIso: String?,
        toIso: String?,
        supplyId: Long?,
    ) = Result.success(emptyList<InventoryMovement>())

    override fun report(
        fromIso: String?,
        toIso: String?,
    ) = Result.success(InventoryReport(fromIso.orEmpty(), toIso.orEmpty(), emptyList(), 0.0, 0.0, 0.0, 0.0))
}
