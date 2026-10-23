package com.vaiinilla.app.ui.discovery

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.runCatchingCancellable
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.guest.SchoolJoinCodeStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.discovery.DiscoveryFailures
import com.vaiinilla.app.domain.location.DeviceLocationProvider
import com.vaiinilla.app.domain.mode.MySchool
import com.vaiinilla.app.domain.mode.MySchools
import com.vaiinilla.app.domain.model.GeoCoordinates
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
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
    /** Coordenadas con las que se ordena por cercanía; `null` = búsqueda normal. */
    val nearbyOrigin: GeoCoordinates? = null,
    val locating: Boolean = false,
    val locationMessage: String? = null,
    /** Escuelas donde ya es cliente, para volver de un toque al pedir en otro lado. */
    val mySchools: List<MySchool> = emptyList(),
    val openingSchoolId: String? = null,
)

@HiltViewModel
class GuestDiscoveryViewModel
    @Inject
    constructor(
        private val discoveryRepository: DiscoveryRepository,
        private val guestSessionStore: GuestSessionStore,
        private val deviceLocationProvider: DeviceLocationProvider,
        private val schoolJoinCodes: SchoolJoinCodeStore,
        private val accessRepository: AuthorizedAccessRepository,
        private val authRepository: StudentAuthRepository,
    ) : ViewModel() {
        private val _state = mutableStateOf(DiscoveryUiState())
        val state: State<DiscoveryUiState> = _state

        private var searchJob: Job? = null
        private var searchRevision: Long = 0
        private var locateJob: Job? = null
        private val activeJobs = mutableSetOf<Job>()

        companion object {
            private const val LOCATION_DENIED_MESSAGE =
                "No compartiste tu ubicación. Puedes buscar por nombre, código o QR."
            private const val LOCATION_UNAVAILABLE_MESSAGE =
                "No pudimos leer tu ubicación. Revisa que el GPS esté activo o busca por nombre."
        }

        init {
            _state.value =
                _state.value.copy(
                    selected = guestSessionStore.readVenue(),
                )
            search("")
        }

        fun refreshSelectedVenue() {
            val selected = guestSessionStore.readVenue()
            if (_state.value.selected != selected) {
                _state.value = _state.value.copy(selected = selected)
            }
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
                    val nearTo = _state.value.nearbyOrigin
                    val result =
                        withContext(Dispatchers.IO) {
                            discoveryRepository.searchEstablishments(query = query.trim(), nearTo = nearTo)
                        }
                    if (revision != searchRevision) return@launch
                    result.fold(
                        onSuccess = { (items, _) ->
                            val selected = _state.value.selected ?: guestSessionStore.readVenue()
                            val refreshedSelected =
                                selected?.let { current ->
                                    items
                                        .firstOrNull { it.id == current.establishment.id }
                                        ?.let(guestSessionStore::refreshSelectedVenueMetadata)
                                        ?: current
                                }
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    establishments = items,
                                    selected = refreshedSelected,
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

        /** Llamar solo con permiso de ubicación concedido: la pantalla lo pide antes. */
        fun enableNearby() {
            if (_state.value.locating) return
            _state.value = _state.value.copy(locating = true, locationMessage = null)
            locateJob =
                viewModelScope.launch {
                    val located =
                        withContext(Dispatchers.IO) {
                            runCatchingCancellable { deviceLocationProvider.currentLocation() }
                        }
                    located.fold(
                        onSuccess = { origin ->
                            if (origin == null) {
                                _state.value =
                                    _state.value.copy(
                                        locating = false,
                                        locationMessage = LOCATION_UNAVAILABLE_MESSAGE,
                                    )
                            } else {
                                _state.value =
                                    _state.value.copy(
                                        locating = false,
                                        nearbyOrigin = origin,
                                        locationMessage = null,
                                    )
                                launchSearch(query = _state.value.query, debounceMs = 0L)
                            }
                        },
                        onFailure = {
                            _state.value =
                                _state.value.copy(locating = false, locationMessage = LOCATION_UNAVAILABLE_MESSAGE)
                        },
                    )
                }
        }

        fun disableNearby() {
            locateJob?.cancel()
            locateJob = null
            _state.value = _state.value.copy(nearbyOrigin = null, locating = false, locationMessage = null)
            launchSearch(query = _state.value.query, debounceMs = 0L)
        }

        fun onNearbyPermissionDenied() {
            _state.value =
                _state.value.copy(
                    nearbyOrigin = null,
                    locating = false,
                    locationMessage = LOCATION_DENIED_MESSAGE,
                )
        }

        fun selectEstablishment(
            establishment: PublicEstablishment,
            onEntered: (GuestVenueContext) -> Unit,
        ) {
            val next = GuestVenueContext(establishment = establishment, space = null)
            tryEnter(next, onEntered)
        }

        fun resolveSpaceToken(
            onEntered: (GuestVenueContext) -> Unit,
            onFinished: () -> Unit = {},
        ) {
            val token = _state.value.spaceTokenInput.trim()
            if (token.isEmpty()) {
                _state.value = _state.value.copy(errorMessage = "Pega el token del QR de espacio.")
                onFinished()
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
                        // Sin mesa, lo tecleado era el código de la escuela: viaja como codigo_union al unirse.
                        if (resolved.space == null) schoolJoinCodes.remember(resolved.establishment.id, token)
                        tryEnter(
                            GuestVenueContext(
                                establishment = resolved.establishment,
                                space = resolved.space?.copy(qrToken = token),
                            ),
                            onEntered,
                        )
                        onFinished()
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                resolving = false,
                                errorMessage = SpaceCodeError.message(error),
                            )
                        onFinished()
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

        /** Las escuelas de sus accesos. Sin sesión verificada o si falla, no hay fila. */
        fun loadMySchools() {
            launchTracked { refreshMySchools() }
        }

        /** La escuela de un aviso; si no está en la lista se vuelve a pedir, por si entró hace poco. */
        suspend fun schoolFor(establishmentId: String): MySchool? =
            _state.value.mySchools.firstOrNull { it.id == establishmentId }
                ?: refreshMySchools().firstOrNull { it.id == establishmentId }

        /** Ya es miembro: entra por el slug, sin pedirle otra vez el código. */
        fun openSchool(
            school: MySchool,
            onEntered: (GuestVenueContext) -> Unit,
        ) {
            if (_state.value.openingSchoolId != null) return
            _state.value = _state.value.copy(openingSchoolId = school.id)
            openSlug(school.slug, onEntered, onFinished = { _state.value = _state.value.copy(openingSchoolId = null) })
        }

        private suspend fun refreshMySchools(): List<MySchool> {
            val session = authRepository.peekSession()?.takeIf { it.emailVerified }
            val schools =
                if (session == null) {
                    emptyList()
                } else {
                    withContext(Dispatchers.IO) { accessRepository.authorizedModes(session) }
                        .map(MySchools::from)
                        .getOrElse { return _state.value.mySchools }
                }
            _state.value = _state.value.copy(mySchools = schools)
            return schools
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
            locateJob?.cancel()
            locateJob = null
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
