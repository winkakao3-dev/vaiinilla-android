package com.vaiinilla.app.ui.admin.school

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import com.vaiinilla.app.domain.school.GuardianRequest
import com.vaiinilla.app.domain.school.MenuCalendar
import com.vaiinilla.app.domain.school.ReservedDay
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.SchoolAdminRepository
import com.vaiinilla.app.domain.school.SchoolDates
import com.vaiinilla.app.domain.school.SchoolGuardianship
import com.vaiinilla.app.domain.school.SchoolSettings
import com.vaiinilla.app.domain.school.SchoolSettingsRules
import com.vaiinilla.app.domain.school.SchoolStudentRequest
import com.vaiinilla.app.domain.school.StudentRequestKind
import com.vaiinilla.app.ui.components.moneyLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class AdminSchoolUiState(
    val settings: SchoolSettings? = null,
    val loadError: String? = null,
    /** De hoy (de la escuela) a 14 días; `null` mientras carga. */
    val menu: MenuCalendar? = null,
    val savingMenuDate: LocalDate? = null,
    val roster: List<RosterStudent>? = null,
    val rosterError: String? = null,
    val requests: List<GuardianRequest>? = null,
    val requestsError: String? = null,
    /** Lo que salió al buscar quién cuida a un alumno; `null` sin búsqueda. */
    val guardianships: List<SchoolGuardianship>? = null,
    val guardianshipsError: String? = null,
    /** Devoluciones de saldo y bajas que esperan a la escuela; `null` mientras carga. */
    val studentRequests: List<SchoolStudentRequest>? = null,
    val studentRequestsError: String? = null,
    /** Lo apartado de cada día que ya se abrió en la tira. */
    val reserved: Map<LocalDate, ReservedDay> = emptyMap(),
    val saving: Boolean = false,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
) {
    val today: LocalDate? get() = menu?.today

    /** Los días de la tira: de hoy al máximo que se puede apartar, siempre con fechas de la escuela. */
    val dates: List<LocalDate>
        get() = today?.let { SchoolDates.dates(it, SchoolSettingsRules.MAX_DAYS_AHEAD) }.orEmpty()

    val hasRoster: Boolean get() = roster?.isNotEmpty() == true
}

