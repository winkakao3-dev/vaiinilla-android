package com.vaiinilla.app.ui.order

import android.content.Context
import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.data.guest.GuestCheckoutPolicy
import com.vaiinilla.app.data.guest.GuestOrderGateway
import com.vaiinilla.app.data.guest.GuestReservationSession
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.guest.LastOrderedPlaceStore
import com.vaiinilla.app.data.operational.StaffPresenceCoordinator
import com.vaiinilla.app.domain.usecase.BuildCreateOrderRequestUseCase
import com.vaiinilla.app.domain.usecase.CreateRemoteOrderUseCase
import com.vaiinilla.app.domain.usecase.GetCatalogUseCase
import com.vaiinilla.app.domain.usecase.GetOperationalStatusUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.RetryStripePaymentUseCase
import com.vaiinilla.app.InMemoryGuestKeyVault
import com.vaiinilla.app.ui.operational.FakeCatalog
import com.vaiinilla.app.ui.operational.FakeOrders
import com.vaiinilla.app.ui.admin.FakeStore
import com.vaiinilla.app.ui.operational.FakeReservations
import java.lang.reflect.Proxy

/** Arnés del carrito y el pago del cliente: lo que el flujo toca va en memoria; lo demás falla fuerte si se usa. */
internal object OrderFlowHarness {
    private val unsafe: Any by lazy {
        val c = Class.forName("sun.misc.Unsafe")
        val f = c.getDeclaredField("theUnsafe")
        f.isAccessible = true
        f.get(null)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> untouched(type: Class<T>): T =
        if (type.isInterface) {
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
                throw UnsupportedOperationException("Este recorrido no debía usar ${type.simpleName}.${method.name}")
            } as T
        } else {
            Class.forName("sun.misc.Unsafe").getMethod("allocateInstance", Class::class.java).invoke(unsafe, type) as T
        }

    private inline fun <reified T : Any> untouched(): T = untouched(T::class.java)

    fun viewModel(
        context: Context,
        catalog: FakeCatalog,
        orders: FakeOrders = FakeOrders(),
    ): OrderFlowViewModel =
        OrderFlowViewModel(
            getCatalog = GetCatalogUseCase(catalog),
            getOperationalStatus = GetOperationalStatusUseCase(catalog),
            buildCreateOrderRequest = BuildCreateOrderRequestUseCase(),
            createRemoteOrder = CreateRemoteOrderUseCase(orders),
            getOrder = GetOrderUseCase(orders),
            retryStripePayment = RetryStripePaymentUseCase(orders),
            discoveryRepository = untouched(),
            guestSessionStore = GuestSessionStore(context, InMemoryGuestKeyVault()),
            studentAuthRepository = untouched(),
            contextoExchange = untouched(),
            sessionStore = com.vaiinilla.app.ui.operational.FakeSessionStore(com.vaiinilla.app.ui.operational.StaffHarness.jwt()),
            staffPresenceCoordinator = untouched<StaffPresenceCoordinator>(),
            reservationRepository = FakeReservations(),
            guestOrders = untouched<GuestOrderGateway>(),
            guestSession = untouched<GuestReservationSession>(),
            guestCheckoutPolicy = untouched<GuestCheckoutPolicy>(),
            orderRepository = orders,
            lastOrderedPlaces = LastOrderedPlaceStore(context),
            sharedTables = untouched(),
            hotelRepository = untouched(),
            cashbackRules = FakeStore(),
            venueContext = VenueContextStore(),
        )
}
