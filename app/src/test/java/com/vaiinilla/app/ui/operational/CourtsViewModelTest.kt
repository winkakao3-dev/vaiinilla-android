package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.domain.repository.WaiterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/** D01–D05: con sesión, el club que se está mirando muestra sus canchas aunque el contexto de cliente sea de otro lugar. */
@OptIn(ExperimentalCoroutinesApi::class)
class CourtsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `with a session the map comes from the venue being viewed`() {
        val asked = mutableListOf<String>()
        val viewModel =
            CourtsViewModel(
                repository(
                    // El contexto de cliente es de la cafetería: ahí no hay canchas.
                    session = {
                        asked += "negocio"
                        Result.success(listOf(space(1, "Mesa 1", "mesa", null)))
                    },
                    public = { slug ->
                        asked += slug
                        Result.success(listOf(space(11, "Cancha 1", "cancha", "300.00")))
                    },
                ),
            )

        viewModel.onVisible(hasSession = true, slug = "padel-pruebas")
        val courts = waitFor { viewModel.uiState.value.courts }
        viewModel.onHidden()

        assertEquals(listOf("Cancha 1"), courts.map { it.space.name })
        assertEquals(true, courts.single().rentable)
        assertEquals("padel-pruebas", asked.first())
    }

    @Test
    fun `a failed poll keeps the courts already shown`() {
        var calls = 0
        val viewModel =
            CourtsViewModel(
                repository(
                    session = { Result.success(emptyList()) },
                    public = {
                        calls++
                        if (calls == 1) {
                            Result.success(listOf(space(11, "Cancha 1", "cancha", "300.00")))
                        } else {
                            Result.failure(IllegalStateException("sin red"))
                        }
                    },
                ),
            )

        viewModel.onVisible(hasSession = false, slug = "padel-pruebas")
        waitFor { viewModel.uiState.value.courts }
        viewModel.onVisible(hasSession = false, slug = "padel-pruebas")
        Thread.sleep(200)
        viewModel.onHidden()

        assertEquals(
            listOf("Cancha 1"),
            viewModel.uiState.value.courts
                .map { it.space.name },
        )
    }

    private fun <T> waitFor(read: () -> List<T>): List<T> {
        repeat(100) {
            val value = read()
            if (value.isNotEmpty()) return value
            Thread.sleep(20)
        }
        return read()
    }

    private fun space(
        id: Int,
        name: String,
        type: String,
        price: String?,
    ) = SpaceAvailability(
        space = TableSpace(id = id, name = name, type = type),
        state = SpaceAvailabilityState.LIBRE,
        settled = true,
        endsAt = null,
        releasesAt = null,
        remainingSeconds = null,
        graceMinutes = 5,
        pricePerHour = price,
    )

    /** Solo el mapa importa aquí; el resto del repositorio no se toca. */
    private fun repository(
        session: () -> Result<List<SpaceAvailability>>,
        public: (String) -> Result<List<SpaceAvailability>>,
    ): WaiterRepository =
        object : WaiterRepository by unsupported() {
            override fun availability() = session()

            override fun publicAvailability(slug: String) = public(slug)
        }

    private fun unsupported(): WaiterRepository =
        Proxy.newProxyInstance(WaiterRepository::class.java.classLoader, arrayOf(WaiterRepository::class.java)) {
            _,
            method,
            _,
            ->
            throw UnsupportedOperationException(method.name)
        } as WaiterRepository
}
