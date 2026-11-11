package com.vaiinilla.app.ui.notices

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.notifications.NoticeExplanation
import com.vaiinilla.app.core.notifications.NoticeOrderLookup
import com.vaiinilla.app.core.notifications.ReceivedNotice
import com.vaiinilla.app.core.notifications.ReceivedNoticeLog
import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.domain.mode.AuthorizedMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class ReceivedNoticesViewModel
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val lookup: NoticeOrderLookup,
        private val venueContext: VenueContextStore,
    ) : ViewModel() {
        val notices: StateFlow<List<ReceivedNotice>> = ReceivedNoticeLog.notices(appContext)

        private val _searching = MutableStateFlow<Set<String>>(emptySet())

        /** Los avisos cuyo pedido se está buscando ahora. */
        val searching: StateFlow<Set<String>> = _searching.asStateFlow()

        /** Una búsqueda a la vez: cada una pide un token por modo y con varios avisos en pantalla serían ráfagas. */
        private val oneAtATime = Mutex()

        /** Busca el pedido del aviso con los modos que la cuenta tiene ahora y guarda lo que encontró. */
        fun lookUp(
            notice: ReceivedNotice,
            modes: List<AuthorizedMode>,
        ) {
            val explanation = NoticeExplanation.explain(venueContext.current, notice.payload)
            val orderId = explanation.orderId ?: return
            if (notice.id in _searching.value) return
            _searching.update { it + notice.id }
            viewModelScope.launch {
                try {
                    val result =
                        oneAtATime.withLock {
                            lookup.lookUp(orderId, explanation.roles, explanation.establishmentId, modes)
                        }
                    ReceivedNoticeLog.saveLookup(appContext, notice.id, result)
                } finally {
                    _searching.update { it - notice.id }
                }
            }
        }

        /**
         * Al aparecer en la lista, busca el pedido si nunca se buscó (AVISOS-02), para que la fila diga negocio,
         * folio y total sin tocarla. Lo que ya no se pudo leer se vuelve a intentar solo a mano.
         */
        fun resolveIfNeeded(
            notice: ReceivedNotice,
            modes: List<AuthorizedMode>,
        ) {
            if (notice.lookup != null || modes.isEmpty()) return
            lookUp(notice, modes)
        }

        fun clear() = ReceivedNoticeLog.clear(appContext)
    }
