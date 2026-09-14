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
    fun `table QR URL becomes space token`() {
        assertEquals(
            QrPayload.SpaceToken("qr-espacio-a-mesa-4"),
            QrPayloadParser.parse("https://vaiinilla.app/demo-a/m/qr-espacio-a-mesa-4").getOrThrow(),
        )
        assertEquals(
            QrPayload.SpaceToken("qr-espacio-a-mesa-4"),
            QrPayloadParser.parse("https://www.vaiinilla.app/demo-a/m/qr-espacio-a-mesa-4").getOrThrow(),
        )
    }

    @Test
    fun `table URL with non-m middle segment falls back to opaque token`() {
        assertEquals(
            QrPayload.SpaceToken("https://vaiinilla.app/demo-a/x/tok"),
            QrPayloadParser.parse("https://vaiinilla.app/demo-a/x/tok").getOrThrow(),
        )
    }

    @Test
    fun `table URL with whitespace token falls back to opaque token`() {
        assertEquals(
            QrPayload.SpaceToken("https://vaiinilla.app/demo-a/m/qr tok"),
            QrPayloadParser.parse("https://vaiinilla.app/demo-a/m/qr tok").getOrThrow(),
        )
    }

    @Test
    fun `table URL with reserved slug falls back to opaque token`() {
        assertEquals(
            QrPayload.SpaceToken("https://vaiinilla.app/e/m/tok"),
            QrPayloadParser.parse("https://vaiinilla.app/e/m/tok").getOrThrow(),
        )
    }

    @Test
    fun `encodeSpace builds canonical table URL`() {
        assertEquals(
            "https://vaiinilla.app/demo-a/m/qr-espacio-a-mesa-4",
            QrPayloadParser.encodeSpace("demo-a", "qr-espacio-a-mesa-4"),
        )
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
