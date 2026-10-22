package com.vaiinilla.app.data.school

import com.vaiinilla.app.domain.school.DayClosure
import com.vaiinilla.app.domain.school.GuardianRequest
import com.vaiinilla.app.domain.school.MenuCalendar
import com.vaiinilla.app.domain.school.NoServiceDay
import com.vaiinilla.app.domain.school.RefundMethod
import com.vaiinilla.app.domain.school.ReservedDay
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.SchoolDates
import com.vaiinilla.app.domain.school.SchoolGuardianship
import com.vaiinilla.app.domain.school.SchoolSettings
import com.vaiinilla.app.domain.school.SchoolSettingsRules
import com.vaiinilla.app.domain.school.SchoolStudentRequest
import com.vaiinilla.app.domain.school.StudentRequestKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Contratos de Negocio › Escuela (migraciones 92, 95, 98, 99, 100, 102 y 103). */
class SchoolAdminJson {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    fun parseSettings(raw: String): SchoolSettings = json.decodeFromString<Envelope<SettingsDto>>(raw).data.toDomain()

    /**
     * Solo lo que cambia: el mínimo y el máximo de recarga no se pueden quitar, solo cambiar, así que un campo
     * que no es un monto no se manda. [joinCode] es "generar", null (quitarlo) o ausente si no se toca.
     */
    fun encodeSettings(
        settings: SchoolSettings,
        joinCode: JoinCodeChange?,
    ): String =
        buildJsonObject {
            when (joinCode) {
                JoinCodeChange.GENERATE -> put("codigo_union", "generar")
                JoinCodeChange.REMOVE -> put("codigo_union", JsonNull)
                null -> Unit
            }
            put("union_requiere_codigo", settings.joinRequiresCode)
            put("tutores_requieren_aprobacion", settings.guardiansNeedApproval)
            SchoolSettingsRules.money(settings.reloadMinimum)?.let { put("recarga_minima", it) }
            SchoolSettingsRules.money(settings.reloadMaximum)?.let { put("recarga_maxima", it) }
            put("dias_adelanto_pedido", settings.daysAhead)
            put("hora_corte_pedido", settings.orderCutoff.trim().ifEmpty { null })
            put("hora_corte_cancelacion", settings.cancelCutoff.trim().ifEmpty { null })
        }.toString()

    fun parseMenuCalendar(raw: String): MenuCalendar {
        val data = json.decodeFromString<Envelope<MenuCalendarDto>>(raw).data
        return MenuCalendar(
            today = requireNotNull(SchoolDates.parse(data.programacion.hoy)) { "El menú llegó sin 'hoy'." },
            days =
                data.dias
                    .mapNotNull { day -> SchoolDates.parse(day.fecha)?.let { it to day.productoIds } }
                    .toMap(),
            closedDays =
                data.sinServicio.mapNotNull { day ->
                    SchoolDates.parse(day.fecha)?.let { NoServiceDay(it, day.motivo) }
                },
        )
    }

    fun encodeMenu(productIds: List<Int>): String =
        buildJsonObject { put("producto_ids", buildJsonArray { productIds.sorted().forEach { add(it) } }) }.toString()

    fun encodeReason(reason: String): String = buildJsonObject { put("motivo", reason.trim()) }.toString()

    fun parseDayClosure(raw: String): DayClosure {
        val data = json.decodeFromString<Envelope<DayClosureDto>>(raw).data
        return DayClosure(cancelled = data.cancelados, refunded = data.devuelto, pending = data.sinCancelar.size)
    }

    fun parseReservedDay(raw: String): ReservedDay {
        val data = json.decodeFromString<Envelope<ReservedDayDto>>(raw).data
        return ReservedDay(
            date = requireNotNull(SchoolDates.parse(data.fecha)) { "Lo apartado llegó sin fecha." },
            orders = data.pedidos,
            closedReason = data.sinServicio,
            lines = data.productos.map { ReservedDay.Line(it.productoId, it.nombre, it.cantidad, it.pedidos) },
        )
    }

