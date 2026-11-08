package com.vaiinilla.app.data.outbox

import android.util.Log
import com.vaiinilla.app.data.mode.StaffOfflineContextStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.outbox.DrainOutcome
import com.vaiinilla.app.domain.outbox.OutboxContext
import com.vaiinilla.app.domain.outbox.OutboxDispatcher
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arma lo que hace falta para mandar lo guardado (la sesión, los modos y un token por modo) y recorre las colas.
 * Lo usan la app abierta ([com.vaiinilla.app.ui.operational.OfflineStaffOrderRescuer]) y WorkManager con la app
 * cerrada; en los dos casos el recorrido es el mismo.
 */
@Singleton
class OutboxDrainer
    @Inject
    constructor(
        private val auth: StudentAuthRepository,
        private val access: AuthorizedAccessRepository,
        private val offlineContext: StaffOfflineContextStore,
        staffOrders: StaffOrderOutboxSource,
        counterPurchases: CounterPurchaseOutboxSource,
    ) {
        private val dispatcher =
            OutboxDispatcher(
                sources = listOf(staffOrders, counterPurchases),
                onSourceFailure = { name, error -> Log.w(TAG, "La bandeja falló con $name", error) },
            )

        /**
         * [knownModes] vacío (la app arrancó en frío para el trabajo): se piden al servidor y, sin red para eso,
         * se usan los últimos que guardó el teléfono.
         */
        suspend fun drain(knownModes: List<AuthorizedMode> = emptyList()): DrainOutcome {
            if (!dispatcher.hasWork()) return DrainOutcome.DONE
            // Sin sesión no hay con qué mandar: al volver a entrar la app pide otro recorrido.
            val session = auth.peekSession() ?: return DrainOutcome.DONE
            val modes =
                knownModes.ifEmpty {
                    access.authorizedModes(session).getOrNull()
                        ?: offlineContext.load(session.uid)?.modes.orEmpty()
                }
            if (modes.isEmpty()) return DrainOutcome.RETRY_LATER
            val context =
                OutboxContext(session, modes) { mode ->
                    access.activateMode(mode, session).getOrNull()?.accessToken
                }
            return dispatcher.drainAll(context)
        }

        private companion object {
            const val TAG = "OutboxDrainer"
        }
    }
