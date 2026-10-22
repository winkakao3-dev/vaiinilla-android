package com.vaiinilla.app.domain.school

import java.time.LocalDate

/** Negocio › Escuela: solo la administración. Cada mutación lleva su `Idempotency-Key`. */
interface SchoolAdminRepository {
    fun settings(): Result<SchoolSettings>

    /** Con [regenerateJoinCode] el servidor genera un código nuevo; el anterior deja de servir. */
    fun save(
        settings: SchoolSettings,
        regenerateJoinCode: Boolean,
        idempotencyKey: String,
    ): Result<SchoolSettings>

    /** Quita el código y, con él, el requisito de usarlo. */
    fun removeJoinCode(
        settings: SchoolSettings,
        idempotencyKey: String,
    ): Result<SchoolSettings>

    /** Sin rango, de hoy al último día que se aparta; trae el "hoy" de la escuela. */
    fun menuCalendar(
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): Result<MenuCalendar>

    fun saveMenu(
        date: LocalDate,
        productIds: List<Int>,
        idempotencyKey: String,
    ): Result<Unit>

    /** Ese día no hay servicio: lo apartado se cancela y el saldo regresa. */
    fun closeDay(
        date: LocalDate,
        reason: String,
        idempotencyKey: String,
    ): Result<DayClosure>

    fun reopenDay(
        date: LocalDate,
        idempotencyKey: String,
    ): Result<Unit>

    fun reservedDay(date: LocalDate): Result<ReservedDay>

    fun roster(): Result<List<RosterStudent>>

    /** Reemplaza el padrón completo; vacío lo quita. Devuelve cuántos quedaron. */
    fun saveRoster(
        students: List<RosterStudent>,
        idempotencyKey: String,
    ): Result<Int>

    /** Tutores que esperan a la escuela, del más viejo al más nuevo. */
    fun guardianRequests(): Result<List<GuardianRequest>>

    fun reviewGuardianRequest(
        id: String,
        approve: Boolean,
        idempotencyKey: String,
    ): Result<Unit>

    /** Quién cuida a los alumnos cuyo nombre o matrícula coincide (dos letras o más). */
    fun searchGuardianships(query: String): Result<List<SchoolGuardianship>>

    fun removeGuardianship(
        id: String,
        idempotencyKey: String,
    ): Result<Unit>

    fun makePrincipal(
        id: String,
        idempotencyKey: String,
    ): Result<Unit>

    /** Devoluciones de saldo y bajas que esperan a la escuela. */
    fun studentRequests(): Result<List<SchoolStudentRequest>>

    fun resolveStudentRequest(
        id: String,
        idempotencyKey: String,
    ): Result<Unit>

    fun rejectStudentRequest(
        id: String,
        reason: String,
        idempotencyKey: String,
    ): Result<Unit>
}
