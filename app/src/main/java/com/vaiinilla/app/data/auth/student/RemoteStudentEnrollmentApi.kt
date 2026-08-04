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
                val legalVersions = fetchCurrentLegalVersions().getOrThrow()
                val body =
                    json.encodeToString(
                        AltaIdentidadRequestDto(
                            nombre = request.nombre,
                            terminosVersion = legalVersions.terminosVersion,
                            privacidadVersion = legalVersions.privacidadVersion,
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
                                                "vaiinilla:vai26:identidad:${request.nombre}:${legalVersions.terminosVersion}:${legalVersions.privacidadVersion}"
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
                json.decodeFromString<AltaIdentidadEnvelopeDto>(raw).data
                StudentEnrollmentResult()
            }

        private fun fetchCurrentLegalVersions(): Result<LegalVersionsDto> =
            runCatching {
                val raw = apiClient.getPublic("publico/legal/vigente").getOrThrow()
                json.decodeFromString<LegalVersionsEnvelopeDto>(raw).data.also { versions ->
                    require(versions.terminosVersion.isNotBlank()) {
                        "El backend no devolvió la versión vigente de términos."
                    }
                    require(versions.privacidadVersion.isNotBlank()) {
                        "El backend no devolvió la versión vigente de privacidad."
                    }
                }
            }
    }

@Serializable
private data class AltaIdentidadEnvelopeDto(
    val data: AltaIdentidadResponseDto,
)

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

@Serializable
private data class LegalVersionsEnvelopeDto(
    val data: LegalVersionsDto,
)

@Serializable
private data class LegalVersionsDto(
    @SerialName("terminos_version") val terminosVersion: String,
    @SerialName("privacidad_version") val privacidadVersion: String,
)