    fun parseRoster(raw: String): List<RosterStudent> =
        json.decodeFromString<Envelope<RosterDto>>(raw).data.alumnos.map {
            RosterStudent(studentId = it.matricula, name = it.nombre, group = it.grupo)
        }

    fun encodeRoster(students: List<RosterStudent>): String =
        buildJsonObject {
            put(
                "alumnos",
                buildJsonArray {
                    students.forEach { student ->
                        add(
                            buildJsonObject {
                                put("matricula", student.studentId)
                                put("nombre", student.name)
                                put("grupo", student.group)
                            },
                        )
                    }
                },
            )
        }.toString()

    fun parseRosterTotal(raw: String): Int = json.decodeFromString<Envelope<RosterTotalDto>>(raw).data.total

    fun parseGuardianRequests(raw: String): List<GuardianRequest> =
        json.decodeFromString<Envelope<List<GuardianRequestDto>>>(raw).data.map { it.toDomain() }

    fun parseGuardianships(raw: String): List<SchoolGuardianship> =
        json.decodeFromString<Envelope<List<GuardianshipDto>>>(raw).data.map { it.toDomain() }

    /** Un tipo que esta versión no conoce se omite. */
    fun parseStudentRequests(raw: String): List<SchoolStudentRequest> =
        json.decodeFromString<Envelope<List<StudentRequestDto>>>(raw).data.mapNotNull { it.toDomain() }

    enum class JoinCodeChange { GENERATE, REMOVE }
}

@Serializable
private data class Envelope<T>(
    val data: T,
)

@Serializable
private data class SettingsDto(
    @SerialName("codigo_union") val codigoUnion: String? = null,
    @SerialName("union_requiere_codigo") val unionRequiereCodigo: Boolean = false,
    @SerialName("tutores_requieren_aprobacion") val tutoresRequierenAprobacion: Boolean = true,
    @SerialName("recarga_minima") val recargaMinima: String? = null,
    @SerialName("recarga_maxima") val recargaMaxima: String? = null,
    @SerialName("dias_adelanto_pedido") val diasAdelantoPedido: Int = 0,
    @SerialName("hora_corte_pedido") val horaCortePedido: String? = null,
    @SerialName("hora_corte_cancelacion") val horaCorteCancelacion: String? = null,
) {
    fun toDomain() =
        SchoolSettings(
            joinCode = codigoUnion?.takeIf { it.isNotBlank() },
            joinRequiresCode = unionRequiereCodigo,
            guardiansNeedApproval = tutoresRequierenAprobacion,
            reloadMinimum = SchoolSettingsRules.fieldText(recargaMinima),
            reloadMaximum = SchoolSettingsRules.fieldText(recargaMaxima),
            daysAhead = diasAdelantoPedido,
            orderCutoff = horaCortePedido?.take(5).orEmpty(),
            cancelCutoff = horaCorteCancelacion?.take(5).orEmpty(),
        )
}

@Serializable
private data class MenuCalendarDto(
    val programacion: ScheduleDto,
    val dias: List<MenuDayDto> = emptyList(),
    @SerialName("sin_servicio") val sinServicio: List<ClosedDto> = emptyList(),
)

@Serializable
private data class ScheduleDto(
    val hoy: String,
)

@Serializable
private data class MenuDayDto(
    val fecha: String,
    @SerialName("producto_ids") val productoIds: List<Int> = emptyList(),
)

@Serializable
private data class ClosedDto(
    val fecha: String,
    val motivo: String,
)

@Serializable
private data class DayClosureDto(
    val cancelados: Int = 0,
    val devuelto: String = "0.00",
    @SerialName("sin_cancelar") val sinCancelar: List<PendingDto> = emptyList(),
)

