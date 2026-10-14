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
}
