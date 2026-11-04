package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.offline.OfflineStaffOrderStore
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lo guardado sin internet no depende de que el mesero vuelva a abrir su modo: si mataron la app, reiniciaron el
 * teléfono o cambiaron de modo, en cuanto la cuenta tiene red y sus modos cargados se pide un token de esa
 * membresía (el contexto es un JWT sin estado: no cierra el modo que esté abierto) y se manda con él.
 */
@Singleton
class OfflineStaffOrderRescuer
    @Inject
    constructor(
        private val queue: OfflineStaffOrderQueue,
        private val store: OfflineStaffOrderStore,
        private val access: AuthorizedAccessRepository,
        private val auth: StudentAuthRepository,
        private val apiClient: HttpVaiinillaApiClient,
        connectivity: ConnectivityWatcher,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var job: Job? = null

        @Volatile
        private var modes: List<AuthorizedMode> = emptyList()

        init {
            connectivity.onReconnect { launch() }
        }

        /** Con los modos de la cuenta recién cargados (o al cambiar de modo). */
        fun rescue(available: List<AuthorizedMode>) {
            modes = available
            launch()
        }

        @Synchronized
        private fun launch() {
            if (job?.isActive == true) return
            val current = modes
            if (current.isEmpty()) return
            job = scope.launch { rescueAll(current) }
        }

        private suspend fun rescueAll(available: List<AuthorizedMode>) {
            val session = auth.peekSession() ?: return
            for (saved in store.scopesWithOrders()) {
                val mode =
                    available.firstOrNull {
                        it.membershipId.equals(saved.membershipId, ignoreCase = true) && it.role in STAFF_ROLES
                    } ?: continue
                val token = access.activateMode(mode, session).getOrNull()?.accessToken ?: continue
                queue.rescue(saved) { body, key ->
                    apiClient
                        .postWithBearer(
                            bearer = token,
                            path = "pedidos",
                            body = body,
                            headers = mapOf("Idempotency-Key" to key),
                        ).map { }
                }
            }
        }

        private companion object {
            val STAFF_ROLES = setOf(OperationalRole.WAITER, OperationalRole.CASHIER, OperationalRole.ADMIN)
        }
    }
