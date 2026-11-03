package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.ui.screens.KitchenSoldOutContent
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import com.vaiinilla.app.ui.screens.WaiterSheetOrderRow
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Las funciones nuevas del personal (N03 a N12 del lienzo de Doop) en el tema oscuro del dibujo. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w390dp-h844dp-normal-long-notround-any-xxhdpi",
    sdk = [33],
)
class PersonalDoopScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent { ScreenshotTheme(mode = VaiinillaThemeMode.Dark, content = content) }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(name)
    }

    @Composable
    private fun OnPaper(content: @Composable () -> Unit) {
        val colors = rememberOperationalColors()
        Box(Modifier.fillMaxSize().background(colors.background).padding(16.dp)) { content() }
    }

    @Test
    fun `n03_se_acabo`() {
        val template = ScreenshotFixtures.catalog().products.first()
        val products =
            listOf(
                template.copy(id = 1, name = "Crepa de cajeta", preparationStation = PreparationStation.KITCHEN),
                template.copy(id = 2, name = "Latte frío", preparationStation = PreparationStation.KITCHEN, available = false),
                template.copy(id = 3, name = "Cheesecake de maracuyá", preparationStation = PreparationStation.KITCHEN),
                template.copy(id = 4, name = "Agua de jamaica", preparationStation = PreparationStation.CASHIER),
            )
        val base = ScreenshotFixtures.sampleOrder(state = OrderState.PAID)
        val queue =
            listOf(12, 13).map { folio ->
                base.copy(
                    summary = base.summary.copy(id = "o$folio", folio = folio),
                    items = listOf(base.items.first().copy(productName = "Latte frío", rejection = null)),
                )
            }
        capture("personal_n03_se_acabo.png") {
            OnPaper {
                KitchenSoldOutContent(
                    products = products,
                    failed = false,
                    busy = false,
                    queue = queue,
                    lastTurnedOff = "Latte frío",
                    errorMessage = null,
                    colors = rememberOperationalColors(),
                    onToggle = { _, _ -> },
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun `n10_te_llaman`() {
        capture("personal_n10_te_llaman.png") {
            WaiterOperationalScreen(
                state = ScreenshotFixtures.waiterBoardState().copy(filterAttending = true),
                onBack = {},
                placeName = "Cafetería Central",
            )
        }
    }

    @Test
    fun `n08_bebidas_ya`() {
        capture("personal_n08_bebidas_ya.png") {
            OnPaper {
                WaiterSheetOrderRow(
                    order =
                        BoardOrder(
                            id = "o-18",
                            folio = 18,
                            state = OrderState.PREPARING,
                            version = 1,
                            clientName = "Ana",
                            itemsSummary = "2× Coca-Cola de vidrio, 1× Agua de jamaica, 2× Crepa de cajeta",
                            updatedAt = "2026-11-03T18:00:00Z",
                        ),
                    readOnly = false,
                    canDeliver = true,
                    colors = rememberOperationalColors(),
                    onDeliver = {},
                    amount = "236.00",
                    stations =
                        mapOf(
                            "coca-cola de vidrio" to PreparationStation.CASHIER,
                            "agua de jamaica" to PreparationStation.CASHIER,
                            "crepa de cajeta" to PreparationStation.KITCHEN,
                        ),
                )
            }
        }
    }
}
