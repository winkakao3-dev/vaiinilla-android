package com.vaiinilla.app.core.error

/**
 * Interruptor del reporte de cierres (Crashlytics). Se fija al compilar con
 * `-Pvaiinilla.crashReporting=on|off|auto` o `VAIINILLA_CRASH_REPORTING`.
 */
enum class CrashReportingMode {
    /** Solo la app de producción compilada en release. Es el comportamiento de siempre. */
    AUTO,
    ON,
    OFF,
    ;

    companion object {
        fun parse(raw: String?): CrashReportingMode =
            when (raw?.trim()?.lowercase()) {
                "on", "true" -> ON
                "off", "false" -> OFF
                else -> AUTO
            }
    }
}

object CrashReportingPolicy {
    fun shouldCollect(
        mode: CrashReportingMode,
        isProduction: Boolean,
        isDebug: Boolean,
    ): Boolean =
        when (mode) {
            CrashReportingMode.OFF -> false
            CrashReportingMode.ON -> true
            CrashReportingMode.AUTO -> isProduction && !isDebug
        }
}
