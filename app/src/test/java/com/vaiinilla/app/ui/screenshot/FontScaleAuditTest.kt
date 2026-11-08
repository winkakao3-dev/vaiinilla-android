package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.operational.QueueSync
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Clase 8 (desbordamiento / letra grande): Carrito, Caja, Cocina y Mesero con letra 1.3 y 2.0, con datos de demo y con
 * el peor caso creíble. Usa los detectores de [VisualAuditBase] (elipsis, texto cortado, palabra partida, textos
 * encimados, pantalla negra) y deja los PNG en `app/build/outputs/roborazzi/auditoria/`.
 *
 * RATCHET por pantalla: lo que ya falla hoy vive en `src/test/resources/font-scale-baseline/<pantalla>.txt`; la prueba
 * solo falla con un problema NUEVO. Para fijar o bajar el baseline en la Mac:
 *   FONT_SCALE_RECORD=1 ./gradlew :app:testDevAlumnoDebugUnitTest --tests '*FontScaleAuditTest*'
 * Sin baseline de una pantalla, su prueba se salta (no hay con qué comparar).
 */
class FontScaleAuditTest : VisualAuditBase() {
    private val record = System.getenv("FONT_SCALE_RECORD") == "1"

    private fun check(
        name: String,
        content: @Composable () -> Unit,
    ) {
        val baseline = File("src/test/resources/font-scale-baseline/$name.txt")
        assumeTrue("Sin baseline de $name: corre con FONT_SCALE_RECORD=1 en la Mac", record || baseline.exists())
        paint(content = content)
        val found =
            listOf(1.3f, 2.0f).flatMap { scale ->
                setScale(scale)
                settle()
                val label = "$name a ${(scale * 100).toInt()} %"
                val file = capture("fuente-$name-${(scale * 100).toInt()}")
                (imageProblems(file) + textProblems()).map { "$label: $it" }
            }
        if (record) {
            baseline.parentFile?.mkdirs()
            baseline.writeText(found.distinct().sorted().joinToString("\n") + if (found.isEmpty()) "" else "\n")
            return
        }
        val allowed = baseline.readLines().filter { it.isNotBlank() }.toSet()
        assertEquals(
            "Problemas NUEVOS de letra grande en $name (PNG en $OUTPUT_DIR)",
            emptyList<String>(),
            found.filterNot { it in allowed },
        )
    }

    private fun cashier(data: BreakUiData) =
        OperationalUiState(
            role = OperationalRole.CASHIER,
            orders = BreakUiFixtures.orders(data, muchos = 20),
            cashSessionOpen = true,
            queue = QueueSync.Live,
            catalog = BreakUiFixtures.catalog(data),
        )

    private fun kitchen(data: BreakUiData) =
        OperationalUiState(
            role = OperationalRole.KITCHEN,
            orders = BreakUiFixtures.orders(data, muchos = 20),
            queue = QueueSync.Live,
        )

    private fun cart(data: BreakUiData): @Composable () -> Unit {
        val state = BreakUiFixtures.cartState(data)
        return {
            ScreenshotWithStudentNav(activeTab = StudentTab.CART, cartCount = state.cartLines.sumOf { it.quantity }) {
                CartScreen(
                    state = state,
                    onMenu = {},
                    onQuantityChange = { _, _ -> },
                    onNotesChange = {},
                    onDestinationChange = {},
                    onPaymentChange = {},
                    onConfirm = {},
                    walletBalance = null,
                )
            }
        }
    }

    private fun cashierScreen(data: BreakUiData): @Composable () -> Unit {
        val state = cashier(data)
        return {
            CashierOperationalScreen(
                state = state,
                onBack = {},
                onOpenCashSession = {},
                onCollect = { _, _, _, _ -> },
                onChangeMode = {},
            )
        }
    }

    private fun kitchenScreen(data: BreakUiData): @Composable () -> Unit {
        val state = kitchen(data)
        return {
            KitchenOperationalScreen(
                state = state,
                onBack = {},
                onStart = { _, _ -> },
                onMarkReady = { _, _ -> },
                onChangeMode = {},
            )
        }
    }

    private fun waiterScreen(data: BreakUiData): @Composable () -> Unit {
        val state = BreakUiFixtures.waiterBoard(data)
        return {
            WaiterOperationalScreen(
                state = state,
                onBack = {},
                placeName = "Cafetería y Tienda Escolar del Instituto Tecnológico",
            )
        }
    }

    @Test fun `carrito demo`() = check("carrito-demo", cart(BreakUiData.DEMO))

    @Test fun `carrito peor caso`() = check("carrito-peor", cart(BreakUiData.PEOR))

    @Test fun `caja demo`() = check("caja-demo", cashierScreen(BreakUiData.DEMO))

    @Test fun `caja peor caso`() = check("caja-peor", cashierScreen(BreakUiData.PEOR))

    @Test fun `cocina demo`() = check("cocina-demo", kitchenScreen(BreakUiData.DEMO))

    @Test fun `cocina peor caso`() = check("cocina-peor", kitchenScreen(BreakUiData.PEOR))

    @Test fun `mesero demo`() = check("mesero-demo", waiterScreen(BreakUiData.DEMO))

    @Test fun `mesero peor caso`() = check("mesero-peor", waiterScreen(BreakUiData.PEOR))
}
