package com.vaiinilla.app.ui.parentmode

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.notifications.DependentNamesStore
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentAccountFactory
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Lo que el Modo Padre necesita del contenedor. */
class ParentModeServices(
    val guardianship: GuardianshipRepository,
    val accounts: DependentAccountFactory,
    val catalog: CatalogRepository,
)

/** "Mis hijos" en la cartera del tutor. */
class ChildrenState(
    val services: ParentModeServices,
    private val onNamesChanged: (Map<String, String>) -> Unit = {},
) {
    var children by mutableStateOf<List<Dependent>>(emptyList())
        private set
    var hasLoaded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** `null` mientras se consulta. Si la consulta falla se deja pasar: el servidor decide al agregar. */
    var isParentUnlocked by mutableStateOf<Boolean?>(null)
        private set

    /** El aviso se abre solo una vez por visita; después queda la tarjeta. */
    var offeredConsent = false

    /** Hijos para los que falta aceptar el aviso de privacidad vigente. */
    val childrenNeedingConsent: List<Dependent> get() = children.filter(Dependent::needsConsent)

    suspend fun load() {
        withContext(Dispatchers.IO) { services.guardianship.dependents() }
            .onSuccess {
                replace(it)
                error = null
            }.onFailure {
                if (children.isEmpty()) error = it.toUserFacingMessage("No pudimos traer a tus hijos.")
            }
        hasLoaded = true
    }

    /** Si ya dio el código de la escuela; quien entró con "Usar código" llega habilitado. */
    suspend fun loadParentAccess() {
        val status = withContext(Dispatchers.IO) { services.catalog.getOperationalStatus() }.getOrNull()
        isParentUnlocked = status?.isParentUnlocked ?: true
    }

    fun markParentUnlocked() {
        isParentUnlocked = true
    }

    /** La respuesta trae a todos los hijos ya actualizados: reemplaza la lista. */
    suspend fun acceptPrivacyNotice(
        version: String,
        idempotencyKey: String,
    ): Result<Unit> =
        withContext(Dispatchers.IO) { services.guardianship.acceptPrivacyNotice(version, idempotencyKey) }
            .map { replace(it) }

    fun upsert(dependent: Dependent) {
        val index = children.indexOfFirst { it.userId == dependent.userId }
        replace(if (index >= 0) children.toMutableList().also { it[index] = dependent } else children + dependent)
    }

    fun remove(dependentId: String) {
        replace(children.filterNot { it.userId == dependentId })
    }

    /** Cierre de sesión: nada de los hijos de quien salió queda en memoria para el siguiente. */
    fun reset() {
        children = emptyList()
        hasLoaded = false
        error = null
        isParentUnlocked = null
        offeredConsent = false
    }

    private fun replace(list: List<Dependent>) {
        children = list
        onNamesChanged(list.associate { it.userId to it.firstName })
    }
}

@HiltViewModel
class ChildrenViewModel
    @Inject
    constructor(
        guardianship: GuardianshipRepository,
        accounts: DependentAccountFactory,
        catalog: CatalogRepository,
        @ApplicationContext context: Context,
    ) : ViewModel() {
        val state =
            ChildrenState(ParentModeServices(guardianship, accounts, catalog)) { names ->
                DependentNamesStore.replace(context, names)
            }

        fun clearForSessionTermination() = state.reset()
    }
