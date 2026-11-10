package com.vaiinilla.app.ui.screens.admin

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

/** Un inventario fijo para las pruebas de pantalla: devuelve [stock] y no guarda nada. */
internal class StubInventory(
    private val stock: List<InventorySupply> = emptyList(),
) : InventoryRepository {
    override fun supplies(includeInactive: Boolean) =
        Result.success(InventorySupplies(stock, stock.count { it.low }, stock.sumOf { it.value }))

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
