package com.vaiinilla.app.data.mode

import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwitchingAuthorizedAccessRepository
    @Inject
    constructor(
        private val resolver: EffectiveDataSourceResolver,
        private val fixture: FixtureAuthorizedAccessRepository,
        private val remote: RemoteAuthorizedAccessRepository,
    ) : AuthorizedAccessRepository {
        private fun active(): AuthorizedAccessRepository =
            when (resolver.effectiveMode()) {
                DataSourceMode.MOCK -> fixture
                DataSourceMode.REMOTE -> remote
            }

        override suspend fun invitation(token: String): Result<AuthorizedInvitation> = active().invitation(token)

        override suspend fun acceptInvitation(
            token: String,
            session: StudentAuthSession,
        ): Result<AuthorizedMode> = active().acceptInvitation(token, session)

        override suspend fun authorizedModes(session: StudentAuthSession): Result<List<AuthorizedMode>> =
            active().authorizedModes(session)

        override suspend fun activateMode(
            mode: AuthorizedMode,
            session: StudentAuthSession,
        ): Result<AuthorizedModeContext> = active().activateMode(mode, session)

        override suspend fun revokeMode(
            mode: AuthorizedMode,
            session: StudentAuthSession,
        ): Result<Unit> = active().revokeMode(mode, session)
    }
