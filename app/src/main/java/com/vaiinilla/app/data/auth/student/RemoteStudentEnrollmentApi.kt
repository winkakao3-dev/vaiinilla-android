package com.vaiinilla.app.data.auth.student

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRequest
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface StudentEnrollmentApi {
    fun enroll(
        request: StudentEnrollmentRequest,
        firebaseIdToken: String,
    ): Result<StudentEnrollmentResult>
}

@Singleton
class RemoteStudentEnrollmentApi
    @Inject
    constructor(
        private val apiClient: HttpVaiinillaApiClient,
    ) : StudentEnrollmentApi {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        override fun enroll(
            request: StudentEnrollmentRequest,
            firebaseIdToken: String,
        ): Result<StudentEnrollmentResult> =
            runCatching {
                val body =
                    json.encodeToString(
                        AltaIdentidadRequestDto(
                            nombre = request.nombre,
                            terminosVersion = request.terminosVersion,
                            privacidadVersion = request.privacidadVersion,
                        ),
                    )
                val raw =
                    apiClient
                        .postWithBearer(
                            bearer = firebaseIdToken,
                            path = "identidad/alta",
                            body = body,
                            headers =
                                mapOf(
                                    "Idempotency-Key" to
                                        UUID
                                            .nameUUIDFromBytes(
                                                "vaiinilla:vai26:identidad:${request.nombre}:${request.terminosVersion}:${request.privacidadVersion}"
                                                    .toByteArray(StandardCharsets.UTF_8),
                                            ).toString(),
                                ),
                        ).getOrElse { error ->
                            if (error is com.vaiinilla.app.core.network.ApiClientException &&
                                error.httpStatus in listOf(404, 501)
                            ) {
                                throw StudentEnrollmentUnavailableException()
                            }
                            throw error
                        }
                json.decodeFromString<AltaIdentidadResponseDto>(raw)
                StudentEnrollmentResult()
            }
    }

@Serializable
private data class AltaIdentidadRequestDto(
    val nombre: String,
    @SerialName("terminos_version") val terminosVersion: String,
    @SerialName("privacidad_version") val privacidadVersion: String,
)

@Serializable
private data class AltaIdentidadResponseDto(
    val usuario: JsonObject,
    val consentimiento: JsonObject,
)
