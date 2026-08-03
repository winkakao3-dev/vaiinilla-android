package com.vaiinilla.app.domain.auth.student

interface StudentEnrollmentRepository {
    suspend fun enroll(
        request: StudentEnrollmentRequest,
        firebaseIdToken: String,
    ): Result<StudentEnrollmentResult>
}
