package com.vaiinilla.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminWalletSummary
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.CashbackRules
import com.vaiinilla.app.domain.admin.StoreConfig
import com.vaiinilla.app.domain.admin.StoreFlow
import com.vaiinilla.app.domain.admin.StoreRules
import com.vaiinilla.app.domain.admin.StoreVideo
import com.vaiinilla.app.domain.admin.StripeSetup
import com.vaiinilla.app.domain.repository.AdminStoreRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import com.vaiinilla.app.domain.repository.StoreImageKind
import com.vaiinilla.app.domain.repository.StoreImages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class AdminStoreUiState(
    val config: StoreConfig? = null,
    val configError: String? = null,
    /** null mientras carga o si Stripe aún no se conecta; ver [stripeLoaded]. */
    val stripe: StripeSetup? = null,
    val stripeLoaded: Boolean = false,
    val cashback: CashbackRule? = null,
    val cashbackLoaded: Boolean = false,
    val walletPreset: AdminPeriodPreset = AdminPeriodPreset.MONTH,
    val wallet: AdminWalletSummary? = null,
    val imageBusy: StoreImageKind? = null,
    val saving: Boolean = false,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
) {
    val images: StoreImages get() = StoreImages(photoUrl = config?.imageUrl, logoUrl = config?.logoUrl)
}

