package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.ui.parentmode.ChildAccountContent
import com.vaiinilla.app.ui.parentmode.ChildAccountState
import com.vaiinilla.app.ui.parentmode.ChildrenViewModel
import com.vaiinilla.app.ui.parentmode.FakeAccount
import com.vaiinilla.app.ui.parentmode.FakeCatalog
import com.vaiinilla.app.ui.parentmode.FakeGuardianship
import com.vaiinilla.app.ui.parentmode.ParentFixtures
import com.vaiinilla.app.ui.parentmode.ParentModeWalletHost
import com.vaiinilla.app.ui.screenshot.BreakUiData
import com.vaiinilla.app.ui.screenshot.BreakUiFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/** Cartera, Carrito y la ficha del hijo con el estilo CREME: portada, pills y fila de datos. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class CremeWalletCartUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Cartera de una tienda trae el saldo, sus pills y la fila de datos`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                WalletScreen(
                    remoteState = BreakUiFixtures.wallet(BreakUiData.DEMO),
                    userId = "u-1",
                    venueName = "Usagi (Pruebas)",
                    onMenu = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Saldo Vaiinilla").assertIsDisplayed()
        composeTestRule.onNodeWithText("\$140").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vale para comer en Usagi (Pruebas)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wallet-recharge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cuenta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gastado este mes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recargas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Último movimiento").assertIsDisplayed()
        composeTestRule.onNodeWithText("Movimientos recientes").assertIsDisplayed()
        // Ni la pill Movimientos ni nada de la escuela en una tienda normal.
        composeTestRule.onAllNodesWithText("Movimientos").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Mis hijos").assertCountEquals(0)
    }

    @Test
    fun `Cartera sin movimientos deja la fila de datos en guion`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                WalletScreen(
                    remoteState = BreakUiFixtures.wallet(BreakUiData.VACIO),
                    userId = "u-1",
                    onMenu = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("—").assertCountEquals(3)
        composeTestRule.onNodeWithText("Ver el menú").assertIsDisplayed()
    }

    @Test
    fun `Cartera del tutor trae Mis hijos a la vista y sus cifras`() {
        val guardianship = FakeGuardianship(listOf(ParentFixtures.ana, ParentFixtures.tilin))
        val viewModel =
            ChildrenViewModel(
                guardianship,
                { FakeAccount(ParentFixtures.wallet) },
                FakeCatalog(),
                ApplicationProvider.getApplicationContext(),
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                ParentModeWalletHost(
                    enabled = true,
                    childrenViewModel = viewModel,
                    hasTutors = false,
                    onFullScreenChange = {},
                    onAccountClaimed = {},
                ) { family ->
                    WalletScreen(
                        remoteState = BreakUiFixtures.wallet(BreakUiData.DEMO),
                        userId = "u-1",
                        venueName = "Usagi (Pruebas)",
                        onMenu = {},
                        familySection = family,
                    )
                }
            }
        }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Agregar otro hijo").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Tu saldo · el de tus hijos va aparte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hijos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saldo de tus hijos").assertIsDisplayed()
        // Mis hijos se ve sin hacer scroll y ya no es una pill.
        composeTestRule.onAllNodesWithText("Mis hijos").assertCountEquals(1)
        composeTestRule.onNodeWithText("Mis hijos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar otro hijo").assertIsDisplayed()
    }

    @Test
    fun `Cartera del alumno con tutor trae Otro tutor, Como funciona y sus limites`() {
        val controls =
            MyGuardianControls(
                tutorNames = listOf("Rosa"),
                dailyLimit = BigDecimal("60.00"),
                availableToday = BigDecimal("32.00"),
                blockedProductIds = setOf(1),
                allergenProductIds = setOf(2),
                allergens = emptyList(),
            )
        val viewModel =
            ChildrenViewModel(
                FakeGuardianship(),
                { FakeAccount(ParentFixtures.wallet) },
                FakeCatalog(),
                ApplicationProvider.getApplicationContext(),
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                ParentModeWalletHost(
                    enabled = true,
                    childrenViewModel = viewModel,
                    hasTutors = true,
                    onFullScreenChange = {},
                    onAccountClaimed = {},
                ) { family ->
                    WalletScreen(
                        remoteState = BreakUiFixtures.wallet(BreakUiData.DEMO),
                        userId = "u-1",
                        venueName = "Usagi (Pruebas)",
                        onMenu = {},
                        caretakerLine = controls.caretakerLine,
                        caretakerDetail = controls.walletDetail,
                        guardianControls = controls,
                        familySection = family,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Te cuida Rosa").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wallet-school-link-code").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wallet-school-help").assertIsDisplayed()
        composeTestRule.onNodeWithText("Límite al día").assertIsDisplayed()
        composeTestRule.onNodeWithText("Te quedan hoy").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 productos").assertIsDisplayed()
        composeTestRule.onNodeWithText("\$60").assertIsDisplayed()
        composeTestRule.onNodeWithText("\$32").assertIsDisplayed()
    }

    @Test
    fun `Carrito vacio trae la portada, Ver menu y Cartera`() {
        var wallet = 0
        composeTestRule.setContent {
            ScreenshotTheme {
                CartScreen(
                    state = ScreenshotFixtures.catalogLoadedState(),
                    onMenu = {},
                    onQuantityChange = { _, _ -> },
                    onNotesChange = {},
                    onDestinationChange = {},
                    onPaymentChange = {},
                    onConfirm = {},
                    venueName = "Usagi (Pruebas)",
                    onWallet = { wallet++ },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("REVISA Y CONFIRMA").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu pedido").assertIsDisplayed()
        composeTestRule.onNodeWithText("Usagi (Pruebas)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ver menú").assertIsDisplayed()
        composeTestRule.onNodeWithText("¿Qué se te antoja?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cart-wallet").performClick()
        assertEquals(1, wallet)
    }

    @Test
    fun `Carrito con pedido trae sus pills y la fila de datos, y Vaciar quita las lineas`() {
        val state = ScreenshotFixtures.cartState()
        val removed = mutableListOf<Pair<String, Int>>()
        composeTestRule.setContent {
            ScreenshotTheme {
                CartScreen(
                    state = state,
                    onMenu = {},
                    onQuantityChange = { key, delta -> removed += key to delta },
                    onNotesChange = {},
                    onDestinationChange = {},
                    onPaymentChange = {},
                    onConfirm = {},
                    venueName = "Usagi (Pruebas)",
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Agregar más").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nota para cocina").assertIsDisplayed()
        composeTestRule.onNodeWithText("Productos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pagar").assertIsDisplayed()

        composeTestRule.onNodeWithTag("cart-clear").performClick()
        assertEquals(state.cartLines.map { it.key to -it.quantity }, removed)
    }

    @Test
    fun `la ficha del hijo trae sus pills y la fila de datos sin dibujo`() {
        val state = ChildAccountState(ParentFixtures.ana, ParentFixtures.services(FakeGuardianship()))
        composeTestRule.setContent {
            ScreenshotTheme { ChildAccountContent(state = state, onChildChanged = {}, onUnlinked = {}, onBack = {}) }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ana López").assertIsDisplayed()
        composeTestRule.onNodeWithTag("child-reload").assertIsDisplayed()
        composeTestRule.onNodeWithTag("child-week").assertIsDisplayed()
        // La tercera pill queda al borde de la fila, que se desliza: existe aunque no se vea entera.
        composeTestRule.onNodeWithTag("child-controls").assertExists()
        composeTestRule.onNodeWithText("Hoy le quedan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apartado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tutores").assertIsDisplayed()
        composeTestRule.onNodeWithText("PIN en Caja").assertIsDisplayed()
        // El servidor no manda lo que le queda hoy: la columna dice «—», no se esconde.
        composeTestRule.onNodeWithText("—").assertIsDisplayed()
        composeTestRule.onNodeWithText("No").assertIsDisplayed()
    }
}
