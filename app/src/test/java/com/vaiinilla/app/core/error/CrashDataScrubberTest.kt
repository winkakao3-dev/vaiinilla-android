package com.vaiinilla.app.core.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashDataScrubberTest {
    @Test
    fun `quita correos telefonos y tokens del mensaje`() {
        val limpio =
            CrashDataScrubber.scrub(
                "falló para ana.perez@correo.mx con +52 55 1234 5678, Bearer abcdef1234567890 y eyJhbGciOi.eyJzdWIiOiIx.firma123",
            )!!
        assertFalse(limpio.contains("ana.perez"))
        assertFalse(limpio.contains("1234 5678"))
        assertFalse(limpio.contains("abcdef1234567890"))
        assertFalse(limpio.contains("eyJhbGciOi"))
        assertTrue(limpio.contains("[correo]"))
    }

    @Test
    fun `deja intacto un mensaje sin datos personales`() {
        assertEquals("Pedido en cola 3 de 5", CrashDataScrubber.scrub("Pedido en cola 3 de 5"))
        assertNull(CrashDataScrubber.scrub(null as String?))
    }

    @Test
    fun `la copia conserva la pila el tipo original y limpia tambien la causa`() {
        val original = IllegalStateException("x@y.com", RuntimeException("token=supersecreto123"))
        val copia = CrashDataScrubber.scrub(original)

        assertTrue(copia.message!!.startsWith("java.lang.IllegalStateException"))
        assertFalse(copia.message!!.contains("x@y.com"))
        assertFalse(copia.cause!!.message!!.contains("supersecreto123"))
        assertTrue(original.stackTrace.contentEquals(copia.stackTrace))
    }

    @Test
    fun `una causa circular no cuelga el limpiador`() {
        val a = RuntimeException("a")
        val b = RuntimeException("b", a)
        a.initCause(b)
        CrashDataScrubber.scrub(a)
    }

    @Test
    fun `el instalador entrega lo limpio al reporte y al handler previo pero loguea el original`() {
        val logged = mutableListOf<Throwable>()
        val reported = mutableListOf<Throwable>()
        val delegated = mutableListOf<Throwable>()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        try {
            GlobalCrashReporterInstaller(
                reporter = { reported += it },
                logger = { _, t -> logged += t },
                outgoing = CrashDataScrubber::scrub,
            ).install(Thread.UncaughtExceptionHandler { _, t -> delegated += t })

            val boom = RuntimeException("dueño@negocio.com")
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), boom)

            assertSame(boom, logged.single())
            assertFalse(reported.single().message!!.contains("dueño@negocio.com"))
            assertSame(reported.single(), delegated.single())
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
    }
}
