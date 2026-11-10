package com.vaiinilla.app.ui.platform

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.auth.student.StudentAuthMfaRequiredException
import com.vaiinilla.app.data.auth.student.StudentAuthProviderNotSupportedException
import com.vaiinilla.app.data.platform.PlatformAuthority
import com.vaiinilla.app.data.platform.PlatformMetrics
import com.vaiinilla.app.data.platform.PlatformRepository
import com.vaiinilla.app.data.platform.PlatformStore
import com.vaiinilla.app.data.platform.PlatformStripe
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaChallenge
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.TotpEnrollmentData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

sealed interface PlatformPage {
    data object Pulse : PlatformPage

    data object Stores : PlatformPage

    data class Form(
        val storeId: String?,
    ) : PlatformPage

    data class Detail(
        val storeId: String,
    ) : PlatformPage

    data class Assign(
        val storeId: String,
    ) : PlatformPage

    data class Suspend(
        val storeId: String,
    ) : PlatformPage

    data class Stripe(
        val storeId: String,
    ) : PlatformPage
}

enum class PulsePeriod(
    val label: String,
) {
    WEEK("7 días"),
    MONTH("30 días"),
    THIS_MONTH("Este mes"),
}

internal fun pulseRange(
    period: PulsePeriod,
    today: LocalDate,
): Pair<String, String> {
    val from =
        when (period) {
            PulsePeriod.WEEK -> today.minus(6, ChronoUnit.DAYS)
            PulsePeriod.MONTH -> today.minus(29, ChronoUnit.DAYS)
            PulsePeriod.THIS_MONTH -> today.withDayOfMonth(1)
        }
    return from.toString() to today.toString()
}

private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')

internal fun slugFrom(name: String): String =
    name
        .lowercase()
        .map { ACCENTS[it] ?: it }
        .joinToString("")
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

data class PlatformUiState(
    val authority: PlatformAuthority? = null,
    val page: PlatformPage = PlatformPage.Pulse,
    val needsConfirm: Boolean = false,
    val confirming: Boolean = false,
    val confirmError: String? = null,
    val challenge: StudentAuthMfaChallenge? = null,
    val enrollData: TotpEnrollmentData? = null,
    val enrollCode: String = "",
    val enrolling: Boolean = false,
    val enrollError: String? = null,
    val period: PulsePeriod = PulsePeriod.WEEK,
    val metrics: PlatformMetrics? = null,
    val stores: List<PlatformStore> = emptyList(),
    val storesCursor: String? = null,
    val storesLoaded: Boolean = false,
    val stateFilter: String? = null,
    val query: String = "",
    val stripe: PlatformStripe? = null,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val stripeUrl: String? = null,
)

private const val SAFETY_MARGIN_MS = 15_000L

