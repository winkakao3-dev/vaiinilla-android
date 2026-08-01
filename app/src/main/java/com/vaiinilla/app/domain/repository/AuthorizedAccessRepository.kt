package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext

interface AuthorizedAccessRepository {
    suspend fun invitation(token: String): Result<AuthorizedInvitation>

    suspend fun acceptInvitation(
        token: String,
        session: StudentAuthSession,
    ): Result<AuthorizedMode>

    suspend fun authorizedModes(session: StudentAuthSession): Result<List<AuthorizedMode>>

    suspend fun activateMode(
        mode: AuthorizedMode,
        session: StudentAuthSession,
    ): Result<AuthorizedModeContext>

    suspend fun revokeMode(
        mode: AuthorizedMode,
        session: StudentAuthSession,
    ): Result<Unit>
}
