package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.domain.admin.AssistantJob
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.domain.admin.AssistantReply
import com.vaiinilla.app.domain.admin.AssistantSheet
import com.vaiinilla.app.domain.repository.AdminStoreRepository
import com.vaiinilla.app.ui.operational.FakeCatalog
import com.vaiinilla.app.ui.operational.StaffHarness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.SocketException
import java.util.concurrent.ConcurrentLinkedQueue

/** El asistente de la tienda arma la respuesta en el servidor y la app pregunta por ella (ASISTENTE-27). */
class StoreAssistantJobsTest {
    private val store = AssistantJobsStore()
    private lateinit var vm: AdminStoreViewModel

    private val chat get() = vm.assistant.value

    @Before
    fun setUp() {
        StaffHarness.installMain()
        vm = AdminStoreViewModel(store, FakeCatalog())
    }

    @Test
    fun salirDeLaAppNoPierdeLaRespuesta() {
        store.polls.add(Result.failure(SocketException("Software caused connection abort")))
        store.polls.add(Result.success(AssistantJob.Done(reply("Voy a crear 40 productos."))))
        vm.sendToAssistant("Aquí está mi menú")
        StaffHarness.awaitUntil("la respuesta del trabajo") { !chat.thinking }
        assertNull(chat.error)
        assertEquals("Voy a crear 40 productos.", chat.messages.last().text)
        assertFalse(chat.thinkingOnServer)
        assertEquals(0, store.directCalls)
    }

    @Test
    fun elErrorDelTrabajoSeExplicaComoSiempre() {
        store.polls.add(Result.success(AssistantJob.Failed("ASSISTANT_REPLY_TOO_LONG", "Es mucho")))
        vm.sendToAssistant("Aquí está mi menú")
        StaffHarness.awaitUntil("el error del trabajo") { !chat.thinking }
        assertEquals(
            "Es mucho para una sola vez. Mándame una parte del menú, acéptala y seguimos con lo demás.",
            chat.error,
        )
    }

    @Test
    fun unServidorSinLaRutaDeTrabajosContestaComoAntes() {
        store.jobsAvailable = false
        vm.sendToAssistant("Hola")
        StaffHarness.awaitUntil("la respuesta directa") { !chat.thinking }
        assertEquals("Respuesta directa", chat.messages.last().text)
        assertEquals(1, store.directCalls)
    }

    @Test
    fun elCorteDeRedSeDiceEnEspanol() {
        assertEquals(
            "Se cortó la conexión con el asistente. Revisa tu internet y toca Reintentar.",
            assistantErrorMessage(SocketException("Software caused connection abort")),
        )
    }

    private fun reply(text: String) = AssistantReply(text, emptyList(), null, 40)

    private inner class AssistantJobsStore(
        base: FakeStore = FakeStore(),
    ) : AdminStoreRepository by base {
        val polls = ConcurrentLinkedQueue<Result<AssistantJob>>()

        @Volatile var jobsAvailable = true

        @Volatile var directCalls = 0

        override fun assistantReply(
            messages: List<AssistantMessage>,
            photoIds: List<String>,
            sheets: List<AssistantSheet>,
        ): Result<AssistantReply> {
            directCalls += 1
            return Result.success(reply("Respuesta directa"))
        }

        override fun startAssistantJob(
            messages: List<AssistantMessage>,
            photoIds: List<String>,
            sheets: List<AssistantSheet>,
        ): Result<String> =
            if (jobsAvailable) Result.success("trabajo-1") else Result.failure(UnsupportedOperationException())

        override fun assistantJob(id: String): Result<AssistantJob> = polls.poll() ?: Result.success(AssistantJob.Thinking)
    }
}