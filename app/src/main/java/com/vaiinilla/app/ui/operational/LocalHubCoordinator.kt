package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.localhub.LocalHubClient
import com.vaiinilla.app.data.localhub.LocalHubDiscovery
import com.vaiinilla.app.data.localhub.LocalHubEndpoint
import com.vaiinilla.app.data.localhub.LocalHubHandler
import com.vaiinilla.app.data.localhub.LocalHubJson
import com.vaiinilla.app.data.localhub.LocalHubServer
import com.vaiinilla.app.data.localhub.PreferencesLocalBoardStore
import com.vaiinilla.app.domain.localhub.LocalHubOperations
import com.vaiinilla.app.domain.localhub.LocalHubRules
import com.vaiinilla.app.domain.localhub.LocalKeyStore
import com.vaiinilla.app.domain.localhub.LocalTicket
import com.vaiinilla.app.domain.localhub.LocalTicketState
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Qué papel tiene este teléfono en la red del local. */
enum class LocalHubRole {
    /** Caja: guarda las comandas y las reparte. */
    HUB,

    /** Mesero: solo manda comandas. */
    SENDER,

    /** Cocina: lee las comandas y las marca listas. */
    KITCHEN,
}

data class LocalHubState(
    val role: LocalHubRole? = null,
    /** Mesero y Cocina ya encontraron la Caja en el Wi‑Fi (la Caja siempre lo está consigo misma). */
    val connected: Boolean = false,
    /** El teléfono ya tiene la llave del negocio; sin ella no puede hablar con nadie del local. */
    val hasKey: Boolean = false,
    val tickets: List<LocalTicket> = emptyList(),
    /** Comandas del mesero que todavía no llegan a la Caja. */
    val outgoing: Int = 0,
)

/**
 * Fase 2 del plan «Vaiinilla sin internet»: la app de Caja es el concentrador del local. Mesero le manda por el
 * Wi‑Fi las comandas que guardó sin internet y Cocina las lee de ahí para preparar. El pedido de verdad lo sigue
 * mandando la cola de cada teléfono ([OfflineStaffOrderQueue]) al volver la red, con el mismo id que la comanda: así
 * Cocina deja de mostrar la comanda cuando el pedido real aparece en su tablero.
 *
 * La llave del negocio se baja con internet al abrir el turno y se guarda; sin ella el concentrador no arranca.
 */