/** Flujo de mi tienda, pagos con tarjeta y cashback: la configuración del negocio. */
@HiltViewModel
class AdminStoreViewModel
    @Inject
    constructor(
        private val repository: AdminStoreRepository,
        private val catalogRepository: CatalogRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(AdminStoreUiState())
        val uiState: State<AdminStoreUiState> = _uiState
        private var noticeJob: Job? = null
        private var scope: String? = null

        fun onVisible(scope: String) {
            if (scope != this.scope) {
                this.scope = scope
                _uiState.value = AdminStoreUiState()
            }
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                val config = withContext(Dispatchers.IO) { repository.config() }
                _uiState.value =
                    config.fold(
                        onSuccess = { _uiState.value.copy(config = it, configError = null) },
                        onFailure = {
                            _uiState.value.copy(
                                configError = it.toUserFacingMessage("No se pudo cargar tu tienda."),
                            )
                        },
                    )
            }
            refreshStripe()
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repository.cashbackRule() }.onSuccess {
                    _uiState.value = _uiState.value.copy(cashback = it, cashbackLoaded = true)
                }
            }
            refreshWallet()
        }

        fun refreshStripe() {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repository.stripe() }.onSuccess {
                    _uiState.value = _uiState.value.copy(stripe = it, stripeLoaded = true)
                }
            }
        }

        // region Flujo

        fun saveFlow(
            flow: StoreFlow,
            onDone: () -> Unit = {},
        ) {
            val ready = flow.copy(orderWindows = StoreRules.sortedWindows(flow.orderWindows))
            StoreRules.problem(ready)?.let { return showNotice(it, isError = true) }
            saveConfig("Flujo guardado", onDone) { repository.saveFlow(ready) }
        }

        fun saveVideos(videos: List<StoreVideo>) {
            StoreRules.videosProblem(videos)?.let { return showNotice(it, isError = true) }
            saveConfig("Videos guardados") { repository.saveVideos(videos) }
        }

        fun setVisibleInDirectory(visible: Boolean) =
            saveConfig(
                if (visible) "Tu tienda aparece en la lista de Vaiinilla" else "Tu tienda solo se abre con QR o código",
            ) {
                repository.setVisibleInDirectory(visible)
            }

        private fun saveConfig(
            success: String,
            onDone: () -> Unit = {},
            call: () -> Result<StoreConfig>,
        ) {
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { call() }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(config = it)
                        showNotice(success, isError = false)
                        onDone()
                    },
                    onFailure = { showNotice(storeErrorMessage(it), isError = true) },
                )
            }
        }

        // endregion

        // region Logo y foto

        fun uploadImage(
            kind: StoreImageKind,
            bytes: ByteArray,
            filename: String,
            mimeType: String,
        ) {
            if (_uiState.value.imageBusy != null) return
            _uiState.value = _uiState.value.copy(imageBusy = kind)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        catalogRepository.uploadStoreImage(
                            kind,
                            bytes,
                            filename,
                            mimeType,
                            MutationIdempotency.storeImage(kind.wireValue, bytes),
                        )
                    }
                applyImages(result, if (kind == StoreImageKind.LOGO) "Logo actualizado" else "Foto actualizada")
            }
        }

        fun removeImage(kind: StoreImageKind) {
            if (_uiState.value.imageBusy != null) return
            _uiState.value = _uiState.value.copy(imageBusy = kind)
            viewModelScope.launch {
                val result =
                    withContext(
                        Dispatchers.IO,
                    ) { catalogRepository.removeStoreImage(kind, UUID.randomUUID().toString()) }
                applyImages(result, if (kind == StoreImageKind.LOGO) "Logo quitado" else "Foto quitada")
            }
        }

        private fun applyImages(
            result: Result<StoreImages>,
            success: String,
        ) {
            _uiState.value = _uiState.value.copy(imageBusy = null)
            result.fold(
                onSuccess = { images ->
                    _uiState.value =
                        _uiState.value.copy(
                            config = _uiState.value.config?.copy(imageUrl = images.photoUrl, logoUrl = images.logoUrl),
                        )
                    showNotice(success, isError = false)
                },
                onFailure = { showNotice(it.toUserFacingMessage("No se pudo guardar la imagen."), isError = true) },
            )
        }

        fun imageProblem(message: String) = showNotice(message, isError = true)

        // endregion

        // region Stripe

        /** Pide el enlace de Stripe; [onLink] lo abre en el navegador. */
        fun openStripe(onLink: (String) -> Unit) {
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) { repository.stripeOnboardingLink(UUID.randomUUID().toString()) }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = onLink,
                    onFailure = { showNotice(storeErrorMessage(it), isError = true) },
                )
            }
        }

        fun setCardPayments(enabled: Boolean) =
            saveStripe(if (enabled) "Ya aceptas pagos con tarjeta" else "Pagos con tarjeta apagados") {
                repository.setCardPayments(enabled, UUID.randomUUID().toString())
            }

        fun setFeePaidByCustomer(paid: Boolean) =
            saveStripe(if (paid) "El cliente paga la comisión" else "Tu negocio paga la comisión") {
                repository.setFeePaidByCustomer(paid, UUID.randomUUID().toString())
            }

        private fun saveStripe(
            success: String,
            call: () -> Result<StripeSetup>,
        ) {
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { call() }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(stripe = it)
                        showNotice(success, isError = false)
                    },
                    onFailure = { showNotice(storeErrorMessage(it), isError = true) },
                )
            }
        }

        // endregion

        // region Cashback

        fun saveCashback(
            rule: CashbackRule,
            onDone: () -> Unit,
        ) {
            CashbackRules.problem(rule)?.let { return showNotice(it, isError = true) }
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        repository.saveCashbackRule(CashbackRules.normalized(rule), UUID.randomUUID().toString())
                    }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(cashback = it, cashbackLoaded = true)
                        showNotice("Cashback guardado", isError = false)
                        onDone()
                    },
                    onFailure = { showNotice(storeErrorMessage(it), isError = true) },
                )
            }
        }

        fun selectWalletPeriod(preset: AdminPeriodPreset) {
            if (preset == _uiState.value.walletPreset && _uiState.value.wallet != null) return
            _uiState.value = _uiState.value.copy(walletPreset = preset)
            refreshWallet()
        }

        private fun refreshWallet() {
            val preset = _uiState.value.walletPreset
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.walletSummary(AdminPeriod.lastDays(preset.days)) }
                if (_uiState.value.walletPreset != preset) return@launch
                result.onSuccess { _uiState.value = _uiState.value.copy(wallet = it) }
            }
        }

        // endregion

        fun notify(
            text: String,
            isError: Boolean,
        ) = showNotice(text, isError)

        fun dismissNotice() {
            noticeJob?.cancel()
            _uiState.value = _uiState.value.copy(notice = null)
        }

        private fun showNotice(
            text: String,
            isError: Boolean,
        ) {
            noticeJob?.cancel()
            _uiState.value = _uiState.value.copy(notice = text, noticeIsError = isError)
            noticeJob =
                viewModelScope.launch {
                    delay(NOTICE_MS)
                    _uiState.value = _uiState.value.copy(notice = null)
                }
        }

        private companion object {
            const val NOTICE_MS = 3_500L
        }
    }

internal fun storeErrorMessage(error: Throwable): String =
    when ((error as? CatalogRepositoryException)?.code) {
        "ACTIVE_BALANCE_EXISTS" -> "Hay clientes con saldo: no se puede quitar el pago con saldo mientras lo tengan."
        "STRIPE_ONBOARDING_REQUIRED" -> "Primero conecta tu cuenta de Stripe."
        "STRIPE_ACCOUNT_NOT_READY" -> "Stripe aún no termina de revisar tu cuenta."
        "STRIPE_ACCOUNT_MODE_MISMATCH" -> "La cuenta de Stripe no corresponde a este entorno."
        else -> error.toUserFacingMessage("No se pudo guardar.")
    }
