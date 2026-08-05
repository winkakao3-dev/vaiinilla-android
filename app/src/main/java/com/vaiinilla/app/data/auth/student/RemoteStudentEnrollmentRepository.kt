package com.vaiinilla.app.data.auth.student

import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRepository
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRequest
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteStudentEnrollmentRepository
    @Inject
    constructor(
        private val api: StudentEnrollmentApi,
    ) : StudentEnrollmentRepository {
        override suspend fun enroll(
            request: StudentEnrollmentRequest,
            firebaseIdToken: String,
        ): Result<StudentEnrollmentResult> = api.enroll(request, firebaseIdToken)
    }
