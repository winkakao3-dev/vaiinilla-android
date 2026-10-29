package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class WalletReloadContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val ana = WalletClient(userId = "u-ana", name = "Ana López", contextualId = "A-102")

    @Test
    fun `el monto aparece hasta elegir al cliente y la recarga va a esa persona`() {
        val reloads = mutableListOf<Pair<String, String>>()
        composeTestRule.setContent {
            ScreenshotTheme {
                WalletReloadContent(
                    clients = listOf(ana),
                    searching = false,
                    receipt = null,
                    canReload = true,
                    colors = rememberOperationalColors(),
                    onSearch = {},
                    onScan = {},
                    onReload = { id, amount -> reloads += id to amount },
                    onDone = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Otro monto").assertDoesNotExist()

        composeTestRule.onNodeWithText("Ana López").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Recargar a Ana López").assertIsNotEnabled()

        composeTestRule.onNodeWithText("$100").performClick()
        composeTestRule.waitForIdle()
        assertEquals(emptyList<Pair<String, String>>(), reloads)

        composeTestRule.onNodeWithText("Recargar $100 a Ana López").performClick()
        assertEquals(listOf("u-ana" to "100.00"), reloads)
    }

    @Test
    fun `solo un monto positivo con centavos cuenta`() {
        assertEquals(BigDecimal("100"), walletReloadAmount("100"))
        assertEquals(BigDecimal("12.50"), walletReloadAmount(" 12.50 "))
        assertNull(walletReloadAmount("0"))
        assertNull(walletReloadAmount(""))
        assertNull(walletReloadAmount("1.234"))
        assertEquals("Recargar $50 a Ana López", walletReloadLabel(BigDecimal("50"), ana))
    }

    @Test
    fun `bajo el nombre van el correo enmascarado, el ultimo pedido y el saldo`() {
        val hoy = LocalDate.of(2026, 10, 29)
        val conDatos =
            ana.copy(maskedEmail = "a***@ejemplo.com", lastOrderAt = "2026-10-28T19:04:11.000Z", balance = "321.95")

        assertEquals("A-102 · a***@ejemplo.com · Pidió ayer", walletClientDetail(conDatos, hoy))
        assertEquals("Tiene $321.95", walletClientBalance(conDatos))
        assertEquals("A-102", walletClientDetail(ana, hoy))
        assertNull(walletClientBalance(ana))
        assertNull(walletClientDetail(ana.copy(contextualId = null), hoy))
    }

    @Test
    fun `el ultimo pedido se cuenta con el dia del negocio`() {
        val hoy = LocalDate.of(2026, 10, 29)
        // 03:30 UTC del 29 son las 21:30 del 28 en Chihuahua.
        assertEquals("Pidió ayer", walletClientLastOrder("2026-10-29T03:30:00Z", hoy))
        assertEquals("Pidió hoy", walletClientLastOrder("2026-10-29T18:00:00+00:00", hoy))
        assertEquals("Pidió el 2 oct", walletClientLastOrder("2026-10-02T18:00:00.000Z", hoy))
        assertNull(walletClientLastOrder(null, hoy))
        assertNull(walletClientLastOrder("ayer", hoy))
    }
}
