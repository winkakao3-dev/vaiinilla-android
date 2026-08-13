package com.vaiinilla.app.data.auth.student

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseAuthMessagesTest {
    @Test
    fun `unknown errors keep a generic Spanish fallback`() {
        assertEquals(
            "No se pudo completar la autenticación.",
            firebaseAuthUserMessage(IllegalStateException("")),
        )
    }
}
