package com.vaiinilla.app.ui.auth.student

import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.model.GuestVenueContext
import java.time.Instant

data class StudentAuthUiState(
    val loading: Boolean = false,
    val session: StudentAuthSession? = null,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val contextualId: String = "",
    val termsAccepted: Boolean = false,
    val termsAcceptedAt: Instant? = null,
    val errorMessage: String? = null,
    val emailExistsSuggestion: Boolean = false,
    val verificationSent: Boolean = false,
    val passwordResetSent: Boolean = false,
    val enrollmentComplete: Boolean = false,
    val guestVenue: GuestVenueContext? = null,
    val clientIdLabel: String = "Identificador",
    val clientIdRequired: Boolean = false,
)
