package com.vaiinilla.app

import com.vaiinilla.app.data.mode.AuthorizedAccessApi
import com.vaiinilla.app.data.mode.RemoteAuthorizedAccessRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteAuthorizedAccessRepositoryTest {
    private val session =
        StudentAuthSession(
            uid = "firebase-user",
            email = "persona@ejemplo.com",
            displayName = "Persona",
            emailVerified = true,
        )

    @Test
    fun `remote access list maps Android roles and omits administration`() =
        runTest {
            val api = RecordingAuthorizedAccessApi()
            api.accessResponse = accessResponse()
            val repository = RemoteAuthorizedAccessRepository(api, FakeStudentAuthRepository(session))

            val modes = repository.authorizedModes(session).getOrThrow()

            assertEquals(
                listOf(OperationalRole.CLIENT, OperationalRole.CASHIER, OperationalRole.KITCHEN),
                modes.map { it.role },
            )
            assertEquals("firebase-id-token", api.listToken)
        }

    @Test
    fun `remote activation validates membership and stores canonical context`() =
        runTest {
            val api = RecordingAuthorizedAccessApi()
            api.accessResponse = accessResponse()
            api.contextResponse = contextResponse("membership-cashier", "cajero")
            val repository = RemoteAuthorizedAccessRepository(api, FakeStudentAuthRepository(session))
            val mode = repository.authorizedModes(session).getOrThrow().first { it.role == OperationalRole.CASHIER }

            val context = repository.activateMode(mode, session).getOrThrow()

            assertEquals("membership-cashier", context.membershipId)
            assertEquals("server-jwt", context.accessToken)
            assertEquals(900, context.expiresIn)
            assertEquals("membership-cashier", api.activatedMembership)
            assertEquals("firebase-id-token", api.activationToken)
        }

    @Test
    fun `remote activation rejects a forged mode before context exchange`() =
        runTest {
            val api = RecordingAuthorizedAccessApi()
            api.accessResponse = accessResponse()
            val repository = RemoteAuthorizedAccessRepository(api, FakeStudentAuthRepository(session))
            val forged =
                AuthorizedMode(
                    role = OperationalRole.WAITER,
                    establishmentId = "establishment-a",
                    establishmentName = "Cafetería",
                    membershipId = "membership-not-owned",
                )

            val result = repository.activateMode(forged, session)

            assertTrue(result.isFailure)
            assertFalse(api.contextWasCalled)
        }

    @Test
    fun `acceptance uses stable idempotency key and does not preview fake metadata`() =
        runTest {
            val api = RecordingAuthorizedAccessApi()
            api.acceptResponse = invitationAcceptedResponse()
            val repository = RemoteAuthorizedAccessRepository(api, FakeStudentAuthRepository(session))

            val preview = repository.invitation("opaque-token").getOrThrow()
            val first = repository.acceptInvitation("opaque-token", session).getOrThrow()
            val second = repository.acceptInvitation("opaque-token", session).getOrThrow()

            assertTrue(preview.requiresRemoteAcceptance)
            assertTrue(preview.role == null)
            assertEquals(OperationalRole.KITCHEN, first.role)
            assertEquals(first, second)
            assertEquals(api.acceptIdempotencyKeys.first(), api.acceptIdempotencyKeys.last())
        }

    private fun accessResponse(): String =
        """
        {
          "data": [
            {
              "membresia_id": "membership-client",
              "establecimiento": {"id": "establishment-a", "nombre": "Cafetería", "slug": "cafeteria"},
              "rol": "cliente",
              "identificador_cliente": "A012",
              "estado_establecimiento": "activo",
              "cierre_operativo_disponible": false
            },
            {
              "membresia_id": "membership-cashier",
              "establecimiento": {"id": "establishment-a", "nombre": "Cafetería", "slug": "cafeteria"},
              "rol": "cajero",
              "identificador_cliente": null,
              "estado_establecimiento": "activo",
              "cierre_operativo_disponible": true
            },
            {
              "membresia_id": "membership-kitchen",
              "establecimiento": {"id": "establishment-a", "nombre": "Cafetería", "slug": "cafeteria"},
              "rol": "cocina",
              "identificador_cliente": null,
              "estado_establecimiento": "activo",
              "cierre_operativo_disponible": false
            },
            {
              "membresia_id": "membership-admin",
              "establecimiento": {"id": "establishment-a", "nombre": "Cafetería", "slug": "cafeteria"},
              "rol": "admin",
              "identificador_cliente": null,
              "estado_establecimiento": "activo",
              "cierre_operativo_disponible": true
            }
          ],
          "meta": {},
          "error": null
        }
        """.trimIndent()

    private fun contextResponse(
        membershipId: String,
        role: String,
    ): String =
        """
        {
          "data": {
            "access_token": "server-jwt",
            "token_type": "Bearer",
            "expires_in": 900,
            "contexto": {
              "usuario_id": "user-id",
              "membresia_id": "$membershipId",
              "establecimiento_id": "establishment-a",
              "rol": "$role",
              "modo_restringido": null
            }
          },
          "meta": {},
          "error": null
        }
        """.trimIndent()

    private fun invitationAcceptedResponse(): String =
        """
        {
          "data": {
            "invitacion_id": "invitation-id",
            "membresia": {
              "id": "membership-kitchen",
              "establecimiento_id": "establishment-a",
              "rol": "cocina",
              "activo": true
            },
            "aceptada_en": "2026-08-01T12:00:00.000Z"
          },
          "meta": {},
          "error": null
        }
        """.trimIndent()

    private class RecordingAuthorizedAccessApi : AuthorizedAccessApi {
        var accessResponse: String = ""
        var contextResponse: String = ""
        var acceptResponse: String = ""
        var listToken: String? = null
        var activationToken: String? = null
        var activatedMembership: String? = null
        var contextWasCalled = false
        val acceptIdempotencyKeys = mutableListOf<String>()

        override fun listAccess(firebaseIdToken: String): Result<String> {
            listToken = firebaseIdToken
            return Result.success(accessResponse)
        }

        override fun acceptInvitation(
            firebaseIdToken: String,
            token: String,
            idempotencyKey: String,
        ): Result<String> {
            acceptIdempotencyKeys += idempotencyKey
            return Result.success(acceptResponse)
        }

        override fun activateContext(
            firebaseIdToken: String,
            membershipId: String,
            idempotencyKey: String,
        ): Result<String> {
            contextWasCalled = true
            activationToken = firebaseIdToken
            activatedMembership = membershipId
            return Result.success(contextResponse)
        }
    }

    private class FakeStudentAuthRepository(
        private val session: StudentAuthSession,
    ) : StudentAuthRepository {
        override fun peekSession(): StudentAuthSession = session

        override fun isReadyForCheckout(establishmentId: String?): Boolean = false

        override suspend fun signUp(
            email: String,
            password: String,
            displayName: String,
        ): Result<StudentAuthSession> =
            Result.failure(
                UnsupportedOperationException(),
            )

        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<StudentAuthSession> =
            Result.failure(
                UnsupportedOperationException(),
            )

        override suspend fun sendEmailVerification(): Result<Unit> = Result.success(Unit)

        override suspend fun reloadSession(): Result<StudentAuthSession?> = Result.success(session)

        override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun getIdToken(forceRefresh: Boolean): Result<String> = Result.success("firebase-id-token")

        override suspend fun signOut() = Unit
    }
}
