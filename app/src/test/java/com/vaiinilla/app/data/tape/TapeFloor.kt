package com.vaiinilla.app.data.tape

import com.vaiinilla.app.core.security.InMemoryPickupTokenStore
import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.data.admin.RemoteAdminRepository
import com.vaiinilla.app.data.admin.RemoteAdminSpacesRepository
import com.vaiinilla.app.data.catalog.RemoteCatalogRepository
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.operational.RemoteCashSessionRepository
import com.vaiinilla.app.data.operational.RemoteDeviceHeartbeatRepository
import com.vaiinilla.app.data.operational.RemoteWaiterRepository
import com.vaiinilla.app.data.operational.WaiterContractJson
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.order.RemoteOrderRepository
import com.vaiinilla.app.data.sharedtable.RemoteSharedTableRepository
import com.vaiinilla.app.data.sharedtable.SharedTableContractJson
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.ui.discovery.QrPayload
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import java.util.UUID

/** Lo que un recorrido necesita antes de empezar en una tienda de grabación: caja abierta, estaciones y un producto. */
class TapeFloor(
    private val tape: ServerTape,
    private val store: String = "restaurante",
) {
    val venue = VenueContextStore()
    val orderJson = OrderContractJson()

    fun client(account: String) = tape.client("$store.$account")

    fun key() = UUID.randomUUID().toString()

    fun orders(account: String) = RemoteOrderRepository(client(account), orderJson, InMemoryPickupTokenStore())

    fun waiter(account: String) = RemoteWaiterRepository(client(account), WaiterContractJson(), orderJson, venue)

    fun cash(account: String = "cajero") = RemoteCashSessionRepository(client(account), orderJson)

    fun admin() = RemoteAdminRepository(client("admin"), ContractResponseParser(), orderJson)

    fun spaces() = RemoteAdminSpacesRepository(client("admin"), venue)

    fun catalog(account: String) = RemoteCatalogRepository(client(account), ContractResponseParser(), venue)

    fun sharedTable(account: String) = RemoteSharedTableRepository(client(account), SharedTableContractJson())

    /** El cliente se sienta leyendo el QR del lugar: sin eso el servidor no lo deja pagar al final. */
    fun sit(
        space: ManagedSpace,
        account: String,
        alias: String,
    ): SharedTable = sharedTable(account).join(qrToken(space), alias).getOrThrow()

    fun qrToken(space: ManagedSpace): String =
        (QrPayloadParser.parse(space.qrUrl).getOrThrow() as QrPayload.SpaceToken).token

    /** Caja y Cocina conectadas y con turno abierto: sin eso el servidor no recibe pedidos. */
    fun open() {
        RemoteDeviceHeartbeatRepository(client("cajero")).sendHeartbeat(DEVICE, OperationalRole.CASHIER).getOrThrow()
        RemoteDeviceHeartbeatRepository(client("cocina")).sendHeartbeat(DEVICE, OperationalRole.KITCHEN).getOrThrow()
        if (cash().activeSession().getOrThrow() == null) cash().openSession("500.00", key()).getOrThrow()
    }

    fun product(): Product {
        catalog("admin")
            .getCatalog()
            .getOrThrow()
            .products
            .firstOrNull { it.name == PRODUCT }
            ?.let { return it }
        val category = admin().createCategory(AdminCategoryInput("Cintas", 1), key()).getOrThrow()
        val input =
            AdminProductInput(
                categoryId = category.id,
                preparationStation = PreparationStation.KITCHEN,
                name = PRODUCT,
                description = "Para grabar las pruebas",
                ingredients = "Pan y frijoles",
                allergens = "",
                estimatedTimeMinutes = 5,
                counterPrice = PRICE,
                available = true,
                optionGroups = emptyList(),
            )
        return admin().createProduct(input, key()).getOrThrow()
    }

    fun newSpace(type: SpaceType = SpaceType.TABLE): ManagedSpace {
        val name = tape.value("lugar") { "Cinta ${UUID.randomUUID().toString().take(6)}" }
        return spaces().create(name, type, key(), null, null, null).getOrThrow()
    }

    /** Un pedido del cliente en efectivo: en la mesa (a la cuenta si [payAtEnd]) o para llevar. */
    fun order(
        product: Product,
        quantity: Int,
        spaceId: Int? = null,
        payAtEnd: Boolean = false,
        account: String = "cliente",
    ): OrderDetail {
        val request =
            CreateOrderRequest(
                paymentMethod = PaymentMethod.CASH,
                destination = if (spaceId == null) OrderDestination.TAKE_AWAY else OrderDestination.IN_SPACE,
                spaceId = spaceId,
                kitchenNotes = "",
                items = listOf(CreateOrderItem(product.id, quantity, emptyList())),
                payAtEnd = payAtEnd,
            )
        return orders(account).createOrder(request, key()).getOrThrow().order
    }

    companion object {
        const val PRODUCT = "Torta de cinta"
        const val PRICE = "45.00"
        const val DEVICE = "c1a7a000-0000-4000-8000-000000000001"
    }
}
