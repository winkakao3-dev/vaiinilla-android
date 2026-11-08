package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.data.outbox.OutboxDrainer
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.mode.AuthorizedMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lo guardado sin internet no depende de que el mesero vuelva a abrir su modo: con la app abierta, en cuanto la
 * cuenta tiene red y sus modos cargados se recorre la bandeja de salida ([OutboxDrainer]). Con la app cerrada lo
 * mismo lo hace WorkManager.
 */
@Singleton
class OfflineStaffOrderRescuer
    @Inject
    constructor(
        private val drainer: OutboxDrainer,
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
            job = scope.launch { drainer.drain(current) }
        }
    }
