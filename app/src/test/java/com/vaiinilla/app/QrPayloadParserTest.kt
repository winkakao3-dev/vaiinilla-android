package com.vaiinilla.app

import com.vaiinilla.app.ui.discovery.QrPayload
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPayloadParserTest {
    @Test
    fun `canonical establishment QR becomes public slug`() {
        assertEquals(
            QrPayload.Establishment("cafeteria-centro"),
            QrPayloadParser.parse("https://vaiinilla.app/e/cafeteria-centro").getOrThrow(),
        )
    }

    @Test
    fun `canonical user QR becomes wallet user id`() {
        assertEquals(
            QrPayload.User("u-42"),
            QrPayloadParser.parse("https://vaiinilla.app/u/u-42").getOrThrow(),
        )
        assertEquals("https://vaiinilla.app/u/u-42", QrPayloadParser.encodeUser(" u-42 "))
    }

    @Test
    fun `space QR remains opaque token`() {
        assertEquals(
            QrPayload.SpaceToken("mesa-token-opaco"),
            QrPayloadParser.parse(" mesa-token-opaco ").getOrThrow(),
        )
    }

    @Test
    fun `empty QR is rejected`() {
        assertTrue(QrPayloadParser.parse(" ").isFailure)
    }
}