@HiltViewModel
class PlatformViewModel
    @Inject
    constructor(
        private val repository: PlatformRepository,
        private val auth: StudentAuthRepository,
    ) : ViewModel() {
        private val _state = mutableStateOf(PlatformUiState())
        val state: State<PlatformUiState> = _state

        private var token: String? = null
        private var tokenUntil = 0L
        private var pending: (suspend (String) -> Unit)? = null

        /** La misma acción reintentada conserva su llave: un doble envío no crea dos tiendas. */
        private val keys = mutableMapOf<String, String>()

        private fun update(change: PlatformUiState.() -> PlatformUiState) {
            _state.value = _state.value.change()
        }

        fun checkAuthority() {
            viewModelScope.launch {
                val firebase = auth.getIdToken(forceRefresh = false).getOrNull()
                if (firebase == null) {
                    update { copy(authority = null) }
                    return@launch
                }
                repository.authority(firebase).onSuccess { update { copy(authority = it) } }
            }
        }

        /** Para decidir a dónde va la cuenta al entrar; un fallo de red no la trata como autoridad. */
        suspend fun fetchIsAuthority(): Boolean {
            val firebase = auth.getIdToken(forceRefresh = false).getOrNull() ?: return false
            val authority = repository.authority(firebase).getOrNull() ?: return false
            update { copy(authority = authority) }
            return authority.isAuthority
        }

        fun clearOnSignOut() {
            token = null
            tokenUntil = 0
            pending = null
            keys.clear()
            auth.cancelTotpEnrollment()
            _state.value = PlatformUiState()
        }

        fun open() {
            update { copy(page = PlatformPage.Pulse, error = null, notice = null) }
            refreshPulse()
            refreshStores()
        }

        fun navigate(page: PlatformPage) {
            update { copy(page = page, error = null, notice = null) }
            if (page is PlatformPage.Stripe) loadStripe(page.storeId)
        }

        fun back(): Boolean {
            val target =
                when (val page = _state.value.page) {
                    PlatformPage.Pulse -> return false
                    PlatformPage.Stores -> PlatformPage.Pulse
                    is PlatformPage.Form -> page.storeId?.let(PlatformPage::Detail) ?: PlatformPage.Stores
                    is PlatformPage.Detail -> PlatformPage.Stores
                    is PlatformPage.Assign -> PlatformPage.Detail(page.storeId)
                    is PlatformPage.Suspend -> PlatformPage.Detail(page.storeId)
                    is PlatformPage.Stripe -> PlatformPage.Detail(page.storeId)
                }
            update { copy(page = target, error = null) }
            return true
        }

        fun dismissMessages() = update { copy(error = null, notice = null) }

        fun stripeUrlOpened() = update { copy(stripeUrl = null) }

        fun selectPeriod(period: PulsePeriod) {
            update { copy(period = period) }
            refreshPulse()
        }

        fun refreshPulse() {
            val (from, to) = pulseRange(_state.value.period, LocalDate.now())
            run(loading = true) { platform ->
                repository
                    .metrics(platform, from, to)
                    .getOrThrow()
                    .let { metrics -> update { copy(metrics = metrics) } }
            }
        }

        fun setStoreFilter(
            state: String?,
            query: String,
        ) {
            update { copy(stateFilter = state, query = query) }
            refreshStores()
        }

        fun refreshStores() {
            val current = _state.value
            run(loading = true) { platform ->
                val page = repository.stores(platform, current.stateFilter, current.query, null).getOrThrow()
                update { copy(stores = page.stores, storesCursor = page.cursor, storesLoaded = true) }
            }
        }

        fun loadMoreStores() {
            val current = _state.value
            val cursor = current.storesCursor ?: return
            run(loading = true) { platform ->
                val page = repository.stores(platform, current.stateFilter, current.query, cursor).getOrThrow()
                update { copy(stores = stores + page.stores, storesCursor = page.cursor) }
            }
        }

        fun saveStore(
            storeId: String?,
            fields: Map<String, Any?>,
            image: Pair<ByteArray, String>?,
            mapsLink: String?,
        ) {
            action("guardar:${storeId ?: "nueva"}:${fields.hashCode()}") { platform, key ->
                val located =
                    if (mapsLink.isNullOrBlank()) {
                        fields
                    } else {
                        val (lat, lng) = repository.resolveMaps(platform, mapsLink).getOrThrow()
                        fields + mapOf("latitud" to lat, "longitud" to lng)
                    }
                val saved =
                    if (storeId == null) {
                        repository.create(platform, located, key).getOrThrow()
                    } else {
                        repository.configure(platform, storeId, located, key).getOrThrow()
                    }
                val withImage =
                    image?.let { (bytes, mime) ->
                        repository.uploadImage(platform, saved.id, bytes, mime, "$key-foto".uuidFrom()).getOrThrow()
                    } ?: saved
                upsert(withImage)
                update {
                    copy(
                        page = if (storeId == null) PlatformPage.Assign(saved.id) else PlatformPage.Detail(saved.id),
                        notice = if (storeId == null) "Tienda creada." else "Cambios guardados.",
                    )
                }
                refreshStoresQuietly(platform)
            }
        }

        fun assignAdmin(
            storeId: String,
            email: String,
        ) {
            action("asignar:$storeId:$email") { platform, key ->
                val result = repository.assignAdmin(platform, storeId, email, key).getOrThrow()
                update {
                    copy(
                        page = PlatformPage.Detail(storeId),
                        notice =
                            when {
                                result.asignado -> "${result.email} ya es administrador. Lo verá al entrar a la app."
                                result.mailSent -> "Le mandamos la invitación a ${result.email}."
                                else ->
                                    "No pudimos mandar el correo. Cuando ${result.email} entre a la app con ese " +
                                        "correo en los próximos 3 días, la tienda ya estará ahí."
                            },
                    )
                }
                refreshStoresQuietly(platform)
            }
        }

        fun setSuspended(
            storeId: String,
            suspend: Boolean,
            reason: String,
        ) {
            action("estado:$storeId:$suspend:$reason") { platform, key ->
                upsert(repository.setSuspended(platform, storeId, suspend, reason, key).getOrThrow())
                update {
                    copy(
                        page = PlatformPage.Detail(storeId),
                        notice = if (suspend) "Tienda suspendida." else "Tienda reactivada.",
                    )
                }
                refreshStoresQuietly(platform)
            }
        }

        private fun loadStripe(storeId: String) {
            update { copy(stripe = null) }
            run(loading = true) { platform ->
                val stripe = repository.stripe(platform, storeId).getOrThrow()
                update { copy(stripe = stripe) }
            }
        }

        fun startStripe(storeId: String) {
            action("stripe:$storeId:${System.currentTimeMillis() / 60_000}") { platform, key ->
                val url = repository.stripeOnboarding(platform, storeId, key).getOrThrow()
                update { copy(stripeUrl = url) }
            }
        }

        fun linkStripe(
            storeId: String,
            accountId: String?,
        ) {
            action("vincular:$storeId:$accountId") { platform, key ->
                repository.stripeLinkExisting(platform, storeId, accountId, key).getOrThrow()
                val stripe = repository.stripe(platform, storeId).getOrThrow()
                update { copy(stripe = stripe, notice = "Cuenta vinculada.") }
            }
        }

        fun setStripeEnabled(
            storeId: String,
            enabled: Boolean,
        ) {
            action("tarjeta:$storeId:$enabled:${System.currentTimeMillis() / 60_000}") { platform, key ->
                repository.stripeSetEnabled(platform, storeId, enabled, key).getOrThrow()
                val stripe = repository.stripe(platform, storeId).getOrThrow()
                update { copy(stripe = stripe) }
            }
        }

        fun cancelConfirm(leave: () -> Unit) {
            _state.value.challenge?.let { auth.cancelMfa(it.id) }
            auth.cancelTotpEnrollment()
            pending = null
            update {
                copy(
                    needsConfirm = false,
                    challenge = null,
                    enrollData = null,
                    enrollCode = "",
                    confirming = false,
                    enrolling = false,
                    confirmError = null,
                    enrollError = null,
                )
            }
            if (token == null) leave()
        }

        fun confirmPassword(password: String) {
            update { copy(confirming = true, confirmError = null) }
            viewModelScope.launch {
                auth.reauthenticateWithPassword(password).fold(
                    onSuccess = {
                        val isAuthority = _state.value.authority?.isAuthority == true
                        val totpEnrolled = _state.value.authority?.totpEnrolled == true
                        if (isAuthority && !totpEnrolled) {
                            startTotpEnrollment()
                        } else {
                            exchange()
                        }
                    },
                    onFailure = { error ->
                        when (error) {
                            is StudentAuthMfaRequiredException ->
                                update { copy(confirming = false, challenge = error.challenge) }
                            is StudentAuthProviderNotSupportedException ->
                                update {
                                    copy(
                                        confirming = false,
                                        confirmError = "Plataforma pide una cuenta con contraseña y código.",
                                    )
                                }
                            else -> update { copy(confirming = false, confirmError = error.toUserFacingMessage()) }
                        }
                    },
                )
            }
        }

        fun confirmCode(
            factorUid: String,
            code: String,
        ) {
            val challenge = _state.value.challenge ?: return
            update { copy(confirming = true, confirmError = null) }
            viewModelScope.launch {
                auth.resolveMfa(challenge.id, factorUid, code).fold(
                    onSuccess = { exchange() },
                    onFailure = { update { copy(confirming = false, confirmError = it.toUserFacingMessage()) } },
                )
            }
        }

        fun startTotpEnrollment() {
            update { copy(confirming = true, confirmError = null, enrollError = null) }
            viewModelScope.launch {
                auth.startTotpEnrollment().fold(
                    onSuccess = { data ->
                        update {
                            copy(
                                confirming = false,
                                enrollData = data,
                                enrollCode = "",
                                enrollError = null,
                            )
                        }
                    },
                    onFailure = { error ->
                        update {
                            copy(
                                confirming = false,
                                confirmError = "No se pudo iniciar el alta del autenticador: ${error.message}",
                            )
                        }
                    },
                )
            }
        }

        fun updateEnrollCode(value: String) {
            update { copy(enrollCode = value.filter(Char::isDigit).take(6), enrollError = null) }
        }

        fun submitTotpEnrollment() {
            val code = _state.value.enrollCode
            if (code.length != 6) return
            update { copy(enrolling = true, enrollError = null) }
            viewModelScope.launch {
                auth.finalizeTotpEnrollment(code).fold(
                    onSuccess = {
                        update {
                            copy(
                                enrolling = false,
                                enrollData = null,
                                enrollCode = "",
                                authority = authority?.copy(totpEnrolled = true),
                            )
                        }
                        exchange()
                    },
                    onFailure = { error ->
                        update {
                            copy(
                                enrolling = false,
                                enrollError = error.toUserFacingMessage(),
                            )
                        }
                    },
                )
            }
        }

        fun cancelTotpEnrollment(leave: () -> Unit) {
            auth.cancelTotpEnrollment()
            update { copy(enrollData = null, enrollCode = "", enrolling = false, enrollError = null) }
            cancelConfirm(leave)
        }

        private suspend fun exchange() {
            val result =
                auth
                    .getIdToken(forceRefresh = true)
                    .mapCatching { repository.context(it).getOrThrow() }
            result.fold(
                onSuccess = { context ->
                    token = context.accessToken
                    tokenUntil = System.currentTimeMillis() + context.expiresIn * 1000L - SAFETY_MARGIN_MS
                    update {
                        copy(
                            needsConfirm = false,
                            confirming = false,
                            challenge = null,
                            enrollData = null,
                            enrollCode = "",
                            confirmError = null,
                            enrollError = null,
                        )
                    }
                    val retry = pending
                    pending = null
                    retry?.let { guarded(it, showBusy = true) }
                    refreshPulse()
                    refreshStores()
                },
                onFailure = { error ->
                    if ((error as? ApiClientException)?.code == "MFA_REQUIRED") {
                        startTotpEnrollment()
                    } else {
                        update {
                            copy(
                                confirming = false,
                                challenge = null,
                                confirmError = error.toUserFacingMessage(),
                            )
                        }
                    }
                },
            )
        }

        private fun action(
            name: String,
            block: suspend (platform: String, key: String) -> Unit,
        ) {
            if (_state.value.busy) return
            val key = keys.getOrPut(name) { UUID.randomUUID().toString() }
            update { copy(error = null, notice = null) }
            launchWithToken(showBusy = true) { platform ->
                block(platform, key)
                keys.remove(name)
            }
        }

        private fun run(
            loading: Boolean,
            block: suspend (String) -> Unit,
        ) {
            launchWithToken(showBusy = false, showLoading = loading, block = block)
        }

        private fun launchWithToken(
            showBusy: Boolean,
            showLoading: Boolean = false,
            block: suspend (String) -> Unit,
        ) {
            viewModelScope.launch {
                if (showLoading) update { copy(loading = true) }
                guarded(block, showBusy)
                if (showLoading) update { copy(loading = false) }
            }
        }

        private suspend fun guarded(
            block: suspend (String) -> Unit,
            showBusy: Boolean,
        ) {
            val current = token?.takeIf { System.currentTimeMillis() < tokenUntil }
            if (current == null) {
                askConfirm(block.takeIf { showBusy })
                return
            }
            if (showBusy) update { copy(busy = true) }
            runCatching { block(current) }
                .onFailure { error ->
                    if ((error as? ApiClientException)?.httpStatus == 401) {
                        askConfirm(block.takeIf { showBusy })
                    } else if (error is kotlinx.coroutines.CancellationException) {
                        throw error
                    } else {
                        update { copy(error = platformMessage(error)) }
                    }
                }
            if (showBusy) update { copy(busy = false) }
        }

        private fun askConfirm(retry: (suspend (String) -> Unit)?) {
            token = null
            if (retry != null) pending = retry
            update { copy(needsConfirm = true, busy = false, loading = false) }
        }

        private fun upsert(store: PlatformStore) {
            update {
                val known = stores.firstOrNull { it.id == store.id }
                val merged = store.copy(administrador = store.administrador ?: known?.administrador)
                copy(stores = if (known == null) listOf(merged) + stores else stores.map { if (it.id == store.id) merged else it })
            }
        }

        private suspend fun refreshStoresQuietly(platform: String) {
            val current = _state.value
            repository
                .stores(platform, current.stateFilter, current.query, null)
                .onSuccess { page -> update { copy(stores = page.stores, storesCursor = page.cursor) } }
        }

        private fun String.uuidFrom(): String = UUID.nameUUIDFromBytes(toByteArray()).toString()
    }

internal fun platformMessage(error: Throwable): String =
    when ((error as? ApiClientException)?.code) {
        "ESTABLISHMENT_SUSPENDED" -> "La tienda está suspendida. Reactívala primero."
        "INVITATION_STATE_CONFLICT" -> "Esta tienda ya tiene administrador."
        "PLATFORM_AUTHORITY_INACTIVE" -> "Esta cuenta ya no tiene permiso de Plataforma."
        else -> error.toUserFacingMessage()
    }
