package com.vaiinilla.app

import com.vaiinilla.app.data.mode.AuthorizedAccessApi
import com.vaiinilla.app.data.mode.RemoteAuthorizedAccessRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRepository
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRequest
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentResult
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
            val repository = remoteRepository(api)

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
            val repository = remoteRepository(api)
            val mode = repository.authorizedModes(session).getOrThrow().first { it.role == OperationalRole.CASHIER }

            val context = repository.activateMode(mode, session).getOrThrow()

            assertEquals("membership-cashier", context.membershipId)
            assertEquals("server-jwt", context.accessToken)
            assertEquals(900, context.expiresIn)
            assertEquals(RestrictedMode.READ_ONLY, context.restrictedMode)
            assertEquals("membership-cashier", api.activatedMembership)
            assertEquals("firebase-id-token", api.activationToken)
        }

    @Test
    fun `remote activation rejects a forged mode before context exchange`() =
        runTest {
            val api = RecordingAuthorizedAccessApi()
            api.accessResponse = accessResponse()
            val repository = remoteRepository(api)
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
    fun `remote activation rejects a context from another establishment`() =
        runTest {
            val api = RecordingAuthorizedAccessApi()
            api.accessResponse = accessResponse()
            api.contextResponse =
                contextResponse("membership-cashier", "cajero").replace(
                    "establishment-a",
                    "establishment-other",
                )
            val repository = remoteRepository(api)
            val mode = repository.authorizedModes(session).getOrThrow().first { it.role == OperationalRole.CASHIER }

            val result = repository.activateMode(mode, session)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("otro establecimiento") == true)
        }

    @Test
    fun `each remote context activation gets a fresh idempotency key`() =
        runTest {
            val api = RecordingAuthorizedAccessApi()
            api.accessResponse = accessResponse()
            api.contextResponse = contextResponse("membership-cashier", "cajero")
            val repository = remoteRepository(api)
            val mode = repository.authorizedModes(session).getOrThrow().first { it.role == OperationalRole.CASHIER }

            repository.activateMode(mode, session).getOrThrow()
            repository.activateMode(mode, session).getOrThrow()

            assertEquals(2, api.contextIdempotencyKeys.size)
            assertNotEquals(api.contextIdempotencyKeys[0], api.contextIdempotencyKeys[1])
            assertTrue(api.contextIdempotencyKeys.all { it.isNotBlank() })
        }

    @Test
    fun `acceptance uses stable idempotency key and does not preview fake metadata`() =
        runTest {
            val api = RecordingAuthorizedAccessApi()
            api.acceptResponse = invitationAcceptedResponse()
            val enrollment = RecordingStudentEnrollmentRepository()
            val repository = remoteRepository(api, enrollment)

            val preview = repository.invitation("opaque-token").getOrThrow()
            val first = repository.acceptInvitation("opaque-token", session).getOrThrow()
            val second = repository.acceptInvitation("opaque-token", session).getOrThrow()

            assertTrue(preview.requiresRemoteAcceptance)
            assertTrue(preview.role == null)
            assertEquals(OperationalRole.KITCHEN, first.role)
            assertEquals(first, second)
            assertEquals(api.acceptIdempotencyKeys.first(), api.acceptIdempotencyKeys.last())
            assertEquals(2, enrollment.requests.size)
            assertEquals(listOf("firebase-id-token", "firebase-id-token"), enrollment.tokens)
        }

    @Test
    fun `remote invitation does not call acceptance when identity enrollment fails`() =
        runTest {
            val api = RecordingAuthorizedAccessApi()
            api.acceptResponse = invitationAcceptedResponse()
            val enrollment = RecordingStudentEnrollmentRepository()
            enrollment.failure = IllegalStateException("identity unavailable")
            val repository = remoteRepository(api, enrollment)

            val result = repository.acceptInvitation("opaque-token", session)

            assertTrue(result.isFailure)
            assertTrue(api.acceptIdempotencyKeys.isEmpty())
        }

    private fun remoteRepository(
        api: RecordingAuthorizedAccessApi,
        enrollment: RecordingStudentEnrollmentRepository = RecordingStudentEnrollmentRepository(),
    ): RemoteAuthorizedAccessRepository =
        RemoteAuthorizedAccessRepository(api, FakeStudentAuthRepository(session), enrollment)

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
        restrictedMode: String = "solo_lectura",
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
              "modo_restringido": "$restrictedMode"
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
        val contextIdempotencyKeys = mutableListOf<String>()

        override suspend fun listAccess(firebaseIdToken: String): Result<String> {
            listToken = firebaseIdToken
            return Result.success(accessResponse)
        }

        override suspend fun acceptInvitation(
            firebaseIdToken: String,
            token: String,
            idempotencyKey: String,
        ): Result<String> {
            acceptIdempotencyKeys += idempotencyKey
            return Result.success(acceptResponse)
        }

        override suspend fun activateContext(
            firebaseIdToken: String,
            membershipId: String,
            idempotencyKey: String,
        ): Result<String> {
            contextWasCalled = true
            activationToken = firebaseIdToken
            activatedMembership = membershipId
            contextIdempotencyKeys += idempotencyKey
            return Result.success(contextResponse)
        }
    }

    private class RecordingStudentEnrollmentRepository : StudentEnrollmentRepository {
        val requests = mutableListOf<StudentEnrollmentRequest>()
        val tokens = mutableListOf<String>()
        var failure: Throwable? = null

        override suspend fun enroll(
            request: StudentEnrollmentRequest,
            firebaseIdToken: String,
        ): Result<StudentEnrollmentResult> {
            requests += request
            tokens += firebaseIdToken
            return failure?.let { Result.failure(it) } ?: Result.success(StudentEnrollmentResult())
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

        override suspend fun reloadSession(): Result<StudentAuthSession?> = Result.success(session)

        override suspend fun getIdToken(forceRefresh: Boolean): Result<String> = Result.success("firebase-id-token")

        override suspend fun signOut() = Unit
    }
}
