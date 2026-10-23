package com.vaiinilla.app.ui.counter

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.counter.ReservedCalendar
import com.vaiinilla.app.domain.counter.ReservedDay
import com.vaiinilla.app.domain.counter.StudentCounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

data class ReservedOrdersUiState(
    /** Solo una escuela tiene "Para mañana". */
    val isSchool: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val selectedDate: String = LocalDate.now().plusDays(1).toString(),
    val days: Map<String, ReservedDay> = emptyMap(),
    val error: String? = null,
)

/** Cocina de una escuela: cuánto de cada cosa se apartó para los próximos días, para preparar a tiempo. */
@HiltViewModel
class ReservedOrdersViewModel
    @Inject
    constructor(
        private val repository: StudentCounterRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(ReservedOrdersUiState())
        val uiState: State<ReservedOrdersUiState> = _uiState

        /** Cocina carga la política de la escuela aunque no tenga mostrador: sin ella no sale "Para mañana". */
        fun refreshPolicy() {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repository.schoolPolicy() }
                    .onSuccess { _uiState.value = _uiState.value.copy(isSchool = it.isSchool) }
            }
        }

        fun open() {
            val today = LocalDate.now()
            // El siguiente día de escuela: un viernes abre el lunes.
            val next = ReservedCalendar.dates(today).getOrNull(1) ?: today.toString()
            _uiState.value = _uiState.value.copy(today = today, selectedDate = next, error = null)
            load(_uiState.value.selectedDate)
        }

        fun select(date: String) {
            _uiState.value = _uiState.value.copy(selectedDate = date, error = null)
            load(date)
        }

        fun retry() = load(_uiState.value.selectedDate)

        private fun load(date: String) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repository.reservedDay(date) }
                    .onSuccess {
                        _uiState.value =
                            _uiState.value.copy(days = _uiState.value.days + (date to it), error = null)
                    }.onFailure {
                        // Con lo ya cargado de ese día no se tapa con un error.
                        if (_uiState.value.days[date] == null) {
                            _uiState.value =
                                _uiState.value.copy(
                                    error = it.toUserFacingMessage("No se pudo ver lo apartado. Intenta otra vez."),
                                )
                        }
                    }
            }
        }
    }
