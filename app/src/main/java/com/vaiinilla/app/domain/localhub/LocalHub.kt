package com.vaiinilla.app.domain.localhub

import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Comanda que viaja por el Wi‑Fi del local mientras no hay internet (plan «Vaiinilla sin internet», fase 2). La app
 * de Caja es el concentrador: Mesero le manda la comanda y Cocina la lee de ahí. El pedido de verdad lo manda la cola de
 * cada teléfono al volver la red; la comanda solo sirve para que Cocina prepare mientras tanto.
 */
data class LocalTicket(
    /** El mismo id que el pedido guardado sin conexión: al volver la red se reconocen. */
    val id: String,
    val localFolio: String,
    val placeLabel: String,
    val items: List<LocalTicketItem>,
    val capturedAt: Instant,
    /** «Mesero» o «Caja»: quién la tomó. */
    val takenBy: String,
    val state: LocalTicketState = LocalTicketState.PENDING,
)

data class LocalTicketItem(
    val name: String,
    val quantity: Int,
    val notes: String = "",
    /** `cocina` o `caja` (barra), como la estación del producto. */
    val station: String,
)

enum class LocalTicketState(
    val wireValue: String,
) {
    PENDING("pendiente"),
    READY("listo"),
    DELIVERED("entregado"),
    ;

    companion object {
        fun fromWire(value: String): LocalTicketState = entries.firstOrNull { it.wireValue == value } ?: PENDING
    }
}

/** Lo que el concentrador guarda de las comandas del turno. */
data class LocalTicketBoardSnapshot(
    val tickets: List<LocalTicket> = emptyList(),
) {
    /** La misma comanda que llega dos veces (reintento del mesero) no se duplica. */
    fun add(ticket: LocalTicket): LocalTicketBoardSnapshot =
        if (tickets.any { it.id == ticket.id }) this else copy(tickets = tickets + ticket)

    fun mark(
        id: String,
        state: LocalTicketState,
    ): LocalTicketBoardSnapshot = copy(tickets = tickets.map { if (it.id == id) it.copy(state = state) else it })

    /** Las de otro turno ya no sirven: el pedido de verdad ya se mandó o se registró a mano. */
    fun withoutOlderThan(
        now: Instant,
        maximumAge: Duration = LocalHubRules.TICKET_MAXIMUM_AGE,
    ): LocalTicketBoardSnapshot = copy(tickets = tickets.filter { Duration.between(it.capturedAt, now) <= maximumAge })
}

object LocalHubRules {
    /** DNS-SD: así se anuncia la app de Caja en la red del local. */
    const val SERVICE_TYPE = "_vaiinilla._tcp."

    /** Atributo del anuncio con el negocio: Cocina solo se conecta a la Caja de su negocio. */
    const val ESTABLISHMENT_ATTRIBUTE = "est"

    const val PORT = 47_821

    /** Cocina pregunta cada 3 s mientras no hay internet. */
    const val POLL_INTERVAL_MS = 3_000L

    val TICKET_MAXIMUM_AGE: Duration = Duration.ofHours(12)

    /** Los relojes del local pueden no coincidir; una firma más vieja que esto se rechaza (no se reusa). */
    val SIGNATURE_WINDOW: Duration = Duration.ofMinutes(10)

    /** Una petición más grande que esto no es una comanda: se corta. */
    const val MAXIMUM_MESSAGE_BYTES = 256 * 1024
}

/**
 * Firma de las peticiones por el Wi‑Fi del local: HMAC-SHA256 con la llave del negocio del día (`GET /llave-local`),
 * sobre la operación, la hora y el contenido. Así nadie más en esa red (clientes, otra tienda) puede meter comandas.
 */
object LocalHubSigning {
    fun sign(
        key: String,
        timestampMs: Long,
        operation: String,
        payload: String,
    ): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(Base64.getUrlDecoder().decode(key), ALGORITHM))
        val message = "$operation\n$timestampMs\n${sha256(payload)}"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    fun verify(
        keys: List<String>,
        timestampMs: Long,
        operation: String,
        payload: String,
        signature: String,
        now: Instant,
    ): Boolean {
        val age = Duration.between(Instant.ofEpochMilli(timestampMs), now).abs()
        if (age > LocalHubRules.SIGNATURE_WINDOW) return false
        val given = signature.toByteArray(Charsets.UTF_8)
        return keys.any { key ->
            val expected = runCatching { sign(key, timestampMs, operation, payload) }.getOrNull() ?: return@any false
            MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), given)
        }
    }

    private fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private const val ALGORITHM = "HmacSHA256"
}

/** Operaciones que entiende el concentrador. */
object LocalHubOperations {
    const val ADD_TICKET = "comanda.agregar"
    const val LIST_TICKETS = "comandas.listar"
    const val MARK_TICKET = "comanda.estado"
}

/** Las llaves del negocio que el teléfono bajó con internet; sin ellas no se habla con el concentrador. */
interface LocalKeyStore {
    fun keys(establishmentId: String): List<String>

    fun save(
        establishmentId: String,
        keys: List<String>,
    )
}
