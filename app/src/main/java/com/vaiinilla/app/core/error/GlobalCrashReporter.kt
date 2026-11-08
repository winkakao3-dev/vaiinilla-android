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
    /** Se aplica a lo que sale de la app (reporte y handler previo); el log local sigue viendo el original. */
    private val outgoing: (Throwable) -> Throwable = { it },
) {
    fun install(previousHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logger(thread, throwable)
            val outbound = runCatching { outgoing(throwable) }.getOrDefault(throwable)
            runCatching { reporter.recordException(outbound) }
            previousHandler?.uncaughtException(thread, outbound)
        }
    }
}
