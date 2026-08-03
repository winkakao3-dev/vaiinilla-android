package com.vaiinilla.app.ui.mode

import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext

data class AuthorizedAccessUiState(
    val loading: Boolean = false,
    val session: StudentAuthSession? = null,
    val invitationToken: String? = null,
    val invitation: AuthorizedInvitation? = null,
    val modes: List<AuthorizedMode> = emptyList(),
    val activeContext: AuthorizedModeContext? = null,
    val errorMessage: String? = null,
    val message: String? = null,
) {
    val hasMultipleModes: Boolean
        get() = modes.size > 1
}
