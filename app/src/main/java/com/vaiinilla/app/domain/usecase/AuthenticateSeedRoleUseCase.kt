package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.data.auth.FirebaseSeedAuthRepository
import com.vaiinilla.app.domain.model.OperationalRole
import javax.inject.Inject

class AuthenticateSeedRoleUseCase @Inject constructor(
    private val authRepository: FirebaseSeedAuthRepository,
) {
    suspend operator fun invoke(role: OperationalRole): Result<Unit> =
        authRepository.authenticateRole(role)
}
