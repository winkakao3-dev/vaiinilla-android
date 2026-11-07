package com.vaiinilla.app.core.network

const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

/**
 * La única forma de armar la cabecera de idempotencia. La llave la decide quien llama: derivada de la
 * operación (`MutationIdempotency`) o guardada hasta que el servidor confirme (`RetryKeys`), para que un
 * reintento de la misma operación mande la misma llave.
 */
fun idempotencyHeaders(key: String): Map<String, String> = mapOf(IDEMPOTENCY_KEY_HEADER to key)
