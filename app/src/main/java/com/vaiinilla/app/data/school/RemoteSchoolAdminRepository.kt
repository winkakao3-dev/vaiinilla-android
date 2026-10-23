package com.vaiinilla.app.data.school

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.school.SchoolAdminJson.JoinCodeChange
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import com.vaiinilla.app.domain.school.DayClosure
import com.vaiinilla.app.domain.school.GuardianRequest
import com.vaiinilla.app.domain.school.MenuCalendar
import com.vaiinilla.app.domain.school.ReservedDay
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.SchoolAdminRepository
import com.vaiinilla.app.domain.school.SchoolGuardianship
import com.vaiinilla.app.domain.school.SchoolSettings
import com.vaiinilla.app.domain.school.SchoolStudentRequest
import java.time.LocalDate

private const val CONFIG = "establecimiento/configuracion"
private const val GUARDIAN_REQUESTS = "solicitudes-tutela"
private const val STUDENT_REQUESTS = "solicitudes-alumno"
private const val EMPTY_BODY = "{}"

class RemoteSchoolAdminRepository(
    private val apiClient: VaiinillaApiClient,
    private val json: SchoolAdminJson = SchoolAdminJson(),
) : SchoolAdminRepository {
    override fun settings(): Result<SchoolSettings> =
        apiClient.get(CONFIG).mapCatching(json::parseSettings).mapApiErrors()

    override fun save(
        settings: SchoolSettings,
        regenerateJoinCode: Boolean,
        idempotencyKey: String,
    ): Result<SchoolSettings> =
        apiClient
            .patch(
                CONFIG,
                json.encodeSettings(settings, if (regenerateJoinCode) JoinCodeChange.GENERATE else null),
                key(idempotencyKey),
            ).mapCatching(json::parseSettings)
            .mapApiErrors()

    override fun removeJoinCode(
        settings: SchoolSettings,
        idempotencyKey: String,
    ): Result<SchoolSettings> =
        apiClient
            .patch(
                CONFIG,
                // Sin código no se puede exigir: el servidor generaría otro.
                json.encodeSettings(settings.copy(joinRequiresCode = false), JoinCodeChange.REMOVE),
                key(idempotencyKey),
            ).mapCatching(json::parseSettings)
            .mapApiErrors()

    override fun menuCalendar(
        from: LocalDate?,
        to: LocalDate?,
    ): Result<MenuCalendar> {
        val query =
            if (from != null && to != null) mapOf("desde" to from.toString(), "hasta" to to.toString()) else emptyMap()
        return apiClient.get("menu-dias", query).mapCatching(json::parseMenuCalendar).mapApiErrors()
    }

    override fun saveMenu(
        date: LocalDate,
        productIds: List<Int>,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient.put("menu-dias/$date", json.encodeMenu(productIds), key(idempotencyKey)).map { Unit }.mapApiErrors()

    override fun closeDay(
        date: LocalDate,
        reason: String,
        idempotencyKey: String,
    ): Result<DayClosure> =
        apiClient
            .put("menu-dias/$date/sin-servicio", json.encodeReason(reason), key(idempotencyKey))
            .mapCatching(json::parseDayClosure)
            .mapApiErrors()

    override fun reopenDay(
        date: LocalDate,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient.delete("menu-dias/$date/sin-servicio", headers = key(idempotencyKey)).map { Unit }.mapApiErrors()

    override fun reservedDay(date: LocalDate): Result<ReservedDay> =
        apiClient.get("menu-dias/$date/apartados").mapCatching(json::parseReservedDay).mapApiErrors()

    override fun roster(): Result<List<RosterStudent>> =
        apiClient.get("padron").mapCatching(json::parseRoster).mapApiErrors()

    override fun saveRoster(
        students: List<RosterStudent>,
        idempotencyKey: String,
    ): Result<Int> =
        apiClient
            .put("padron", json.encodeRoster(students), key(idempotencyKey))
            .mapCatching(json::parseRosterTotal)
            .mapApiErrors()

    override fun guardianRequests(): Result<List<GuardianRequest>> =
        apiClient.get(GUARDIAN_REQUESTS).mapCatching(json::parseGuardianRequests).mapApiErrors()

    override fun reviewGuardianRequest(
        id: String,
        approve: Boolean,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .post("$GUARDIAN_REQUESTS/$id/${if (approve) "aprobar" else "rechazar"}", EMPTY_BODY, key(idempotencyKey))
            .map { Unit }
            .mapApiErrors()

    override fun searchGuardianships(query: String): Result<List<SchoolGuardianship>> =
        apiClient
            .get("$GUARDIAN_REQUESTS/tutores", mapOf("q" to query))
            .mapCatching(json::parseGuardianships)
            .mapApiErrors()

    override fun removeGuardianship(
        id: String,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .post("$GUARDIAN_REQUESTS/$id/quitar", EMPTY_BODY, key(idempotencyKey))
            .map { Unit }
            .mapApiErrors()

    override fun makePrincipal(
        id: String,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient.post("$GUARDIAN_REQUESTS/$id/principal", EMPTY_BODY, key(idempotencyKey)).map { Unit }.mapApiErrors()

    override fun studentRequests(): Result<List<SchoolStudentRequest>> =
        apiClient.get(STUDENT_REQUESTS).mapCatching(json::parseStudentRequests).mapApiErrors()

    override fun resolveStudentRequest(
        id: String,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient.post("$STUDENT_REQUESTS/$id/resolver", EMPTY_BODY, key(idempotencyKey)).map { Unit }.mapApiErrors()

    override fun rejectStudentRequest(
        id: String,
        reason: String,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .post("$STUDENT_REQUESTS/$id/rechazar", json.encodeReason(reason), key(idempotencyKey))
            .map { Unit }
            .mapApiErrors()

    private fun key(idempotencyKey: String) = mapOf("Idempotency-Key" to idempotencyKey)

    /** Los errores del backend llegan con su código para que la pantalla diga qué pasó. */
    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Result.failure(
                    if (error is ApiClientException) {
                        CatalogRepositoryException(error.code, error.message ?: error.code, error)
                    } else {
                        error
                    },
                )
            },
        )
}
