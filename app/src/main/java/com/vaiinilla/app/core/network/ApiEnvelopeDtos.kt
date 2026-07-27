package com.vaiinilla.app.core.network

import com.vaiinilla.app.data.fixture.MetaDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
)

@Serializable
data class ErrorEnvelopeDto(
    val data: JsonElement? = null,
    val meta: MetaDto,
    val error: ApiErrorDto? = null,
)
