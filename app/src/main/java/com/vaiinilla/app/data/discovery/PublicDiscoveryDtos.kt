package com.vaiinilla.app.data.discovery

import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.domain.model.PublicVideo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicEstablishmentDto(
    val id: String,
    val nombre: String,
    val slug: String,
    @SerialName("identificador_cliente_etiqueta") val clientIdLabel: String,
    @SerialName("identificador_cliente_obligatorio") val clientIdRequired: Boolean,
    @SerialName("videos_local") val localVideos: List<PublicVideoDto>? = null,
    @SerialName("imagen_url") val photoUrl: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
)

@Serializable
data class PublicVideoDto(
    val url: String? = null,
    @SerialName("titulo") val title: String? = null,
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
        localVideos = localVideos.orEmpty().mapNotNull { it.toDomainOrNull() },
        photoUrl = photoUrl.httpsOrNull(),
        logoUrl = logoUrl.httpsOrNull(),
    )

// Las imágenes se descargan sin sesión: solo https.
private fun String?.httpsOrNull(): String? = this?.trim()?.takeIf { it.startsWith("https://", ignoreCase = true) }

// Solo http(s): la URL llega a un ACTION_VIEW, así que descarta esquemas como intent: o javascript:.
private fun PublicVideoDto.toDomainOrNull(): PublicVideo? {
    val link = url?.trim().orEmpty()
    val videoTitle = title?.trim().orEmpty()
    val isWebLink = link.startsWith("https://", ignoreCase = true) || link.startsWith("http://", ignoreCase = true)
    if (!isWebLink || videoTitle.isEmpty()) return null
    return PublicVideo(url = link, title = videoTitle)
}

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
