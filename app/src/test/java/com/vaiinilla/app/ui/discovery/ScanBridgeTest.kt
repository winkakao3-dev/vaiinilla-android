package com.vaiinilla.app.ui.discovery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.vaiinilla.app.ui.counter.NfcCardReaderEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Los recorridos de QR y NFC se prueban sin cámara: lo inyectado llega al mismo callback que el lector real. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScanBridgeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `el QR inyectado llega al lector abierto y se va al cerrarlo`() {
        val read = mutableListOf<String>()
        var open by mutableStateOf(true)
        composeTestRule.setContent {
            if (open) QrScannerDialog(onClose = {}, onPayload = { read += it }, title = "Apunta al QR")
        }
        composeTestRule.waitForIdle()

        assertTrue(ScanBridge.injectQr("vaiinilla:alumno:abc"))
        assertEquals(listOf("vaiinilla:alumno:abc"), read)

        open = false
        composeTestRule.waitForIdle()
        assertFalse(ScanBridge.injectQr("otro"))
        assertEquals(1, read.size)
    }

    @Test
    fun `el NFC inyectado llega solo con el lector armado`() {
        val cards = mutableListOf<String>()
        var enabled by mutableStateOf(true)
        composeTestRule.setContent { NfcCardReaderEffect(enabled = enabled) { cards += it } }
        composeTestRule.waitForIdle()

        assertTrue(ScanBridge.injectNfc("04A1B2C3"))
        assertEquals(listOf("04A1B2C3"), cards)

        enabled = false
        composeTestRule.waitForIdle()
        assertFalse(ScanBridge.injectNfc("04A1B2C3"))
    }
}