class LocalHubCoordinator(
    private val apiClient: VaiinillaApiClient,
    private val keyStore: LocalKeyStore,
    private val boardStore: PreferencesLocalBoardStore,
    private val discovery: LocalHubDiscovery,
    private val client: LocalHubClient = LocalHubClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val _state = MutableStateFlow(LocalHubState())
    val state: StateFlow<LocalHubState> = _state.asStateFlow()

    private var establishmentId: String? = null
    private var role: LocalHubRole? = null
    private var server: LocalHubServer? = null
    private var handler: LocalHubHandler? = null
    private var endpoint: LocalHubEndpoint? = null
    private var loop: Job? = null
    private val outgoing = mutableListOf<LocalTicket>()

    /** Se llama con el modo abierto; otro modo o ninguno apaga lo del local. */
    @Synchronized
    fun bind(
        operationalRole: OperationalRole?,
        establishment: String?,
    ) {
        val wanted =
            when (operationalRole) {
                OperationalRole.CASHIER -> LocalHubRole.HUB
                OperationalRole.WAITER -> LocalHubRole.SENDER
                OperationalRole.KITCHEN -> LocalHubRole.KITCHEN
                else -> null
            }
        if (wanted == role && establishment == establishmentId) return
        stopLocked()
        if (wanted == null || establishment == null) return
        role = wanted
        establishmentId = establishment
        _state.value = LocalHubState(role = wanted, hasKey = keyStore.keys(establishment).isNotEmpty())
        loop = scope.launch { run(wanted, establishment) }
    }

    @Synchronized
    fun stop() = stopLocked()

    /** Mesero o Caja guardaron un pedido sin internet: la comanda va a la Caja para que Cocina la vea. */
    fun sendTicket(ticket: LocalTicket) {
        val local = synchronized(this) { handler }
        if (local != null) {
            local.update { it.add(ticket) }
            return
        }
        synchronized(this) {
            if (role == null || outgoing.any { it.id == ticket.id }) return
            outgoing += ticket
            _state.update { it.copy(outgoing = outgoing.size) }
        }
        scope.launch { flushOutgoing() }
    }

    /** Cocina marcó una comanda (Listo). */
    fun markTicket(
        id: String,
        newState: LocalTicketState,
    ) {
        _state.update { current ->
            current.copy(tickets = current.tickets.map { if (it.id == id) it.copy(state = newState) else it })
        }
        val local = synchronized(this) { handler }
        if (local != null) {
            local.update { it.mark(id, newState) }
            return
        }
        val payload = LocalHubJson.encodeMark(LocalHubJson.MarkTicket(id, newState.wireValue))
        scope.launch { call(LocalHubOperations.MARK_TICKET, payload) }
    }

    private suspend fun run(
        role: LocalHubRole,
        establishment: String,
    ) {
        var lastKeyFetch = 0L
        var lastKeyAttempt = 0L
        while (scope.isActive && isCurrent(role, establishment)) {
            val now = System.currentTimeMillis()
            // Con la llave se refresca cada hora; sin ella se reintenta cada minuto (no cada 3 s contra el servidor).
            val due = if (keyStore.keys(establishment).isEmpty()) KEY_RETRY_MS else KEY_REFRESH_MS
            if (now - lastKeyFetch >= due && now - lastKeyAttempt >= KEY_RETRY_MS) {
                lastKeyAttempt = now
                if (refreshKeys(establishment)) lastKeyFetch = now
            }
            val hasKey = keyStore.keys(establishment).isNotEmpty()
            _state.update { it.copy(hasKey = hasKey) }
            if (hasKey) {
                when (role) {
                    LocalHubRole.HUB -> ensureHub(establishment)
                    LocalHubRole.SENDER, LocalHubRole.KITCHEN -> ensureFinding(establishment)
                }
                if (role != LocalHubRole.HUB) flushOutgoing()
                if (role == LocalHubRole.KITCHEN) pollTickets()
            }
            delay(LocalHubRules.POLL_INTERVAL_MS)
        }
    }

    private fun refreshKeys(establishment: String): Boolean =
        apiClient
            .get(KEYS_PATH)
            .mapCatching { LocalHubJson.decodeKeys(it) }
            .onSuccess { keys -> if (keys.isNotEmpty()) keyStore.save(establishment, keys) }
            .isSuccess

    @Synchronized
    private fun ensureHub(establishment: String) {
        if (server != null) return
        val hubHandler =
            LocalHubHandler(
                keys = { keyStore.keys(establishment) },
                loadBoard = { boardStore.load(establishment) },
                saveBoard = { boardStore.save(establishment, it) },
                onChange = { board -> _state.update { it.copy(tickets = board.tickets) } },
            )
        val hubServer = LocalHubServer(hubHandler)
        val started = runCatching { hubServer.start() }.isSuccess
        if (!started) return
        handler = hubHandler
        server = hubServer
        discovery.advertise(establishment, hubServer.boundPort)
        _state.update { it.copy(connected = true, tickets = hubHandler.update { board -> board }.tickets) }
    }

    @Synchronized
    private fun ensureFinding(establishment: String) {
        discovery.find(establishment) { found ->
            synchronized(this) { endpoint = found }
            _state.update { it.copy(connected = true) }
        }
    }

    private fun flushOutgoing() {
        val pending = synchronized(this) { outgoing.toList() }
        pending.forEach { ticket ->
            val sent = call(LocalHubOperations.ADD_TICKET, LocalHubJson.encodeTicket(ticket))?.ok == true
            if (sent) {
                synchronized(this) {
                    outgoing.removeAll { it.id == ticket.id }
                    _state.update { it.copy(outgoing = outgoing.size) }
                }
            }
        }
    }

    private fun pollTickets() {
        val response = call(LocalHubOperations.LIST_TICKETS, "") ?: return
        if (!response.ok) return
        val tickets = runCatching { LocalHubJson.decodeTickets(response.payload) }.getOrNull() ?: return
        _state.update { it.copy(tickets = tickets) }
    }

    /**
     * Firma con la llave de hoy o la de mañana: la Caja y el mesero pudieron bajarlas en días distintos, así que si
     * la Caja no reconoce una se prueba la otra. Sin respuesta, el teléfono deja de estar conectado hasta la siguiente.
     */
    private fun call(
        operation: String,
        payload: String,
    ): LocalHubJson.Response? {
        val target = synchronized(this) { endpoint } ?: return null
        val establishment = synchronized(this) { establishmentId } ?: return null
        var last: LocalHubJson.Response? = null
        for (key in keyStore.keys(establishment)) {
            val result = client.call(target.host, target.port, key, operation, payload)
            val response =
                result.getOrElse {
                    _state.update { it.copy(connected = false) }
                    return null
                }
            _state.update { it.copy(connected = true) }
            if (response.error != LocalHubHandler.ERROR_SIGNATURE) return response
            last = response
        }
        return last
    }

    @Synchronized
    private fun isCurrent(
        role: LocalHubRole,
        establishment: String,
    ): Boolean = this.role == role && establishmentId == establishment

    private fun stopLocked() {
        loop?.cancel()
        loop = null
        server?.stop()
        server = null
        handler = null
        endpoint = null
        discovery.stopAdvertising()
        discovery.stopFinding()
        outgoing.clear()
        role = null
        establishmentId = null
        _state.value = LocalHubState()
    }

    private companion object {
        const val KEYS_PATH = "llave-local"

        /** Con internet se baja la llave al abrir y cada hora: así siempre trae la de hoy y la de mañana. */
        const val KEY_REFRESH_MS = 60 * 60 * 1000L

        const val KEY_RETRY_MS = 60 * 1000L
    }
}
