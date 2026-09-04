package com.vaiinilla.app

import android.app.Application
import android.net.http.HttpResponseCache
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class VaiinillaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installHttpResponseCache()
    }

    private fun installHttpResponseCache() {
        try {
            val httpCacheDir = File(cacheDir, "http_cache")
            val httpCacheSize = 25L * 1024L * 1024L // 25 MiB
            if (HttpResponseCache.getInstalled() == null) {
                HttpResponseCache.install(httpCacheDir, httpCacheSize)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo inicializar HttpResponseCache", e)
        }
    }

    private companion object {
        const val TAG = "VaiinillaApp"
    }
}
