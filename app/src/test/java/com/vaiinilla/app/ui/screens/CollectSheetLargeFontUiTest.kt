package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import com.vaiinilla.app.domain.repository.AccountOrder
import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * La hoja "Cobrar cuenta" en un teléfono chico con la letra al 115 % (I04, I06, D06, D07): el botón de cobrar
 * quedaba debajo del borde y la hoja no se desplazaba. Ahora el contenido se desplaza y el botón queda fijo.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h568dp-normal-long-notround-any-xhdpi", sdk = [33])
class CollectSheetLargeFontUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun order(
        folio: Int,
        total: String,
        payer: String,
    ) = AccountOrder(
        id = "o-$folio",
        folio = folio,
        state = "entregado",
        total = total,
        clientName = payer,
        itemsSummary = "2× Taco de cochinita, 1× Agua de jamaica",
        payAtEnd = true,
        pending = true,
        payer = payer,
    )

    @Test
    fun `con letra grande el botón de cobrar se ve y cobra en efectivo lo escrito sin decimales`() {
        var charged: Pair<AccountPaymentMethod, String?>? = null
        composeTestRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1.15f)) {
                ScreenshotTheme {
                    val colors = rememberOperationalColors()
                    Box(Modifier.fillMaxSize().background(colors.background)) {
                        CollectAccountContent(
                            total = "300.00",
                            payable =
                                listOf(
                                    order(101, "120.00", "Ana"),
                                    order(102, "100.00", "Luis"),
                                    order(103, "80.00", "Sofía"),
                                ),
                            confirming = false,
                            colors = colors,
                            onConfirm = { method, received, _, _, _ -> charged = method to received },
                            remaining = "300.00",
                            onAbono = { _, _, _, _, _, _, _ -> },
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Otro monto").performTextReplacement("500")
        composeTestRule
            .onNodeWithText("Cobrar $300.00")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(AccountPaymentMethod.CASH to "500.00", charged)
    }

    @Test
    fun `la renta de una cancha también deja el botón a la vista`() {
        var received: String? = null
        composeTestRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1.15f)) {
                ScreenshotTheme {
                    val colors = rememberOperationalColors()
                    Box(Modifier.fillMaxSize().background(colors.background)) {
                        CollectAccountContent(
                            total = "250.00",
                            payable = emptyList(),
                            confirming = false,
                            colors = colors,
                            title = "Rentar Cancha 2 · 1 h 30 min",
                            allowTerminal = false,
                            allowTip = false,
                            onConfirm = { _, cash, _, _, _ -> received = cash },
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Otro monto").performTextReplacement("300,5")
        composeTestRule.onNodeWithText("Cobrar $250.00").assertIsDisplayed().performClick()

        assertEquals("300.50", received)
    }
}
