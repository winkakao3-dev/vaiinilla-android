package com.vaiinilla.app.data.auth.student

import com.google.firebase.auth.FirebaseAuthException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseAuthMessagesTest {
    @Test
    fun `unknown errors keep a generic Spanish fallback`() {
        assertEquals(
            "No se pudo completar la autenticación.",
            firebaseAuthUserMessage(IllegalStateException("")),
        )
    }

    @Test
    fun `invalid TOTP code has an actionable Spanish message`() {
        val error = FirebaseAuthException("ERROR_INVALID_VERIFICATION_CODE", "invalid code")

        assertEquals(
            "El código de tu aplicación autenticadora no es válido. Revisa el código actual e inténtalo de nuevo.",
            firebaseAuthUserMessage(error),
        )
    }

    @Test
    fun `expired MFA challenge has an actionable Spanish message`() {
        val error = FirebaseAuthException("ERROR_INVALID_MFA_PENDING_CREDENTIAL", "expired")

        assertEquals(
            "El desafío de autenticación expiró. Inicia el proceso nuevamente.",
            firebaseAuthUserMessage(error),
        )
    }
}
