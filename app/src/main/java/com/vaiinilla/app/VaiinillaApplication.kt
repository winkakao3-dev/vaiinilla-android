package com.vaiinilla.app

import android.app.Application
import android.net.http.HttpResponseCache
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vaiinilla.app.core.error.GlobalCrashReporterInstaller
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class VaiinillaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashReporting()
        installGlobalCrashReporter()
        installHttpResponseCache()
    }

    private fun installCrashReporting() {
        try {
            // Never send dev/debug crashes to the production dashboard.
            val shouldCollectCrashes = BuildConfig.IS_PRODUCTION && !BuildConfig.DEBUG
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(shouldCollectCrashes)
        } catch (e: IllegalStateException) {
            // FirebaseApp is not initialized yet (e.g. Robolectric unit tests). No-op in production.
            Log.w(TAG, "Crashlytics no disponible en este entorno", e)
        }
    }

    private fun installGlobalCrashReporter() {
        GlobalCrashReporterInstaller(
            reporter = { throwable -> FirebaseCrashlytics.getInstance().recordException(throwable) },
            logger = { thread, throwable -> Log.e(TAG, "Uncaught exception on ${thread.name}", throwable) },
        ).install()
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
