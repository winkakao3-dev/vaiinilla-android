package com.vaiinilla.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.admin.AssistantPartialFailure
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminWalletSummary
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.domain.admin.AssistantPhoto
import com.vaiinilla.app.domain.admin.AssistantProposal
import com.vaiinilla.app.domain.admin.AssistantUpload
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.CashbackRules
import com.vaiinilla.app.domain.admin.HomeBusinessRules
import com.vaiinilla.app.domain.admin.HomeDeliverySettings
import com.vaiinilla.app.domain.admin.PreorderSettings
import com.vaiinilla.app.domain.admin.StoreConfig
import com.vaiinilla.app.domain.admin.StoreFlow
import com.vaiinilla.app.domain.admin.StoreRules
import com.vaiinilla.app.domain.admin.StoreVideo
import com.vaiinilla.app.domain.admin.StripeSetup
import com.vaiinilla.app.domain.admin.steps
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

/** La plática con el asistente de configuración (marcos AS2 y AS3). */
data class StoreAssistantState(
    val messages: List<AssistantMessage> = emptyList(),
    val options: List<String> = emptyList(),
    /** La última propuesta; se queda aunque el dueño siga platicando. */
    val proposal: AssistantProposal? = null,
    val reviewing: Boolean = false,
    val thinking: Boolean = false,
    val applying: Boolean = false,
    val error: String? = null,
    val remainingMessages: Int = Int.MAX_VALUE,
    /** Las fotos que el dueño mandó en el chat, por id ("foto1"…); se suben al aceptar. */
    val photos: Map<String, AssistantPhoto> = emptyMap(),
    /** La foto elegida que se manda con el siguiente mensaje. */
    val pendingPhotoId: String? = null,
    /** Llave base de la propuesta actual: reintentar con la misma no duplica nada. */
    val proposalKey: String? = null,
    /** Pasos de la propuesta que ya quedaron guardados (por índice), para reintentar solo lo que faltó. */
    val appliedSteps: Set<Int> = emptySet(),
    /** Lo que no se pudo guardar al aceptar: qué y por qué. */
    val failures: List<Pair<String, String>> = emptyList(),
    /** "Guardando 3 de 9" mientras se aplica. */
    val progress: String? = null,
) {
    val canSend: Boolean get() = !thinking && !applying && remainingMessages > 1
    val canAddPhoto: Boolean get() = canSend && photos.size < ASSISTANT_MAX_PHOTOS
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
        private val _assistant = mutableStateOf(StoreAssistantState())
        val assistant: State<StoreAssistantState> = _assistant
        private var noticeJob: Job? = null
        private var scope: String? = null

        fun onVisible(scope: String) {
            if (scope != this.scope) {
                this.scope = scope
                _uiState.value = AdminStoreUiState()
                _assistant.value = StoreAssistantState()
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

        fun setSchoolMode(isSchool: Boolean) =
            saveConfig(
                if (isSchool) {
                    "Ya eres escuela: aparecen Mis hijos, Alumnos y Escuela"
                } else {
                    "Ya no se muestran Mis hijos ni Alumnos"
                },
            ) {
                repository.setSchoolMode(isSchool)
            }

        fun saveHomeDelivery(delivery: HomeDeliverySettings) {
            HomeBusinessRules.problem(delivery)?.let { return showNotice(it, isError = true) }
            saveConfig(if (delivery.enabled) "Entregas a domicilio guardadas" else "Entregas a domicilio apagadas") {
                repository.saveHomeDelivery(delivery)
            }
        }

        fun savePreorders(preorders: PreorderSettings) {
            HomeBusinessRules.problem(preorders)?.let { return showNotice(it, isError = true) }
            saveConfig("Encargos guardados") { repository.savePreorders(preorders) }
        }

        fun saveStoreLocation(
            latitude: Double,
            longitude: Double,
        ) = saveConfig("Ubicación guardada") { repository.saveStoreLocation(latitude, longitude) }

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

        // region Asistente de configuración

        /** Al abrir el asistente: si todavía no hay plática, que salude y haga la primera pregunta. */
        fun startAssistant() {
            val current = _assistant.value
            if (current.messages.isEmpty() && !current.thinking) askAssistant(emptyList())
        }

        fun sendToAssistant(text: String) {
            val current = _assistant.value
            val photo = current.pendingPhotoId?.let { " [$it]" }.orEmpty()
            val clean = (text.trim().take(ASSISTANT_MAX_LETTERS - photo.length) + photo).trim()
            if (clean.isEmpty() || !current.canSend) return
            val messages = current.messages + AssistantMessage(fromOwner = true, text = clean)
            _assistant.value =
                current.copy(messages = messages, options = emptyList(), error = null, pendingPhotoId = null)
            askAssistant(messages)
        }

        /** Una foto elegida en el chat: queda lista para mandarse con el siguiente mensaje. */
        fun addAssistantPhoto(
            image: AssistantUpload,
            logo: AssistantUpload,
        ) {
            val current = _assistant.value
            if (!current.canAddPhoto) return
            current.pendingPhotoId?.let { removeAssistantPhoto() }
            val now = _assistant.value
            val id = "foto${(
                now.photos.keys
                    .mapNotNull {
                        it
                            .removePrefix(
                                "foto",
                            ).toIntOrNull()
                    }.maxOrNull() ?: 0
            ) + 1}"
            _assistant.value =
                now.copy(
                    photos = now.photos + (id to AssistantPhoto(id, image, logo)),
                    pendingPhotoId = id,
                    error = null,
                )
        }

        /** Quita la foto que aún no se manda. */
        fun removeAssistantPhoto() {
            val current = _assistant.value
            val id = current.pendingPhotoId ?: return
            _assistant.value = current.copy(photos = current.photos - id, pendingPhotoId = null)
        }

        fun photoPreparationFailed() {
            _assistant.value = _assistant.value.copy(error = "No se pudo preparar la foto. Prueba otra de hasta 16 MB.")
        }

        /** Vuelve a pedir la respuesta que falló. */
        fun retryAssistant() {
            val current = _assistant.value
            if (current.thinking) return
            if (current.messages.isEmpty() || current.messages.last().fromOwner) askAssistant(current.messages)
        }

        fun reviewProposal(show: Boolean) {
            val current = _assistant.value
            if (show && current.proposal == null) return
            _assistant.value = current.copy(reviewing = show, error = null, failures = emptyList())
        }

        /** Empieza otra plática desde cero (la propuesta anterior se descarta). */
        fun restartAssistant() {
            if (_assistant.value.thinking || _assistant.value.applying) return
            _assistant.value = StoreAssistantState()
            askAssistant(emptyList())
        }

        /**
         * Aplica la propuesta paso por paso con las rutas de siempre. Si algo falla sigue con lo demás y enseña qué
         * faltó; aceptar otra vez reintenta solo eso, con las mismas llaves. [onDone] cierra el asistente.
         */
        fun acceptProposal(onDone: () -> Unit) {
            val current = _assistant.value
            val proposal = current.proposal ?: return
            if (current.applying) return
            val baseKey = current.proposalKey ?: UUID.randomUUID().toString()
            val steps = proposal.steps()
            _assistant.value =
                current.copy(applying = true, error = null, failures = emptyList(), proposalKey = baseKey)
            viewModelScope.launch {
                var applied = current.appliedSteps
                val failures = mutableListOf<Pair<String, String>>()
                steps.forEachIndexed { index, step ->
                    if (index in applied) return@forEachIndexed
                    _assistant.value = _assistant.value.copy(progress = "Guardando ${index + 1} de ${steps.size}")
                    val result =
                        withContext(Dispatchers.IO) {
                            repository.applyAssistantStep(step, _assistant.value.photos, "$baseKey-$index")
                        }
                    result.fold(
                        onSuccess = { applied = applied + index },
                        onFailure = {
                            // Ya estaba invitado: no es un error, ya quedó.
                            if ((it as? CatalogRepositoryException)?.code in ALREADY_INVITED) {
                                applied = applied + index
                            } else {
                                failures += step.label to assistantStepError(it)
                            }
                        },
                    )
                }
                if (proposal.patchBody != null || proposal.storePhotos.isNotEmpty()) refresh()
                if (failures.isEmpty()) {
                    _assistant.value = StoreAssistantState()
                    showNotice("Tu tienda quedó configurada", isError = false)
                    onDone()
                } else {
                    _assistant.value =
                        _assistant.value.copy(
                            applying = false,
                            progress = null,
                            appliedSteps = applied,
                            failures = failures,
                        )
                }
            }
        }

        private fun askAssistant(messages: List<AssistantMessage>) {
            _assistant.value = _assistant.value.copy(thinking = true, error = null)
            viewModelScope.launch {
                val photoIds =
                    _assistant.value.photos.keys
                        .toList()
                val result = withContext(Dispatchers.IO) { repository.assistantReply(messages, photoIds) }
                val current = _assistant.value
                _assistant.value =
                    result.fold(
                        onSuccess = { reply ->
                            current.copy(
                                messages = messages + AssistantMessage(fromOwner = false, text = reply.message),
                                options = reply.options,
                                proposal = reply.proposal ?: current.proposal,
                                reviewing = reply.proposal != null,
                                // Una propuesta nueva reemplaza a la anterior: se aplica desde cero.
                                proposalKey = if (reply.proposal != null) null else current.proposalKey,
                                appliedSteps = if (reply.proposal != null) emptySet() else current.appliedSteps,
                                failures = if (reply.proposal != null) emptyList() else current.failures,
                                thinking = false,
                                remainingMessages = reply.remainingMessages,
                            )
                        },
                        onFailure = { current.copy(thinking = false, error = assistantErrorMessage(it)) },
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

internal fun assistantErrorMessage(error: Throwable): String =
    when ((error as? CatalogRepositoryException)?.code) {
        "ASSISTANT_UNAVAILABLE" -> "El asistente no está disponible ahorita. Puedes configurar tu tienda paso a paso."
        "ASSISTANT_CONVERSATION_TOO_LONG" -> "La plática ya está muy larga. Revisa la propuesta o empieza de nuevo."
        "RATE_LIMITED" -> "Muchos mensajes seguidos. Espera unos minutos y vuelve a intentar."
        else -> error.toUserFacingMessage("No se pudo hablar con el asistente. Vuelve a intentar.")
    }

private const val ASSISTANT_MAX_LETTERS = 2000
private const val ASSISTANT_MAX_PHOTOS = 20
private val ALREADY_INVITED = setOf("INVITATION_PENDING_EXISTS", "MEMBERSHIP_ROLE_EXISTS")

internal fun assistantStepError(error: Throwable): String =
    when {
        error is AssistantPartialFailure -> "Faltó: ${error.items.joinToString(", ")}"
        else -> storeErrorMessage(error)
    }

internal fun storeErrorMessage(error: Throwable): String =
    when ((error as? CatalogRepositoryException)?.code) {
        "ACTIVE_BALANCE_EXISTS" -> "Hay clientes con saldo: no se puede quitar el pago con saldo mientras lo tengan."
        "STRIPE_ONBOARDING_REQUIRED" -> "Primero conecta tu cuenta de Stripe."
        "STRIPE_ACCOUNT_NOT_READY" -> "Stripe aún no termina de revisar tu cuenta."
        "STRIPE_ACCOUNT_MODE_MISMATCH" -> "La cuenta de Stripe no corresponde a este entorno."
        else -> error.toUserFacingMessage("No se pudo guardar.")
    }
