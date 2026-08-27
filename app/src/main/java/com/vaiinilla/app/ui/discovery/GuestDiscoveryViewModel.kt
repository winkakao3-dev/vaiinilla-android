package com.vaiinilla.app.ui.discovery

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.discovery.DiscoveryFailures
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.repository.DiscoveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DiscoveryUiState(
    val query: String = "",
    val loading: Boolean = false,
    val resolving: Boolean = false,
    val establishments: List<PublicEstablishment> = emptyList(),
    val errorMessage: String? = null,
    val suspendedMessage: String? = null,
    val spaceTokenInput: String = "",
    val pendingSwitch: GuestVenueContext? = null,
    val selected: GuestVenueContext? = null,
)

@HiltViewModel
class GuestDiscoveryViewModel
    @Inject
    constructor(
        private val discoveryRepository: DiscoveryRepository,
        private val guestSessionStore: GuestSessionStore,
    ) : ViewModel() {
        private val _state = mutableStateOf(DiscoveryUiState())
        val state: State<DiscoveryUiState> = _state

        private var searchJob: Job? = null
        private var searchRevision: Long = 0
        private val activeJobs = mutableSetOf<Job>()

        init {
            _state.value =
                _state.value.copy(
                    selected = guestSessionStore.readVenue(),
                )
            search("")
        }

        fun updateQuery(query: String) {
            _state.value = _state.value.copy(query = query)
            launchSearch(query = query, debounceMs = 220L)
        }

        fun updateSpaceToken(token: String) {
            _state.value = _state.value.copy(spaceTokenInput = token)
        }

        fun resolveQrPayload(
            rawValue: String,
            onEntered: (GuestVenueContext) -> Unit,
        ) {
            QrPayloadParser.parse(rawValue).fold(
                onSuccess = { payload ->
                    when (payload) {
                        is QrPayload.Establishment ->
                            openSlug(
                                slug = payload.slug,
                                onEntered = onEntered,
                            )
                        is QrPayload.User -> {
                            _state.value =
                                _state.value.copy(
                                    errorMessage =
                                        "Ese QR es de un alumno. En Caja se usa para recargar saldo.",
                                )
                        }
                        is QrPayload.SpaceToken -> {
                            updateSpaceToken(payload.token)
                            resolveSpaceToken(onEntered)
                        }
                    }
                },
                onFailure = { error ->
                    _state.value =
                        _state.value.copy(
                            errorMessage = error.toUserFacingMessage("No se pudo leer el QR."),
                        )
                },
            )
        }

        fun search(query: String = _state.value.query) {
            launchSearch(query = query, debounceMs = 0L)
        }

        private fun launchSearch(
            query: String,
            debounceMs: Long,
        ) {
            searchJob?.cancel()
            val revision = ++searchRevision
            _state.value = _state.value.copy(loading = true, errorMessage = null, suspendedMessage = null)
            searchJob =
                viewModelScope.launch {
                    if (debounceMs > 0) delay(debounceMs)
                    val result =
                        withContext(Dispatchers.IO) {
                            discoveryRepository.searchEstablishments(query = query.trim())
                        }
                    if (revision != searchRevision) return@launch
                    result.fold(
                        onSuccess = { (items, _) ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    establishments = items,
                                    errorMessage =
                                        if (items.isEmpty()) {
                                            "No encontramos cafeterías con ese nombre."
                                        } else {
                                            null
                                        },
                                )
                        },
                        onFailure = { error ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    establishments = emptyList(),
                                    errorMessage =
                                        error.toUserFacingMessage("No se pudo cargar el descubrimiento."),
                                )
                        },
                    )
                }
        }

        fun selectEstablishment(
            establishment: PublicEstablishment,
            onEntered: (GuestVenueContext) -> Unit,
        ) {
            val next = GuestVenueContext(establishment = establishment, space = null)
            tryEnter(next, onEntered)
        }

        fun resolveSpaceToken(onEntered: (GuestVenueContext) -> Unit) {
            val token = _state.value.spaceTokenInput.trim()
            if (token.isEmpty()) {
                _state.value = _state.value.copy(errorMessage = "Pega el token del QR de espacio.")
                return
            }
            _state.value = _state.value.copy(resolving = true, errorMessage = null, suspendedMessage = null)
            launchTracked {
                val result =
                    withContext(Dispatchers.IO) {
                        discoveryRepository.resolveSpaceToken(token)
                    }
                result.fold(
                    onSuccess = { resolved ->
                        _state.value = _state.value.copy(resolving = false)
                        tryEnter(
                            GuestVenueContext(
                                establishment = resolved.establishment,
                                space = resolved.space,
                            ),
                            onEntered,
                        )
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                resolving = false,
                                errorMessage = error.toUserFacingMessage("No se pudo resolver el espacio."),
                            )
                    },
                )
            }
        }

        fun openSlug(
            slug: String,
            onEntered: (GuestVenueContext) -> Unit,
            onFinished: () -> Unit = {},
        ) {
            _state.value = _state.value.copy(resolving = true, errorMessage = null, suspendedMessage = null)
            launchTracked {
                val result =
                    withContext(Dispatchers.IO) {
                        discoveryRepository.getEstablishment(slug)
                    }
                result.fold(
                    onSuccess = { establishment ->
                        _state.value = _state.value.copy(resolving = false)
                        tryEnter(
                            GuestVenueContext(establishment = establishment, space = null),
                            onEntered,
                        )
                        onFinished()
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                resolving = false,
                                errorMessage =
                                    if (DiscoveryFailures.isEstablishmentSuspended(error)) {
                                        null
                                    } else {
                                        error.toUserFacingMessage("QR de establecimiento inválido.")
                                    },
                                suspendedMessage =
                                    if (DiscoveryFailures.isEstablishmentSuspended(error)) {
                                        error.toUserFacingMessage("Este establecimiento está suspendido.")
                                    } else {
                                        null
                                    },
                            )
                        onFinished()
                    },
                )
            }
        }

        fun confirmPendingSwitch(onEntered: (GuestVenueContext) -> Unit) {
            val pending = _state.value.pendingSwitch ?: return
            _state.value = _state.value.copy(pendingSwitch = null)
            commit(pending, onEntered)
        }

        fun dismissPendingSwitch() {
            _state.value = _state.value.copy(pendingSwitch = null)
        }

        fun clearForSessionTermination() {
            searchRevision += 1
            searchJob?.cancel()
            searchJob = null
            activeJobs.toList().forEach(Job::cancel)
            activeJobs.clear()
            _state.value = DiscoveryUiState()
        }

        private fun launchTracked(block: suspend () -> Unit): Job {
            lateinit var job: Job
            job =
                viewModelScope.launch {
                    try {
                        block()
                    } finally {
                        activeJobs.remove(job)
                    }
                }
            activeJobs += job
            return job
        }

        private fun currentVenueHasCart(): Boolean {
            val current = guestSessionStore.readVenue() ?: return false
            val key =
                guestSessionStore.cartStorageKey(
                    current.establishment.id,
                    current.space?.id,
                )
            return guestSessionStore.readCartSnapshot(key).isNotEmpty()
        }

        private fun tryEnter(
            next: GuestVenueContext,
            onEntered: (GuestVenueContext) -> Unit,
        ) {
            val current = guestSessionStore.readVenue()
            val switchingTenant =
                current != null &&
                    (
                        current.establishment.id != next.establishment.id ||
                            current.space?.id != next.space?.id
                    )
            if (switchingTenant && currentVenueHasCart()) {
                _state.value = _state.value.copy(pendingSwitch = next)
                return
            }
            commit(next, onEntered)
        }

        private fun commit(
            next: GuestVenueContext,
            onEntered: (GuestVenueContext) -> Unit,
        ) {
            guestSessionStore.saveVenue(next)
            _state.value = _state.value.copy(selected = next, errorMessage = null)
            onEntered(next)
        }
    }
