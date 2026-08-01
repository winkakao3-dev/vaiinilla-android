package com.vaiinilla.app

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.mode.FixtureAuthorizedAccessRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class AuthorizedAccessRepositoryTest {
    private lateinit var repository: FixtureAuthorizedAccessRepository

    @Before
    fun setUp() {
        repository = FixtureAuthorizedAccessRepository()
    }

    @Test
    fun `client only account has no operational modes`() =
        runTest {
            assertTrue(repository.authorizedModes(verifiedSession()).getOrThrow().isEmpty())
        }

    @Test
    fun `valid invitation requires verified matching email and is idempotent`() =
        runTest {
            val session = verifiedSession()
            val first = repository.acceptInvitation("vai27-valid-cashier", session).getOrThrow()
            val second = repository.acceptInvitation("vai27-valid-cashier", session).getOrThrow()

            assertEquals(OperationalRole.CASHIER, first.role)
            assertEquals(first, second)
            assertEquals(1, repository.authorizedModes(session).getOrThrow().size)
        }

    @Test
    fun `accepted invitation cannot be reused by another user`() =
        runTest {
            val ana = verifiedSession(uid = "user-ana", email = "ana@vaiinilla.test")
            val other = verifiedSession(uid = "user-other", email = "ana@vaiinilla.test")

            repository.acceptInvitation("vai27-valid-cashier", ana).getOrThrow()
            assertFailureContains(
                repository.acceptInvitation("vai27-valid-cashier", other),
                "utilizada",
            )
            assertTrue(repository.authorizedModes(other).getOrThrow().isEmpty())

            val retry = repository.acceptInvitation("vai27-valid-cashier", ana).getOrThrow()
            assertEquals(OperationalRole.CASHIER, retry.role)
            assertEquals(1, repository.authorizedModes(ana).getOrThrow().size)
        }

    @Test
    fun `expired revoked unverified and mismatched invitations are rejected`() =
        runTest {
            assertFailureContains(
                repository.acceptInvitation("vai27-expired", verifiedSession()),
                "expiró",
            )
            assertFailureContains(
                repository.acceptInvitation("vai27-revoked", verifiedSession()),
                "revocada",
            )
            assertFailureContains(
                repository.acceptInvitation("vai27-valid-cashier", verifiedSession(emailVerified = false)),
                "Verifica",
            )
            assertFailureContains(
                repository.acceptInvitation("vai27-valid-cashier", verifiedSession(email = "otra@vaiinilla.test")),
                "corresponde",
            )
        }

    @Test
    fun `multiple modes can switch only among authorized contexts`() =
        runTest {
            val session = verifiedSession()
            repository.acceptInvitation("vai27-valid-cashier", session).getOrThrow()
            repository.acceptInvitation("vai27-valid-kitchen", session).getOrThrow()
            repository.acceptInvitation("vai27-valid-waiter", session).getOrThrow()
            val modes = repository.authorizedModes(session).getOrThrow()

            assertEquals(3, modes.size)
            assertNotNull(repository.activateMode(modes.first(), session).getOrThrow().accessToken)
            val unauthorized = modes.first().copy(role = OperationalRole.CLIENT)
            assertTrue(repository.activateMode(unauthorized, session).isFailure)
        }

    @Test
    fun `each mode activation issues a distinct mock context token`() =
        runTest {
            val session = verifiedSession()
            repository.acceptInvitation("vai27-valid-cashier", session).getOrThrow()
            repository.acceptInvitation("vai27-valid-kitchen", session).getOrThrow()
            val modes = repository.authorizedModes(session).getOrThrow()
            val cashier = modes.first { it.role == OperationalRole.CASHIER }
            val kitchen = modes.first { it.role == OperationalRole.KITCHEN }

            val first = repository.activateMode(cashier, session).getOrThrow()
            val second = repository.activateMode(kitchen, session).getOrThrow()
            val again = repository.activateMode(cashier, session).getOrThrow()

            assertTrue(first.accessToken.contains("cajero"))
            assertTrue(second.accessToken.contains("cocina"))
            assertEquals(first.accessToken, again.accessToken)
            assertTrue(first.accessToken != second.accessToken)
        }

    @Test
    fun `external admin revocation removes active access and leaves other modes intact`() =
        runTest {
            val session = verifiedSession()
            repository.acceptInvitation("vai27-valid-cashier", session).getOrThrow()
            repository.acceptInvitation("vai27-valid-kitchen", session).getOrThrow()
            val cashier = repository.authorizedModes(session).getOrThrow().first { it.role == OperationalRole.CASHIER }

            repository.activateMode(cashier, session).getOrThrow()
            // Revocación externa/admin (no acción del invitado en UI).
            repository.revokeMode(cashier, session).getOrThrow()
            val remaining = repository.authorizedModes(session).getOrThrow()

            assertFalse(remaining.any { it.role == OperationalRole.CASHIER })
            assertTrue(remaining.any { it.role == OperationalRole.KITCHEN })
            assertTrue(repository.activateMode(cashier, session).isFailure)
        }

    private fun verifiedSession(
        uid: String = "user-ana",
        email: String = "ana@vaiinilla.test",
        emailVerified: Boolean = true,
    ): StudentAuthSession =
        StudentAuthSession(
            uid = uid,
            email = email,
            displayName = "Ana",
            emailVerified = emailVerified,
        )

    private fun assertFailureContains(
        result: Result<*>,
        expected: String,
    ) {
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains(expected, ignoreCase = true) == true)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthorizedAccessDeepLinkTest {
    @Test
    fun `only local mock invitation URI is parsed`() {
        assertEquals(
            "vai27-valid-cashier",
            MainActivity.mockInvitationTokenFrom(Uri.parse("vaiinilla://mock/invitation/vai27-valid-cashier")),
        )
        assertEquals(
            null,
            MainActivity.mockInvitationTokenFrom(
                Uri.parse("https://vaiinilla.app/invitation/vai27-valid-cashier"),
            ),
        )
        assertEquals(
            null,
            MainActivity.mockInvitationTokenFrom(
                Uri.parse("vaiinilla://other/invitation/vai27-valid-cashier"),
            ),
        )
        assertEquals(
            "real-token",
            MainActivity.invitationTokenFrom(
                Uri.parse("https://vaiinilla.app/invitaciones/aceptar?token=real-token"),
            ),
        )
        assertEquals(
            null,
            MainActivity.invitationTokenFrom(
                Uri.parse("https://vaiinilla.app/invitaciones/aceptar"),
            ),
        )
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthorizedAccessCartIsolationTest {
    @Test
    fun `switching authorized mode preserves student cart storage`() =
        runTest {
            val store = GuestSessionStore(ApplicationProvider.getApplicationContext())
            val storageKey = store.cartStorageKey("student-establishment", null)
            val line =
                CartLine(
                    product =
                        Product(
                            id = 27,
                            categoryId = 1,
                            preparationStation = PreparationStation.KITCHEN,
                            name = "Mock bowl",
                            description = "Fixture",
                            ingredients = "Fixture",
                            allergens = "",
                            estimatedTimeMinutes = 10,
                            counterPrice = "10.00",
                            digitalPrice = "9.00",
                            available = true,
                            imageUrl = "",
                            optionGroups = emptyList(),
                        ),
                    quantity = 2,
                    selectedOptionIds = emptySet(),
                )
            store.saveCartSnapshot(storageKey, listOf(line))

            val repository = FixtureAuthorizedAccessRepository()
            val session =
                StudentAuthSession(
                    uid = "user-ana",
                    email = "ana@vaiinilla.test",
                    displayName = "Ana",
                    emailVerified = true,
                )
            repository.acceptInvitation("vai27-valid-cashier", session).getOrThrow()
            val mode = repository.authorizedModes(session).getOrThrow().single()
            repository.activateMode(mode, session).getOrThrow()

            assertEquals(1, store.readCartSnapshot(storageKey).size)
            assertEquals(2, store.readCartSnapshot(storageKey).single().quantity)
            store.clearCart(storageKey)
        }
}
