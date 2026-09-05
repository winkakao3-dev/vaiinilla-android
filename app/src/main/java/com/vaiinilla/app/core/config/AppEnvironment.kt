package com.vaiinilla.app.core.config

data class AppEnvironment(
    val environmentName: String,
    val apiBaseUrl: String,
    val webUrl: String,
    val firebaseProjectId: String,
    val isProduction: Boolean,
    val versionName: String,
    val versionCode: Int,
)
