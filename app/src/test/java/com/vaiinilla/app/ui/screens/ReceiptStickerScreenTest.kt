package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReceiptStickerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // La talla también se pinta en el sticker de vista previa: el chip es el nodo que se puede tocar.
    private fun sizeChip(label: String) = composeTestRule.onNode(hasText(label) and hasClickAction())

    @Test
    fun `size chips are 48 dp wide and tall and a real tap changes the selected size`() {
        composeTestRule.setContent {
            ScreenshotTheme { ReceiptStickerScreen(order = null, onBack = {}) }
        }

        listOf("XS", "S", "M", "XL", "XXL").forEach { label ->
            sizeChip(label).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
        sizeChip("M").assertIsSelected()

        sizeChip("S").performClick()

        sizeChip("S").assertIsSelected()
    }

    @Test
    fun `receipt time is shown in the phone zone and many products are summarized`() {
        assertEquals("07:05", receiptLocalTime("2026-10-23T13:05:00Z", ZoneId.of("America/Mexico_City")))
        assertEquals("—", receiptLocalTime("no-es-fecha"))
        assertEquals("Pedido", receiptProductName(emptyList()))
        assertEquals("Café", receiptProductName(listOf("Café")))
        assertEquals("Café y 2 más", receiptProductName(listOf("Café", "Pan", "Jugo")))
    }
}
