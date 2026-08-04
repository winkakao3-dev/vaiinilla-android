package com.vaiinilla.app.data.auth

import com.vaiinilla.app.data.fixture.MetaDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SesionesContextoEnvelopeDto(
    val data: SesionesContextoDataDto,
    val meta: MetaDto,
    val error: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
data class SesionesContextoDataDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    val contexto: SesionesContextoContextDto? = null,
)

@Serializable
data class SesionesContextoContextDto(
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("membresia_id") val membresiaId: String,
    @SerialName("establecimiento_id") val establecimientoId: String,
    val rol: String,
    @SerialName("modo_restringido") val modoRestringido: String? = null,
)