/** Negocio › Escuela: quién se une, cuánto se recarga, el padrón, los tutores, cada día y las devoluciones. */
@HiltViewModel
class AdminSchoolViewModel
    @Inject
    constructor(
        private val repository: SchoolAdminRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(AdminSchoolUiState())
        val uiState: State<AdminSchoolUiState> = _uiState
        private var scope: String? = null
        private var noticeJob: Job? = null
        private var searchJob: Job? = null
        private var guardianshipQuery = ""

        // Un reintento del mismo cuerpo reusa su llave; el backend la libera si la operación falló.
        private val pendingKeys = mutableMapOf<String, String>()

        private var state: AdminSchoolUiState
            get() = _uiState.value
            set(value) {
                _uiState.value = value
            }

        fun onVisible(scope: String) {
            if (scope != this.scope) {
                this.scope = scope
                state = AdminSchoolUiState()
                pendingKeys.clear()
            }
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                io { repository.settings() }.fold(
                    onSuccess = { state = state.copy(settings = it, loadError = null) },
                    onFailure = {
                        state =
                            state.copy(loadError = it.toUserFacingMessage("No pudimos cargar la escuela."))
                    },
                )
            }
            loadMenu()
            loadRoster()
            loadRequests()
            loadStudentRequests()
        }

        // region Ajustes

        fun save(
            updated: SchoolSettings,
            regenerateJoinCode: Boolean = false,
            onDone: () -> Unit = {},
        ) {
            SchoolSettingsRules.problem(updated)?.let { return showNotice(it, isError = true) }
            mutate(
                signature = "ajustes:$updated:$regenerateJoinCode",
                success = if (regenerateJoinCode) "Código nuevo listo" else "Guardado",
                fallback = "No se guardó.",
                call = { key -> repository.save(updated, regenerateJoinCode, key) },
            ) {
                state = state.copy(settings = it)
                onDone()
            }
        }

        fun removeJoinCode() {
            val settings = state.settings ?: return
            mutate(
                signature = "quitar-codigo",
                success = "Ya no hay código para unirse",
                fallback = "No se quitó el código.",
                call = { key -> repository.removeJoinCode(settings, key) },
            ) { state = state.copy(settings = it) }
        }

        // endregion

        // region Solicitudes de tutores

        fun loadRequests() {
            viewModelScope.launch {
                io { repository.guardianRequests() }.fold(
                    onSuccess = { state = state.copy(requests = it, requestsError = null) },
                    onFailure = {
                        state =
                            state.copy(
                                requestsError = it.toUserFacingMessage("No pudimos traer las solicitudes de tutores."),
                            )
                    },
                )
            }
        }

        fun review(
            request: GuardianRequest,
            approve: Boolean,
        ) = mutate(
            signature = "revisar:${request.id}:$approve",
            success =
                if (approve) {
                    "${request.guardianName} ya cuida a ${request.studentName}"
                } else {
                    "Solicitud rechazada"
                },
            fallback = "No se guardó la decisión.",
            call = { key -> repository.reviewGuardianRequest(request.id, approve, key) },
            onFailure = { loadRequests() },
        ) { state = state.copy(requests = state.requests?.filterNot { it.id == request.id }) }

        // endregion

        // region Padrón

        fun loadRoster() {
            viewModelScope.launch {
                io { repository.roster() }.fold(
                    onSuccess = { state = state.copy(roster = it, rosterError = null) },
                    onFailure = {
                        state =
                            state.copy(rosterError = it.toUserFacingMessage("No pudimos traer el padrón."))
                    },
                )
            }
        }

        fun saveRoster(
            students: List<RosterStudent>,
            onDone: () -> Unit = {},
        ) = mutate(
            signature = "padron:${students.hashCode()}:${students.size}",
            success = if (students.isEmpty()) "Padrón quitado" else "Padrón guardado: ${students.size} alumnos",
            fallback = "No se guardó el padrón.",
            call = { key -> repository.saveRoster(students, key) },
        ) {
            state = state.copy(roster = students)
            onDone()
        }

        // endregion

        // region Quién cuida a cada alumno

        /** Espera a que deje de escribir para no buscar letra por letra. */
        fun searchGuardianships(text: String) {
            val query = text.trim()
            guardianshipQuery = query
            searchJob?.cancel()
            if (query.length < SchoolGuardianship.MIN_SEARCH_LENGTH) {
                state = state.copy(guardianships = null, guardianshipsError = null)
                return
            }
            searchJob =
                viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS)
                    runSearch(query)
                }
        }

        private suspend fun runSearch(query: String) {
            val result = io { repository.searchGuardianships(query) }
            // Una respuesta lenta de una búsqueda vieja no pisa la de ahora.
            if (query != guardianshipQuery) return
            result.fold(
                onSuccess = { state = state.copy(guardianships = it, guardianshipsError = null) },
                onFailure = { state = state.copy(guardianshipsError = it.toUserFacingMessage("No pudimos buscar.")) },
            )
        }

        fun removeGuardianship(guardianship: SchoolGuardianship) =
            mutate(
                signature = "quitar-tutor:${guardianship.id}",
                success = "${guardianship.guardianName} ya no cuida a ${guardianship.studentName}",
                fallback = "No se quitó.",
                call = { key -> repository.removeGuardianship(guardianship.id, key) },
            ) { viewModelScope.launch { runSearch(guardianshipQuery) } }

        fun makePrincipal(guardianship: SchoolGuardianship) =
            mutate(
                signature = "principal:${guardianship.id}",
                success = "Ahora ${guardianship.guardianName} pone los controles de ${guardianship.studentName}",
                fallback = "No se cambió.",
                call = { key -> repository.makePrincipal(guardianship.id, key) },
            ) { viewModelScope.launch { runSearch(guardianshipQuery) } }

        // endregion

        // region Cada día

        /** Primero pide el "hoy" de la escuela y luego los 15 días de la tira con esas fechas. */
        fun loadMenu() {
            viewModelScope.launch {
                val today =
                    state.today ?: io { repository.menuCalendar() }.getOrNull()?.today ?: return@launch
                io { repository.menuCalendar(today, today.plusDays(SchoolSettingsRules.MAX_DAYS_AHEAD.toLong())) }
                    .onSuccess { state = state.copy(menu = it) }
            }
        }

        fun loadReserved(date: LocalDate) {
            viewModelScope.launch {
                io { repository.reservedDay(date) }.onSuccess { day ->
                    state = state.copy(reserved = state.reserved + (date to day))
                }
            }
        }

        fun saveMenu(
            date: LocalDate,
            productIds: Set<Int>,
            onDone: () -> Unit = {},
        ) {
            if (state.saving) return
            state = state.copy(savingMenuDate = date)
            mutate(
                signature = "menu:$date:${productIds.sorted()}",
                success = if (productIds.isEmpty()) "Ese día se vende todo el menú" else "Menú del día guardado",
                fallback = "No se guardó el menú del día.",
                call = { key -> repository.saveMenu(date, productIds.sorted(), key) },
                onFailure = { state = state.copy(savingMenuDate = null) },
            ) {
                state = state.copy(savingMenuDate = null)
                onDone()
                loadMenu()
            }
        }

        fun closeDay(
            date: LocalDate,
            reason: String,
        ) {
            val typed = reason.trim()
            mutate(
                signature = "cerrar:$date:$typed",
                success = null,
                fallback = "No se cerró el día.",
                call = { key -> repository.closeDay(date, typed, key) },
            ) { closure ->
                showNotice(closure.summary(::moneyLabel), isError = false)
                loadMenu()
                loadReserved(date)
            }
        }

        fun reopenDay(date: LocalDate) =
            mutate(
                signature = "abrir:$date",
                success = "Ese día vuelve a haber servicio",
                fallback = "No se abrió el día.",
                call = { key -> repository.reopenDay(date, key) },
            ) { loadMenu() }

        // endregion

        // region Devoluciones y bajas

        fun loadStudentRequests() {
            viewModelScope.launch {
                io { repository.studentRequests() }.fold(
                    onSuccess = { state = state.copy(studentRequests = it, studentRequestsError = null) },
                    onFailure = {
                        state =
                            state.copy(
                                studentRequestsError =
                                    it.toUserFacingMessage(
                                        "No pudimos traer las devoluciones y bajas.",
                                    ),
                            )
                    },
                )
            }
        }

        /**
         * Devolver: el saldo de hoy sale de la cartera (y, en efectivo, de la caja abierta). Baja: el alumno deja la
         * escuela y se borra lo que lo identifica.
         */
        fun resolve(request: SchoolStudentRequest) =
            answer(
                request,
                "resolver",
                resolvedText(request),
            ) { key -> repository.resolveStudentRequest(request.id, key) }

        fun reject(
            request: SchoolStudentRequest,
            reason: String,
            onDone: () -> Unit = {},
        ) {
            val typed = reason.trim()
            if (typed.length < SchoolStudentRequest.MIN_REJECTION_REASON) {
                return showNotice("Escribe por qué no procede.", isError = true)
            }
            answer(request, "rechazar:$typed", "Le avisamos a ${request.guardianName}", onDone) { key ->
                repository.rejectStudentRequest(request.id, typed, key)
            }
        }

        private fun answer(
            request: SchoolStudentRequest,
            action: String,
            success: String,
            onDone: () -> Unit = {},
            call: (String) -> Result<Unit>,
        ) = mutate(
            signature = "solicitud:${request.id}:$action",
            success = success,
            fallback = "No se guardó.",
            call = call,
            onFailure = { loadStudentRequests() },
        ) {
            state = state.copy(studentRequests = state.studentRequests?.filterNot { it.id == request.id })
            onDone()
        }

        // endregion

        fun dismissNotice() {
            noticeJob?.cancel()
            state = state.copy(notice = null)
        }

        private fun <T> mutate(
            signature: String,
            success: String?,
            fallback: String,
            call: (key: String) -> Result<T>,
            onFailure: () -> Unit = {},
            onSuccess: (T) -> Unit,
        ) {
            if (state.saving) return
            state = state.copy(saving = true)
            val key = pendingKeys.getOrPut(signature) { UUID.randomUUID().toString() }
            viewModelScope.launch {
                val result = io { call(key) }
                state = state.copy(saving = false)
                result.fold(
                    onSuccess = {
                        pendingKeys.remove(signature)
                        success?.let { text -> showNotice(text, isError = false) }
                        onSuccess(it)
                    },
                    onFailure = {
                        showNotice(schoolErrorMessage(it, fallback), isError = true)
                        onFailure()
                    },
                )
            }
        }

        private fun showNotice(
            text: String,
            isError: Boolean,
        ) {
            noticeJob?.cancel()
            state = state.copy(notice = text, noticeIsError = isError)
            noticeJob =
                viewModelScope.launch {
                    delay(NOTICE_MS)
                    state = state.copy(notice = null)
                }
        }

        private suspend fun <T> io(call: () -> Result<T>): Result<T> = withContext(Dispatchers.IO) { call() }

        private companion object {
            const val NOTICE_MS = 3_500L
            const val SEARCH_DEBOUNCE_MS = 350L
        }
    }

