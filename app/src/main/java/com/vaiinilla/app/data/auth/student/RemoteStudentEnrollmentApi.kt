package com.vaiinilla.app.data.auth.student

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.data.fixture.MetaDto
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRequest
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
                        ClienteAltaRequestDto(
                            establecimientoId = request.establecimientoId,
                            identificadorContextual = request.identificadorContextual,
                            nombre = request.nombre,
                            aceptacionTerminosEn = request.aceptacionTerminosEn.toString(),
                        ),
                    )
                val raw =
                    apiClient
                        .postWithBearer(
                            bearer = firebaseIdToken,
                            path = "identidades/cliente/alta",
                            body = body,
                        ).getOrElse { error ->
                            if (error is com.vaiinilla.app.core.network.ApiClientException &&
                                error.httpStatus in listOf(404, 501)
                            ) {
                                throw StudentEnrollmentUnavailableException()
                            }
                            throw error
                        }
                val envelope = json.decodeFromString<ClienteAltaEnvelopeDto>(raw)
                StudentEnrollmentResult(membresiaId = envelope.data.membresiaId)
            }
    }

@Serializable
private data class ClienteAltaRequestDto(
    @SerialName("establecimiento_id") val establecimientoId: String,
    @SerialName("identificador_contextual") val identificadorContextual: String?,
    val nombre: String,
    @SerialName("aceptacion_terminos_en") val aceptacionTerminosEn: String,
)

@Serializable
private data class ClienteAltaEnvelopeDto(
    val data: ClienteAltaDataDto,
    val meta: MetaDto,
)

@Serializable
private data class ClienteAltaDataDto(
    @SerialName("membresia_id") val membresiaId: String,
)
