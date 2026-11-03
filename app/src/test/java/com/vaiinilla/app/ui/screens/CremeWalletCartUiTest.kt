package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
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
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
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
    fun `Cartera de una tienda trae el saldo, Recargar, Mi codigo y los ultimos movimientos`() {
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

        // Como el dibujo de Doop (E02): título, el saldo de este lugar, Recargar y Mi código, y los últimos movimientos.
        composeTestRule.onNodeWithText("Cartera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saldo en Usagi (Pruebas)").assertIsDisplayed()
        composeTestRule.onNodeWithText("\$140").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wallet-recharge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mi código").assertIsDisplayed()
        composeTestRule.onNodeWithText("MOVIMIENTOS").assertIsDisplayed()
        // Lo de la cartera vieja ya no sale: la foto del lugar, la pill Cuenta y la fila de datos.
        composeTestRule.onAllNodesWithText("Saldo Vaiinilla").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Último movimiento").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("MIS HIJOS").assertCountEquals(0)

        // «Ver todos» abre Movimientos (E04) con sus filtros.
        composeTestRule.onNodeWithText("Ver todos").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Movimientos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Todo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cashback").assertExists()
    }

    @Test
    fun `Cartera sin movimientos invita a ver el menu`() {
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

        composeTestRule.onNodeWithText("Ver el menú").assertIsDisplayed()
        // Sin movimientos no hay «Este mes gastaste…» ni «Ver todos».
        composeTestRule.onAllNodesWithText("Ver todos").assertCountEquals(0)
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
            composeTestRule.onAllNodesWithText("+ Agregar").fetchSemanticsNodes().isNotEmpty()
        }

        // Mis hijos se ve sin hacer scroll, con «+ Agregar» a la derecha (Doop E02).
        composeTestRule.onAllNodesWithText("MIS HIJOS").assertCountEquals(1)
        composeTestRule.onNodeWithText("MIS HIJOS").assertIsDisplayed()
        composeTestRule.onNodeWithText("+ Agregar").assertIsDisplayed()
    }

    @Test
    fun `Cartera del alumno con tutor trae lo que le queda hoy, el candado y Otro tutor`() {
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

        // Como el dibujo de Doop (E14): el anillo de hoy, su saldo con quién lo cuida y lo que tiene candado.
        composeTestRule.onNodeWithContentDescription("Hoy te quedan \$32 de \$60").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu saldo: \$140 · te cuida Rosa").assertIsDisplayed()
        composeTestRule.onNodeWithText("CON CANDADO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Por tu alergia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lo decidió Rosa").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("1 producto").assertCountEquals(2)
        composeTestRule.onNodeWithText("Mostrar mi código en Caja").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wallet-school-link-code").assertExists()
        composeTestRule.onNodeWithTag("wallet-school-help").assertExists()
    }

    @Test
    fun `captura oscura de la Cartera y de Movimientos para compararlas con Doop E02 y E04`() {
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                WalletScreen(
                    remoteState = BreakUiFixtures.wallet(BreakUiData.DEMO),
                    userId = "u-1",
                    venueName = "Usagi (Pruebas)",
                    onMenu = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("wallet/e02_cartera_oscura.png")

        composeTestRule.onNodeWithText("Ver todos").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("wallet/e04_movimientos_oscura.png")
    }

    @Test
    fun `captura oscura de la Cartera del alumno para compararla con Doop E14`() {
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
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
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
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("wallet/e14_alumno_oscura.png")
    }

    @Test
    fun `Carrito vacio trae la portada, Ver menu y Cartera`() {        var wallet = 0
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
