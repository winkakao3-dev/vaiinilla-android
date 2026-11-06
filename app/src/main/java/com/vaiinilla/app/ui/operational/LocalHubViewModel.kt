package com.vaiinilla.app.ui.operational

import androidx.lifecycle.ViewModel
import com.vaiinilla.app.domain.localhub.LocalTicketState
import com.vaiinilla.app.domain.model.OperationalRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Puente de las pantallas con la Caja del local; el concentrador en sí vive lo que vive la app. */
@HiltViewModel
class LocalHubViewModel
    @Inject
    constructor(
        private val coordinator: LocalHubCoordinator,
    ) : ViewModel() {
        val state: StateFlow<LocalHubState> = coordinator.state

        fun bind(
            role: OperationalRole?,
            establishmentId: String?,
        ) = coordinator.bind(role, establishmentId)

        fun markReady(id: String) = coordinator.markTicket(id, LocalTicketState.READY)
    }
