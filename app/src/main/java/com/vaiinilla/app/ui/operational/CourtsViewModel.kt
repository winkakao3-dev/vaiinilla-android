package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.WaiterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CourtsUiState(
    /** Canchas del establecimiento con su estado; vacío si no tiene o si el servidor aún no lo ofrece. */
    val courts: List<SpaceAvailability> = emptyList(),
)

/**
 * Mapa de canchas del cliente: qué canchas están libres y cuánto le falta a las ocupadas. Consulta al
 * servidor cada pocos segundos mientras el menú está a la vista. Si falla (sin sesión, servidor sin
 * el endpoint), simplemente no se muestra: es un extra, nunca un bloqueo.
 */
@HiltViewModel
class CourtsViewModel
    @Inject
    constructor(
        private val waiterRepository: WaiterRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(CourtsUiState())
        val uiState: State<CourtsUiState> = _uiState

        private var pollingJob: Job? = null
        private var hasSession = false
        private var slug: String? = null

        /**
         * Con el [slug] del lugar que se está mirando se lee su mapa público, con o sin sesión: el mapa del
         * negocio sale del contexto de cliente, que al entrar por QR puede ser de otro lugar o no existir aún,
         * y entonces el club no mostraba sus canchas. Sin slug, con sesión, se usa el del negocio.
         */
        fun onVisible(
            hasSession: Boolean,
            slug: String?,
        ) {
            if (slug != this.slug) _uiState.value = CourtsUiState()
            this.hasSession = hasSession
            this.slug = slug
            pollingJob?.cancel()
            pollingJob =
                viewModelScope.launch {
                    while (isActive) {
                        refresh()
                        delay(POLL_INTERVAL_MS)
                    }
                }
        }

        fun onHidden() {
            pollingJob?.cancel()
            pollingJob = null
        }

        private suspend fun refresh() {
            val venue = slug
            val result =
                withContext(Dispatchers.IO) {
                    when {
                        venue != null -> waiterRepository.publicAvailability(venue)
                        hasSession -> waiterRepository.availability()
                        else -> Result.success(emptyList())
                    }
                }
            result.fold(
                onSuccess = { all ->
                    _uiState.value =
                        CourtsUiState(
                            courts =
                                all
                                    .filter { it.space.type == COURT_TYPE }
                                    .sortedBy { it.space.name },
                        )
                },
                // Un fallo de una consulta no esconde las canchas que ya se veían.
                onFailure = {},
            )
        }

        override fun onCleared() {
            pollingJob?.cancel()
            super.onCleared()
        }

        private companion object {
            const val POLL_INTERVAL_MS = 10_000L
            const val COURT_TYPE = "cancha"
        }
    }
