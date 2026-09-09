package com.vaiinilla.app.core.error

/**
 * Minimal seam so uncaught-exception reporting can be unit-tested without a real
 * Crashlytics instance (which requires a live Firebase app context).
 */
fun interface CrashReporter {
    fun recordException(throwable: Throwable)
}

/**
 * Installs a process-wide [Thread.UncaughtExceptionHandler] that reports the crash
 * before delegating to whatever handler was previously registered (normally the
 * platform default, which terminates the process). This never swallows or recovers
 * from the crash: Android must still restart the process cleanly, especially given
 * fail-closed invariants like the Stripe pickup QR gating.
 */
class GlobalCrashReporterInstaller(
    private val reporter: CrashReporter,
    private val logger: (Thread, Throwable) -> Unit = { _, _ -> },
) {
    fun install(previousHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logger(thread, throwable)
            runCatching { reporter.recordException(throwable) }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
