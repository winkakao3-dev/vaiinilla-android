package com.vaiinilla.app.data.discovery

import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicEstablishmentDto(
    val id: String,
    val nombre: String,
    val slug: String,
    @SerialName("identificador_cliente_etiqueta") val clientIdLabel: String,
    @SerialName("identificador_cliente_obligatorio") val clientIdRequired: Boolean,
)

@Serializable
data class PublicSpaceDto(
    val id: Int,
    val nombre: String,
    val tipo: String,
)

@Serializable
data class SpaceResolveDataDto(
    val establecimiento: PublicEstablishmentDto,
    val espacio: PublicSpaceDto? = null,
)

fun PublicEstablishmentDto.toDomain() =
    PublicEstablishment(
        id = id,
        name = nombre,
        slug = slug,
        clientIdLabel = clientIdLabel,
        clientIdRequired = clientIdRequired,
    )

fun PublicSpaceDto.toDomain() =
    PublicSpace(
        id = id,
        name = nombre,
        type = tipo,
    )

@Serializable
data class EstablishmentListEnvelope(
    val data: List<PublicEstablishmentDto>,
    val meta: PublicMetaDto? = null,
)

@Serializable
data class EstablishmentDetailEnvelope(
    val data: PublicEstablishmentDto,
)

@Serializable
data class SpaceResolveEnvelope(
    val data: SpaceResolveDataDto,
)

@Serializable
data class SpaceResolveRequestDto(
    val token: String,
)

@Serializable
data class PublicMetaDto(
    val cursor: String? = null,
)
