package com.vaiinilla.app.core.network

/**
 * Converts transport failures into copy that is safe and useful to show in the UI.
 * API/domain messages are preserved because they are already written for the user.
 */
fun Throwable?.toUserFacingMessage(fallback: String = DEFAULT_ERROR_MESSAGE): String =
    ApiErrorMapper.map(this).message(fallback)
