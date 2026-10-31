package com.vaiinilla.app.ui.notices

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.notifications.NoticeExplanation
import com.vaiinilla.app.core.notifications.NoticeOrderLookup
import com.vaiinilla.app.core.notifications.ReceivedNotice
import com.vaiinilla.app.core.notifications.ReceivedNoticeLog
import com.vaiinilla.app.domain.mode.AuthorizedMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceivedNoticesViewModel
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val lookup: NoticeOrderLookup,
    ) : ViewModel() {
        val notices: StateFlow<List<ReceivedNotice>> = ReceivedNoticeLog.notices(appContext)

        private val _searching = MutableStateFlow<Set<String>>(emptySet())

        /** Los avisos cuyo pedido se está buscando ahora. */
        val searching: StateFlow<Set<String>> = _searching.asStateFlow()

        /** Busca el pedido del aviso con los modos que la cuenta tiene ahora y guarda lo que encontró. */
        fun lookUp(
            notice: ReceivedNotice,
            modes: List<AuthorizedMode>,
        ) {
            val explanation = NoticeExplanation.explain(notice.payload, notice.isRemote)
            val orderId = explanation.orderId ?: return
            if (notice.id in _searching.value) return
            _searching.update { it + notice.id }
            viewModelScope.launch {
                try {
                    val result = lookup.lookUp(orderId, explanation.roles, explanation.establishmentId, modes)
                    ReceivedNoticeLog.saveLookup(appContext, notice.id, result)
                } finally {
                    _searching.update { it - notice.id }
                }
            }
        }

        fun clear() = ReceivedNoticeLog.clear(appContext)
    }
