package com.vaiinilla.app.jvmroles

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileNotFoundException

class TestSetupHeadersTest {
    @get:Rule
    val home = TemporaryFolder()

    @Test
    fun `en la nube sin archivo la peticion sale sin header`() {
        assertEquals(emptyMap<String, String>(), testSetupHeaders(home.root, mapOf("CLAUDE_CODE_REMOTE" to "true")))
    }

    @Test(expected = FileNotFoundException::class)
    fun `en la Mac sin archivo falla en vez de mandar la peticion sin secreto`() {
        testSetupHeaders(home.root, emptyMap())
    }
}
