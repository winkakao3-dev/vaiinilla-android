package com.vaiinilla.app.data.inventory

import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.domain.model.OrderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** El repositorio real de inventario contra lo que el servidor de pruebas contestó de verdad. */
class RemoteInventoryRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape) }

    private fun inventory(account: String = "admin") = RemoteInventoryRepository(floor.client(account))

    private fun id() = UUID.randomUUID().toString()

    @Test
    fun `un insumo entra, se merma, se cuenta, se liga a un producto y la venta lo descuenta`() {
        val inventory = inventory()
        val name = tape.value("insumo") { "Frijol de cinta ${id().take(6)}" }
        val input =
            InventorySupplyInput(
                name = name,
                category = "Abarrotes",
                unit = InventoryUnit.KILO,
                minimum = 2.0,
                unitCost = 30.0,
                initialStock = 5.0,
                supplierName = "Don Beto",
                supplierPhone = "6140000000",
            )
        val supply = inventory.createSupply(input).getOrThrow()
        assertEquals(name, supply.name)
        assertEquals(InventoryUnit.KILO, supply.unit)
        assertEquals(5.0, supply.stock, 0.001)
        assertEquals(150.0, supply.value, 0.001)
        assertFalse(supply.low)
        assertEquals("Don Beto", supply.supplierName)
        assertTrue(
            "subir el mínimo por encima de lo que hay lo marca bajo",
            inventory.updateSupply(supply.id, input.copy(minimum = 6.0)).getOrThrow().low,
        )

        val purchase = id()
        val entry =
            inventory
                .registerMovement(
                    purchase,
                    supply.id,
                    InventoryMovementType.ENTRY,
                    10.0,
                    32.0,
                    "Compra",
                ).getOrThrow()
        assertEquals(15.0, entry.stock, 0.001)
        assertFalse(entry.duplicate)
        val again =
            inventory
                .registerMovement(
                    purchase,
                    supply.id,
                    InventoryMovementType.ENTRY,
                    10.0,
                    32.0,
                    "Compra",
                ).getOrThrow()
        assertTrue("la misma compra dos veces no suma dos veces", again.duplicate)
        assertEquals(15.0, again.stock, 0.001)
        val waste =
            inventory
                .registerMovement(
                    id(),
                    supply.id,
                    InventoryMovementType.WASTE,
                    1.0,
                    null,
                    "Se cayó",
                ).getOrThrow()
        assertEquals(14.0, waste.stock, 0.001)
        assertEquals(1, inventory.registerCount(listOf(Triple(id(), supply.id, 13.5)), "Conteo de cierre").getOrThrow())
        assertEquals(
            13.5,
            inventory
                .supplies()
                .getOrThrow()
                .supplies
                .single { it.id == supply.id }
                .stock,
            0.001,
        )

        val product = floor.product()
        val line = RecipeLine(supply.id, name, InventoryUnit.KILO, 0.25, null, null, 0.0)
        val saved = inventory.saveRecipe(product.id, listOf(line)).getOrThrow().single()
        assertEquals(0.25, saved.quantity, 0.001)
        assertTrue("la receta cuesta lo que cuesta su insumo", saved.cost > 0.0)
        assertEquals(listOf(supply.id), inventory.recipe(product.id).getOrThrow().map { it.supplyId })
        val listed = inventory.products().getOrThrow().single { it.id == product.id }
        assertTrue(listed.hasRecipe)
        assertEquals(54, listed.enoughFor)
        assertEquals(listOf(product.id), inventory.supplyProducts(supply.id).getOrThrow().map { it.productId })
        val linked = inventory.saveSupplyProducts(supply.id, mapOf(product.id to 0.5)).getOrThrow().single()
        assertEquals(0.5, linked.quantity, 0.001)

        floor.open()
        val order = floor.order(product, 2)
        val cashier = floor.orders("cajero")
        val paid =
            cashier
                .collectCash(
                    order.summary.id,
                    order.summary.total,
                    order.summary.version,
                    floor.key(),
                    null,
                ).getOrThrow()
        val kitchen = floor.orders("cocina")
        val cooking =
            kitchen
                .transition(
                    order.summary.id,
                    OrderState.PREPARING,
                    paid.order.summary.version,
                    floor.key(),
                    null,
                ).getOrThrow()
        val ready =
            kitchen
                .transition(
                    order.summary.id,
                    OrderState.READY,
                    cooking.summary.version,
                    floor.key(),
                    null,
                ).getOrThrow()
        cashier
            .transition(
                order.summary.id,
                OrderState.DELIVERED,
                ready.summary.version,
                floor.key(),
                null,
            ).getOrThrow()
        assertEquals(
            12.5,
            inventory
                .supplies()
                .getOrThrow()
                .supplies
                .single { it.id == supply.id }
                .stock,
            0.001,
        )

        val movements = inventory.movements(null, null, supply.id).getOrThrow()
        assertTrue(movements.any { it.folio == order.summary.folio })
        assertTrue(movements.any { it.note == "Se cayó" })
        val row =
            inventory
                .report(null, null)
                .getOrThrow()
                .rows
                .single { it.id == supply.id }
        assertEquals(10.0, row.entries, 0.001)
        assertEquals(1.0, row.waste, 0.001)
        assertEquals(1.0, row.sold, 0.001)
        assertEquals(12.5, row.end, 0.001)

        inventory.updateSupply(supply.id, input.copy(minimum = 20.0)).getOrThrow()
        val alert =
            inventory
                .alerts()
                .getOrThrow()
                .belowMinimum
                .single { it.id == supply.id }
        assertEquals("Don Beto", alert.supplierName)
        assertEquals("6140000000", alert.supplierPhone)

        assertTrue(inventory.setKitchenEdits(true).getOrThrow())
        assertTrue(inventory("cocina").supplies().getOrThrow().canEdit)
        assertFalse(inventory.setKitchenEdits(false).getOrThrow())
        assertFalse(inventory("cocina").supplies().getOrThrow().canEdit)

        inventory.saveRecipe(product.id, emptyList()).getOrThrow()
        inventory.updateSupply(supply.id, input.copy(active = false)).getOrThrow()
        assertTrue(
            inventory
                .supplies()
                .getOrThrow()
                .supplies
                .none { it.id == supply.id },
        )
    }

    @Test
    fun `el asistente de inventario contesta y sugiere alergenos de un producto`() {
        val inventory = inventory()
        val reply =
            inventory
                .askAssistant(
                    listOf(InventoryChatMessage(fromStaff = true, text = "¿Qué me falta para mañana?")),
                ).getOrThrow()
        assertTrue(reply.message.isNotBlank())
        assertTrue(reply.remainingMessages >= 0)
        val suggestion =
            inventory
                .suggestAllergens(
                    "Torta de huevo con queso",
                    null,
                    "huevo, queso, pan de trigo, mantequilla",
                    null,
                ).getOrThrow()
        assertTrue("huevo y queso traen alérgenos", suggestion.codes.isNotEmpty())
    }
}
