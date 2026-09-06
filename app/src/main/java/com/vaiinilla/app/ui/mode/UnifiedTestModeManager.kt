package com.vaiinilla.app.ui.mode

import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.OrderUser
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.PublicEstablishment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UnifiedTestModeManager {
    private const val TEST_VENUE_ID = "test-unified-venue-id"
    private const val TEST_VENUE_SLUG = "tienda-demo"
    private const val TEST_VENUE_NAME = "Cafetería Central (Test)"

    val testEstablishment =
        PublicEstablishment(
            id = TEST_VENUE_ID,
            name = TEST_VENUE_NAME,
            slug = TEST_VENUE_SLUG,
            clientIdLabel = "Matrícula",
            clientIdRequired = false,
        )

    val testGuestVenue = GuestVenueContext(establishment = testEstablishment)

    private val initialProducts =
        listOf(
            Product(
                id = 1,
                categoryId = 1,
                preparationStation = PreparationStation.KITCHEN,
                name = "Hot dog",
                description = "Sin cebolla · extra mostaza",
                ingredients = "Salchicha, pan, mostaza, salsa de tomate",
                allergens = "Gluten",
                estimatedTimeMinutes = 5,
                counterPrice = "75.00",
                digitalPrice = "75.00",
                available = true,
                imageUrl = "https://images.unsplash.com/photo-1619740455993-9e612b1af08a?w=400&q=80",
                optionGroups = emptyList(),
            ),
            Product(
                id = 2,
                categoryId = 2,
                preparationStation = PreparationStation.CASHIER,
                name = "Vaiinilla Latte",
                description = "Caliente · leche entera",
                ingredients = "Café espresso, leche, vainilla",
                allergens = "Lácteos",
                estimatedTimeMinutes = 3,
                counterPrice = "65.00",
                digitalPrice = "65.00",
                available = true,
                imageUrl = "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=400&q=80",
                optionGroups = emptyList(),
            ),
            Product(
                id = 3,
                categoryId = 1,
                preparationStation = PreparationStation.KITCHEN,
                name = "Sándwich de pavo",
                description = "Pan artesanal con aderezo",
                ingredients = "Pavo, queso, pan integral, lechuga",
                allergens = "Gluten, lácteos",
                estimatedTimeMinutes = 6,
                counterPrice = "90.00",
                digitalPrice = "90.00",
                available = true,
                imageUrl = "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=400&q=80",
                optionGroups = emptyList(),
            ),
            Product(
                id = 4,
                categoryId = 3,
                preparationStation = PreparationStation.CASHIER,
                name = "Galleta choco",
                description = "Chispas de chocolate semiamargo",
                ingredients = "Harina, chocolate, mantequilla",
                allergens = "Gluten, lácteos, huevo",
                estimatedTimeMinutes = 1,
                counterPrice = "38.00",
                digitalPrice = "38.00",
                available = false,
                imageUrl = "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=400&q=80",
                optionGroups = emptyList(),
            ),
        )

    private val initialCategories =
        listOf(
            Category(id = 1, name = "Comidas", order = 1),
            Category(id = 2, name = "Bebidas", order = 2),
            Category(id = 3, name = "Postres", order = 3),
        )

    private val testUser = OrderUser(name = "Alumno Demo", enrollment = "MAT-1002")

    private val initialOrders =
        listOf(
            OrderDetail(
                summary =
                    OrderSummary(
                        id = "order-047",
                        folio = 47,
                        operationalDate = "2026-09-05",
                        state = OrderState.READY,
                        paymentMethod = PaymentMethod.CASH,
                        destination = OrderDestination.TAKE_AWAY,
                        space = null,
                        subtotal = "140.00",
                        combinedSavings = "0.00",
                        cashbackAwarded = "0.00",
                        total = "140.00",
                        version = 1,
                        createdAt = "2026-09-05T18:00:00Z",
                        updatedAt = "2026-09-05T18:05:00Z",
                    ),
                user = testUser,
                items =
                    listOf(
                        OrderItem(
                            id = 1,
                            productId = 1,
                            productName = "Hot dog",
                            preparationStation = PreparationStation.KITCHEN,
                            quantity = 1,
                            unitDigitalPrice = "75.00",
                            subtotal = "75.00",
                            options = emptyList(),
                        ),
                        OrderItem(
                            id = 2,
                            productId = 2,
                            productName = "Vaiinilla Latte",
                            preparationStation = PreparationStation.CASHIER,
                            quantity = 1,
                            unitDigitalPrice = "65.00",
                            subtotal = "65.00",
                            options = emptyList(),
                        ),
                    ),
                kitchenNotes = "Entregar todo junto.",
                pickupToken = "047",
            ),
            OrderDetail(
                summary =
                    OrderSummary(
                        id = "order-048",
                        folio = 48,
                        operationalDate = "2026-09-05",
                        state = OrderState.PREPARING,
                        paymentMethod = PaymentMethod.BALANCE,
                        destination = OrderDestination.IN_SPACE,
                        space = null,
                        subtotal = "90.00",
                        combinedSavings = "0.00",
                        cashbackAwarded = "0.00",
                        total = "90.00",
                        version = 1,
                        createdAt = "2026-09-05T18:10:00Z",
                        updatedAt = "2026-09-05T18:12:00Z",
                    ),
                user = testUser,
                items =
                    listOf(
                        OrderItem(
                            id = 3,
                            productId = 3,
                            productName = "Sándwich de pavo",
                            preparationStation = PreparationStation.KITCHEN,
                            quantity = 1,
                            unitDigitalPrice = "90.00",
                            subtotal = "90.00",
                            options = emptyList(),
                        ),
                    ),
                kitchenNotes = "",
                pickupToken = "048",
            ),
            OrderDetail(
                summary =
                    OrderSummary(
                        id = "order-049",
                        folio = 49,
                        operationalDate = "2026-09-05",
                        state = OrderState.PAID,
                        paymentMethod = PaymentMethod.BALANCE,
                        destination = OrderDestination.IN_SPACE,
                        space = null,
                        subtotal = "230.00",
                        combinedSavings = "0.00",
                        cashbackAwarded = "0.00",
                        total = "230.00",
                        version = 1,
                        createdAt = "2026-09-05T18:14:00Z",
                        updatedAt = "2026-09-05T18:14:00Z",
                    ),
                user = testUser,
                items =
                    listOf(
                        OrderItem(
                            id = 4,
                            productId = 2,
                            productName = "Vaiinilla Latte",
                            preparationStation = PreparationStation.CASHIER,
                            quantity = 2,
                            unitDigitalPrice = "65.00",
                            subtotal = "130.00",
                            options = emptyList(),
                        ),
                        OrderItem(
                            id = 5,
                            productId = 1,
                            productName = "Hot dog",
                            preparationStation = PreparationStation.KITCHEN,
                            quantity = 1,
                            unitDigitalPrice = "75.00",
                            subtotal = "75.00",
                            options = emptyList(),
                        ),
                    ),
                kitchenNotes = "",
                pickupToken = "049",
            ),
        )

    private val _isTestModeActive = MutableStateFlow(false)
    val isTestModeActive: StateFlow<Boolean> = _isTestModeActive.asStateFlow()

    private val _catalogFlow =
        MutableStateFlow(
            Catalog(
                products = initialProducts,
                categories = initialCategories,
                cursor = null,
            ),
        )
    val catalogFlow: StateFlow<Catalog> = _catalogFlow.asStateFlow()

    private val _ordersFlow = MutableStateFlow<List<OrderDetail>>(initialOrders)
    val ordersFlow: StateFlow<List<OrderDetail>> = _ordersFlow.asStateFlow()

    fun enableTestMode() {
        _isTestModeActive.value = true
    }

    fun disableTestMode() {
        _isTestModeActive.value = false
    }

    fun getAuthorizedModes(): List<AuthorizedMode> =
        listOf(
            AuthorizedMode(
                role = OperationalRole.CLIENT,
                establishmentId = TEST_VENUE_ID,
                establishmentName = TEST_VENUE_NAME,
                membershipId = "test-mem-client",
            ),
            AuthorizedMode(
                role = OperationalRole.CASHIER,
                establishmentId = TEST_VENUE_ID,
                establishmentName = TEST_VENUE_NAME,
                membershipId = "test-mem-cashier",
            ),
            AuthorizedMode(
                role = OperationalRole.KITCHEN,
                establishmentId = TEST_VENUE_ID,
                establishmentName = TEST_VENUE_NAME,
                membershipId = "test-mem-kitchen",
            ),
        )

    fun createAuthorizedContext(role: OperationalRole): AuthorizedModeContext =
        AuthorizedModeContext(
            role = role,
            establishmentId = TEST_VENUE_ID,
            establishmentName = TEST_VENUE_NAME,
            membershipId = "test-mem-${role.name.lowercase()}",
            accessToken = "mock-test-jwt-token",
        )

    fun toggleProductAvailable(
        productId: Int,
        available: Boolean,
    ) {
        val current = _catalogFlow.value
        val updated =
            current.products.map { p ->
                if (p.id == productId) p.copy(available = available) else p
            }
        _catalogFlow.value = current.copy(products = updated)
    }

    fun addProduct(
        name: String,
        price: Int,
    ) {
        val current = _catalogFlow.value
        val newId = (current.products.maxOfOrNull { it.id } ?: 0) + 1
        val formattedPrice = String.format("%.2f", price.toDouble())
        val newProduct =
            Product(
                id = newId,
                categoryId = 1,
                preparationStation = PreparationStation.CASHIER,
                name = name,
                description = "Agregado en caja",
                ingredients = "",
                allergens = "",
                estimatedTimeMinutes = 3,
                counterPrice = formattedPrice,
                digitalPrice = formattedPrice,
                available = true,
                imageUrl = "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=400&q=80",
                optionGroups = emptyList(),
            )
        _catalogFlow.value = current.copy(products = listOf(newProduct) + current.products)
    }

    fun updateOrderState(
        orderId: String,
        nextState: OrderState,
    ) {
        val list = _ordersFlow.value.toMutableList()
        val index = list.indexOfFirst { it.summary.id == orderId }
        if (index != -1) {
            val order = list[index]
            val updated =
                order.copy(
                    summary =
                        order.summary.copy(
                            state = nextState,
                            version = order.summary.version + 1,
                        ),
                )
            list[index] = updated
            _ordersFlow.value = list
        }
    }
}
