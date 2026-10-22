package com.vaiinilla.app.data.guardianship

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentContextGrant
import com.vaiinilla.app.domain.guardianship.DependentControls
import com.vaiinilla.app.domain.guardianship.GuardianLinkCode
import com.vaiinilla.app.domain.guardianship.GuardianshipException
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.PrivacyNotice
import com.vaiinilla.app.domain.guardianship.RefundMethod
import com.vaiinilla.app.domain.guardianship.StudentRequest
import com.vaiinilla.app.domain.guardianship.StudentRequestKind
import java.math.BigDecimal
import java.util.UUID

class RemoteGuardianshipRepository(
    private val apiClient: VaiinillaApiClient,
    private val json: GuardianshipJson = GuardianshipJson(),
) : GuardianshipRepository {
    override fun dependents(): Result<List<Dependent>> =
        apiClient.get(BASE).mapCatching(json::parseDependents).mapGuardianshipErrors()

    override fun privacyNotice(): Result<PrivacyNotice> =
        apiClient.get("$BASE/consentimiento").mapCatching(json::parsePrivacyNotice).mapGuardianshipErrors()

    override fun acceptPrivacyNotice(
        version: String,
        idempotencyKey: String,
    ): Result<List<Dependent>> =
        apiClient
            .post("$BASE/consentimiento", json.encodeConsent(version), key(idempotencyKey))
            .mapCatching(json::parseDependents)
            .mapGuardianshipErrors()

    override fun addDependent(
        name: String,
        studentId: String,
        group: String?,
        consentVersion: String,
        idempotencyKey: String,
    ): Result<Dependent> =
        apiClient
            .post(
                "$BASE/dependientes",
                json.encodeNewDependent(name, studentId, group, consentVersion),
                key(idempotencyKey),
            ).mapCatching(json::parseDependent)
            .mapGuardianshipErrors()

    override fun link(
        code: String,
        consentVersion: String,
        idempotencyKey: String,
    ): Result<Dependent> =
        apiClient
            .post("$BASE/vincular", json.encodeLink(code, consentVersion), key(idempotencyKey))
            .mapCatching(json::parseDependent)
            .mapGuardianshipErrors()

    override fun updateControls(
        dependentId: String,
        controls: DependentControls,
        idempotencyKey: String,
    ): Result<Dependent> =
        apiClient
            .patch("$BASE/$dependentId", json.encodeControls(controls), key(idempotencyKey))
            .mapCatching(json::parseDependent)
            .mapGuardianshipErrors()

    override fun updateLowBalanceAlert(
        dependentId: String,
        amount: BigDecimal?,
        idempotencyKey: String,
    ): Result<Dependent> =
        apiClient
            .patch("$BASE/$dependentId", json.encodeLowBalanceAlert(amount), key(idempotencyKey))
            .mapCatching(json::parseDependent)
            .mapGuardianshipErrors()

    override fun createGuardianCode(
        dependentId: String,
        idempotencyKey: String,
    ): Result<GuardianLinkCode> =
        apiClient
            .postWithoutBody("$BASE/$dependentId/codigo-tutor", key(idempotencyKey))
            .mapCatching(json::parseLinkCode)
            .mapGuardianshipErrors()

    override fun removeGuardian(
        dependentId: String,
        guardianshipId: String,
        idempotencyKey: String,
    ): Result<Dependent> =
        apiClient
            .delete("$BASE/$dependentId/tutores/$guardianshipId", headers = key(idempotencyKey))
            .mapCatching(json::parseDependent)
            .mapGuardianshipErrors()

    override fun makePrincipal(
        dependentId: String,
        guardianshipId: String,
        idempotencyKey: String,
    ): Result<Dependent> =
        apiClient
            .postWithoutBody("$BASE/$dependentId/tutores/$guardianshipId/principal", key(idempotencyKey))
            .mapCatching(json::parseDependent)
            .mapGuardianshipErrors()

    override fun unlink(
        dependentId: String,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient.delete("$BASE/$dependentId", headers = key(idempotencyKey)).map { }.mapGuardianshipErrors()

    override fun createLinkCode(idempotencyKey: String): Result<GuardianLinkCode> =
        apiClient
            .postWithoutBody("$BASE/codigos-vinculo", key(idempotencyKey))
            .mapCatching(json::parseLinkCode)
            .mapGuardianshipErrors()

    override fun issueContext(dependentId: String): Result<DependentContextGrant> =
        apiClient
            // El contexto se emite cada vez que vence: no es una escritura que se repita igual.
            .postWithoutBody("$BASE/$dependentId/contexto", key(UUID.randomUUID().toString()))
            .mapCatching(json::parseContext)
            .mapGuardianshipErrors()

    override fun myControls(): Result<MyGuardianControls?> =
        apiClient.get("$BASE/mis-controles").mapCatching(json::parseMyControls).mapGuardianshipErrors()

    override fun createAccountCode(
        dependentId: String,
        idempotencyKey: String,
    ): Result<GuardianLinkCode> =
        apiClient
            .postWithoutBody("$BASE/$dependentId/codigo-cuenta", key(idempotencyKey))
            .mapCatching(json::parseLinkCode)
            .mapGuardianshipErrors()

    override fun claimAccount(
        code: String,
        idempotencyKey: String,
    ): Result<String> =
        apiClient
            .post("$BASE/reclamar-cuenta", json.encodeCode(code), key(idempotencyKey))
            .mapCatching(json::parseClaimedName)
            .mapGuardianshipErrors()

    override fun studentRequests(dependentId: String): Result<List<StudentRequest>> =
        apiClient.get("$BASE/$dependentId/solicitudes").mapCatching(json::parseStudentRequests).mapGuardianshipErrors()

    override fun requestForStudent(
        dependentId: String,
        kind: StudentRequestKind,
        refundMethod: RefundMethod?,
        note: String?,
        idempotencyKey: String,
    ): Result<StudentRequest> =
        apiClient
            .post(
                "$BASE/$dependentId/solicitudes",
                json.encodeStudentRequest(kind, refundMethod, note),
                key(idempotencyKey),
            ).mapCatching(json::parseStudentRequest)
            .mapGuardianshipErrors()

    override fun setPin(
        dependentId: String,
        pin: String?,
        idempotencyKey: String,
    ): Result<Unit> {
        val path = "$BASE/$dependentId/pin"
        val call =
            if (pin != null) {
                apiClient.put(path, json.encodePin(pin), key(idempotencyKey))
            } else {
                apiClient.delete(path, headers = key(idempotencyKey))
            }
        return call.map { }.mapGuardianshipErrors()
    }

    override fun blockCredentials(
        dependentId: String,
        idempotencyKey: String,
    ): Result<Int> =
        apiClient
            .postWithoutBody("$BASE/$dependentId/credenciales/bloquear", key(idempotencyKey))
            .mapCatching(json::parseBlocked)
            .mapGuardianshipErrors()

    private fun key(idempotencyKey: String) = mapOf("Idempotency-Key" to idempotencyKey)

    private companion object {
        const val BASE = "tutelas"
    }
}

internal fun <T> Result<T>.mapGuardianshipErrors(): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { error ->
            Result.failure(
                if (error is ApiClientException) {
                    GuardianshipException(error.code, error.message ?: error.code, error.httpStatus, error)
                } else {
                    error
                },
            )
        },
    )
