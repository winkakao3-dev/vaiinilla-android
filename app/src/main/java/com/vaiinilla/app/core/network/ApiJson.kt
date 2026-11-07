package com.vaiinilla.app.core.network

import kotlinx.serialization.json.Json

/**
 * Los `Json` de la app, definidos una sola vez. Todos toleran claves desconocidas (el servidor puede
 * agregar campos sin romper versiones viejas) y solo difieren en cómo escriben: con o sin los valores por
 * defecto y con o sin los nulos explícitos, que el backend trata distinto según el endpoint.
 */
object ApiJson {
    /** Lee tolerante; al escribir omite los valores por defecto y escribe los nulos. */
    val plain: Json = Json { ignoreUnknownKeys = true }

    val withDefaults: Json =
        Json(plain) {
            encodeDefaults = true
        }

    val omitNulls: Json =
        Json(plain) {
            explicitNulls = false
        }

    val omitNullsWithDefaults: Json =
        Json(plain) {
            explicitNulls = false
            encodeDefaults = true
        }

    val coercing: Json =
        Json(plain) {
            coerceInputValues = true
        }

    val omitNullsCoercing: Json =
        Json(plain) {
            explicitNulls = false
            coerceInputValues = true
        }

    /** Plataforma: el dinero llega como texto o número según la consulta, y los valores raros se coaccionan. */
    val lenientCoercing: Json =
        Json(plain) {
            coerceInputValues = true
            isLenient = true
        }
}