@Serializable
private data class PendingDto(
    @SerialName("pedido_id") val pedidoId: String? = null,
)

@Serializable
private data class ReservedDayDto(
    val fecha: String,
    val pedidos: Int = 0,
    @SerialName("sin_servicio") val sinServicio: String? = null,
    val productos: List<ReservedLineDto> = emptyList(),
)

@Serializable
private data class ReservedLineDto(
    @SerialName("producto_id") val productoId: Int,
    val nombre: String,
    val cantidad: Int,
    val pedidos: Int = 0,
)

@Serializable
private data class RosterDto(
    val total: Int = 0,
    val alumnos: List<RosterStudentDto> = emptyList(),
)

@Serializable
private data class RosterStudentDto(
    val matricula: String,
    val nombre: String? = null,
    val grupo: String? = null,
)

@Serializable
private data class RosterTotalDto(
    val total: Int,
)

@Serializable
private data class PersonDto(
    val nombre: String? = null,
    val email: String? = null,
) {
    val displayName: String get() = nombre?.takeIf { it.isNotBlank() } ?: email ?: "Sin nombre"
}

@Serializable
private data class StudentDto(
    @SerialName("usuario_id") val usuarioId: String? = null,
    val nombre: String? = null,
    val matricula: String? = null,
    val grupo: String? = null,
    @SerialName("tiene_cuenta") val tieneCuenta: Boolean = false,
)

@Serializable
private data class GuardianRequestDto(
    @SerialName("tutela_id") val tutelaId: String,
    val tutor: PersonDto = PersonDto(),
    val alumno: StudentDto = StudentDto(),
    @SerialName("en_padron") val enPadron: Boolean = false,
    @SerialName("otros_tutores") val otrosTutores: Int = 0,
) {
    fun toDomain() =
        GuardianRequest(
            id = tutelaId,
            guardianName = tutor.displayName,
            guardianEmail = tutor.email,
            studentName = alumno.nombre ?: "Sin nombre",
            studentId = alumno.matricula,
            group = alumno.grupo,
            studentHasAccount = alumno.tieneCuenta,
            inRoster = enPadron,
            otherGuardians = otrosTutores,
        )
}

@Serializable
private data class GuardianshipDto(
    @SerialName("tutela_id") val tutelaId: String,
    val principal: Boolean = false,
    val tutor: PersonDto = PersonDto(),
    val alumno: StudentDto = StudentDto(),
) {
    fun toDomain() =
        SchoolGuardianship(
            id = tutelaId,
            guardianName = tutor.displayName,
            guardianEmail = tutor.email,
            isPrincipal = principal,
            studentUserId = alumno.usuarioId ?: tutelaId,
            studentName = alumno.nombre ?: "Sin nombre",
            studentId = alumno.matricula,
            group = alumno.grupo,
        )
}

@Serializable
private data class StudentRequestDto(
    val id: String,
    val tipo: String,
    @SerialName("como_devolver") val comoDevolver: String? = null,
    val nota: String? = null,
    @SerialName("saldo_al_pedir") val saldoAlPedir: String? = null,
    @SerialName("saldo_actual") val saldoActual: String? = null,
    val tutor: PersonDto = PersonDto(),
    val alumno: StudentDto = StudentDto(),
) {
    fun toDomain(): SchoolStudentRequest? {
        val kind = StudentRequestKind.fromWire(tipo) ?: return null
        return SchoolStudentRequest(
            id = id,
            kind = kind,
            refundMethod = RefundMethod.fromWire(comoDevolver),
            note = nota?.takeIf { it.isNotBlank() },
            balanceAtRequest = saldoAlPedir ?: "0.00",
            currentBalance = saldoActual ?: "0.00",
            guardianName = tutor.displayName,
            guardianEmail = tutor.email,
            studentName = alumno.nombre ?: "Sin nombre",
            studentId = alumno.matricula,
            group = alumno.grupo,
        )
    }
}