internal fun resolvedText(request: SchoolStudentRequest): String {
    val amount = moneyLabel(request.amountToRefund)
    return when {
        request.kind == StudentRequestKind.DATA_REMOVAL -> "${request.studentName} quedó dado de baja"
        request.isCashRefund -> "Listo: salieron $amount de la caja para ${request.guardianName}"
        else -> "Listo: entrega $amount a ${request.guardianName}"
    }
}

/** Administración no cobra: este código solo le llega al devolver saldo en efectivo con la caja cerrada. */
internal const val CASH_SESSION_REQUIRED_TEXT = "Abre la caja primero: el efectivo se registra en ella."

internal fun schoolErrorMessage(
    error: Throwable,
    fallback: String,
): String {
    val code = (error as? CatalogRepositoryException)?.code
    return when (code) {
        // El servidor ya lo dice para la administración ("Abre la caja para devolver efectivo…").
        "CASH_SESSION_REQUIRED" -> error.message?.takeIf { it.isNotBlank() && it != code } ?: CASH_SESSION_REQUIRED_TEXT
        "GUARDIANSHIP_NOT_PENDING" -> "Esa solicitud ya se revisó. Actualiza la lista."
        "GUARDIANSHIP_NOT_FOUND" -> "Ese tutor ya no cuida a este alumno. Busca otra vez."
        "REQUEST_NOT_PENDING" -> "Esa solicitud ya se resolvió. Actualiza la lista."
        "REFUND_BALANCE_FIRST" -> "Todavía tiene saldo: primero hay que devolvérselo a su tutor."
        "STUDENT_HAS_ACTIVE_ORDERS" -> "Tiene comida por entregar: entrégala o cancélala antes de darlo de baja."
        "REJECTION_REASON_REQUIRED" -> "Escribe por qué no procede."
        "DATE_IN_THE_PAST" -> "Ese día ya pasó."
        "NO_SERVICE_DAY_NOT_FOUND" -> "Ese día ya tenía servicio. Actualiza la pantalla."
        else -> error.toUserFacingMessage(fallback)
    }
}
