package com.vaiinilla.app.data.auth.student

import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRepository
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRequest
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwitchingStudentAuthRepository
    @Inject
    constructor(
        private val resolver: EffectiveDataSourceResolver,
        private val fixture: FixtureStudentAuthRepository,
        private val firebase: FirebaseStudentAuthRepository,
    ) : StudentAuthRepository {
        private fun active(): StudentAuthRepository =
            when (resolver.effectiveMode()) {
                DataSourceMode.MOCK -> fixture
                DataSourceMode.REMOTE -> firebase
            }

        override fun peekSession(): StudentAuthSession? = active().peekSession()

        override fun isReadyForCheckout(establishmentId: String?): Boolean =
            active().isReadyForCheckout(establishmentId)

        override suspend fun signUp(
            email: String,
            password: String,
            displayName: String,
        ): Result<StudentAuthSession> = active().signUp(email, password, displayName)

        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<StudentAuthSession> = active().signIn(email, password)

        override suspend fun sendEmailVerification(): Result<Unit> = active().sendEmailVerification()

        override suspend fun reloadSession(): Result<StudentAuthSession?> = active().reloadSession()

        override suspend fun sendPasswordReset(email: String): Result<Unit> = active().sendPasswordReset(email)

        override suspend fun getIdToken(forceRefresh: Boolean): Result<String> = active().getIdToken(forceRefresh)

        override suspend fun signOut() = active().signOut()
    }

@Singleton
class SwitchingStudentEnrollmentRepository
    @Inject
    constructor(
        private val resolver: EffectiveDataSourceResolver,
        private val fixture: FixtureStudentEnrollmentRepository,
        private val remote: RemoteStudentEnrollmentRepository,
    ) : StudentEnrollmentRepository {
        private fun active(): StudentEnrollmentRepository =
            when (resolver.effectiveMode()) {
                DataSourceMode.MOCK -> fixture
                DataSourceMode.REMOTE -> remote
            }

        override suspend fun enroll(
            request: StudentEnrollmentRequest,
            firebaseIdToken: String,
        ): Result<StudentEnrollmentResult> = active().enroll(request, firebaseIdToken)
    }
