package com.vaiinilla.app.core.config

enum class DataSourceMode {
    MOCK,
    REMOTE;

    companion object {
        fun from(raw: String): DataSourceMode = entries.firstOrNull {
            it.name.equals(raw.trim(), ignoreCase = true)
        } ?: throw IllegalArgumentException("Modo de datos no soportado: $raw")
    }
}

data class AppEnvironment(
    val dataSourceMode: DataSourceMode,
    val apiBaseUrl: String,
)
