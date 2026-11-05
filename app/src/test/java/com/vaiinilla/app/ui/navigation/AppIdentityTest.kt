package com.vaiinilla.app.ui.navigation

import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIdentityTest {
    private fun session(verified: Boolean) =
        StudentAuthSession(uid = "u1", email = "a@b.mx", displayName = "Ana", emailVerified = verified)

    private val cashier =
        AuthorizedModeContext(
            role = OperationalRole.CASHIER,
            establishmentId = "e1",
            establishmentName = "Café",
            membershipId = "m1",
            accessToken = "t",
        )

    @Test
    fun `the account stage follows session, verification and checkout context`() {
        assertEquals(AccountStage.ANONYMOUS, appIdentity(null, { true }, null).stage)
        assertEquals(AccountStage.UNVERIFIED, appIdentity(session(false), { true }, null).stage)
        assertEquals(AccountStage.VERIFIED, appIdentity(session(true), { false }, null).stage)
        assertEquals(AccountStage.READY, appIdentity(session(true), { true }, null).stage)
    }

    @Test
    fun `checkout readiness is not asked before the email is verified`() {
        var asked = false
        appIdentity(session(false), { true.also { asked = true } }, null)
        assertFalse(asked)
    }

    @Test
    fun `a staff mode keeps the client account but is not the client shell`() {
        val identity = appIdentity(session(true), { true }, cashier)
        assertTrue(identity.canUseClientAccount)
        assertFalse(identity.isClient)
        assertEquals(OperationalRole.CASHIER, identity.staffRole)
        assertTrue(appIdentity(session(true), { true }, null).isClient)
    }
}
