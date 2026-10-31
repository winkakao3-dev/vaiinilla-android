package com.vaiinilla.app.core.notifications

import android.app.ActivityManager
import android.content.Context
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Un aviso que llegó a este teléfono, guardado para poder explicarlo después: lo que traía, cómo llegó y en qué
 * cuenta y modo estaba la app. El servidor manda los avisos del personal por rol y negocio, no por el modo abierto
 * (EQUIPO-37), así que sin esto no hay forma de saber por qué al mesero le llegó un «por cobrar» de otro negocio.
 */
@Serializable
data class ReceivedNotice(
    val id: String,
    val receivedAtMillis: Long,
    val title: String,
    val body: String,
    val payload: Map<String, String>,
    /** Vino del servidor (push) y no de un aviso que armó la app. */
    val isRemote: Boolean,
    val arrivals: List<Arrival>,
    /** La app al llegar el aviso; `null` si llegó antes de saber la cuenta. */
    val phone: NoticePhoneContext? = null,
    /** La cuenta se anotó después, al abrir la app, y no cuando llegó el aviso. */
    val phoneSeenLater: Boolean = false,
    val lookup: NoticeLookup? = null,
) {
    @Serializable
    enum class Arrival(
        val label: String,
    ) {
        /** Con la app abierta: se mostró arriba. */
        SHOWN("Llegó con la app abierta"),

        /** Con la app cerrada o en segundo plano: quedó en la barra de notificaciones. */
        BACKGROUND("Llegó con la app cerrada o en segundo plano"),

        /** No se mostró: notificaciones apagadas o el aviso no traía lo necesario. */
        NOT_SHOWN("No se mostró en el teléfono"),

        /** Se tocó. */
        OPENED("Se tocó"),
    }
}

@Serializable
data class NoticeMembership(
    val membershipId: String,
    val role: OperationalRole,
    val establishmentId: String,
    val establishmentName: String,
)

@Serializable
data class NoticePhoneContext(
    val account: String?,
    val openMode: String,
    val memberships: List<NoticeMembership>,
)

/** Lo que se pudo leer del pedido del aviso, con el modo que lo leyó. */
@Serializable
data class NoticeOrderSummary(
    val establishmentName: String,
    val readAs: String,
    val folio: Int,
    val status: String,
    val customerName: String?,
    val total: String,
    val paymentMethod: String,
    val destination: String,
    val space: String?,
    val items: List<String>,
    val createdAt: String,
)

@Serializable
sealed interface NoticeLookup {
    val checkedAtMillis: Long

    @Serializable
    data class Found(
        val order: NoticeOrderSummary,
        override val checkedAtMillis: Long,
    ) : NoticeLookup

    @Serializable
    data class Unavailable(
        val reason: String,
        override val checkedAtMillis: Long,
    ) : NoticeLookup
}

/**
 * Historial de avisos de este teléfono: los últimos 100, en un archivo que no sale del teléfono ni entra al
 * respaldo de Google. Se borra al cerrar la sesión. Lo escriben el servicio de Firebase (también con la app
 * cerrada), el aviso de seguimiento que arma la app y la actividad al tocar un aviso.
 */
object ReceivedNoticeLog {
    private const val FILE_NAME = "avisos-recibidos.json"
    private const val LIMIT = 100

    @Serializable
    private data class Stored(
        val phone: NoticePhoneContext? = null,
        val notices: List<ReceivedNotice> = emptyList(),
    )

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    private val lock = Any()
    private val state = MutableStateFlow<List<ReceivedNotice>>(emptyList())
    private var phone: NoticePhoneContext? = null
    private var loaded = false

    fun notices(context: Context): StateFlow<List<ReceivedNotice>> {
        synchronized(lock) { ensureLoaded(context) }
        return state.asStateFlow()
    }

    fun record(
        context: Context,
        id: String,
        title: String,
        body: String,
        payload: Map<String, String>,
        isRemote: Boolean,
        arrival: ReceivedNotice.Arrival,
        receivedAtMillis: Long = System.currentTimeMillis(),
    ) = mutate(context) { notices ->
        val index = notices.indexOfFirst { it.id == id }
        if (index >= 0) {
            val existing = notices[index]
            if (arrival in existing.arrivals) return@mutate notices
            val updated = existing.copy(arrivals = existing.arrivals + arrival)
            return@mutate notices.toMutableList().also { it[index] = updated }
        }
        val notice =
            ReceivedNotice(
                id = id,
                receivedAtMillis = receivedAtMillis,
                title = title,
                body = body,
                payload = cleanPayload(payload),
                isRemote = isRemote,
                arrivals = listOf(arrival),
                phone = phone,
            )
        (listOf(notice) + notices).sortedByDescending { it.receivedAtMillis }.take(LIMIT)
    }

    /** Se tocó el aviso; si no estaba anotado (llegó con otra versión de la app), no se inventa. */
    fun markOpened(
        context: Context,
        id: String,
    ) = mutate(context) { notices ->
        notices.map { notice ->
            if (notice.id == id && ReceivedNotice.Arrival.OPENED !in notice.arrivals) {
                notice.copy(arrivals = notice.arrivals + ReceivedNotice.Arrival.OPENED)
            } else {
                notice
            }
        }
    }

    /** La cuenta, el modo abierto y los modos de la cuenta, tal como los ve la app ahora. */
    fun updatePhone(
        context: Context,
        current: NoticePhoneContext,
    ) {
        synchronized(lock) {
            ensureLoaded(context)
            if (phone == current) return
            phone = current
            val filled =
                state.value.map { notice ->
                    if (notice.phone == null) notice.copy(phone = current, phoneSeenLater = true) else notice
                }
            state.value = filled
            persist(context)
        }
    }

    fun saveLookup(
        context: Context,
        id: String,
        lookup: NoticeLookup,
    ) = mutate(context) { notices -> notices.map { if (it.id == id) it.copy(lookup = lookup) else it } }

    fun clear(context: Context) {
        synchronized(lock) {
            phone = null
            state.value = emptyList()
            loaded = true
            file(context).delete()
        }
    }

    /** ¿La app está al frente? Decide si el aviso se vio arriba o quedó en la barra. */
    fun appInForeground(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    /** Todo lo que trajo el aviso menos los datos internos de Firebase, que no explican nada. */
    fun cleanPayload(payload: Map<String, String>): Map<String, String> =
        payload.filterKeys { key -> !key.startsWith("google.") && !key.startsWith("gcm.") }

    private fun mutate(
        context: Context,
        change: (List<ReceivedNotice>) -> List<ReceivedNotice>,
    ) {
        synchronized(lock) {
            ensureLoaded(context)
            val updated = change(state.value)
            if (updated === state.value) return
            state.value = updated
            persist(context)
        }
    }

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val stored =
            runCatching { json.decodeFromString<Stored>(file(context).readText()) }.getOrNull() ?: return
        phone = stored.phone
        state.value = stored.notices
    }

    private fun persist(context: Context) {
        runCatching { file(context).writeText(json.encodeToString(Stored(phone, state.value))) }
    }

    private fun file(context: Context): File = File(context.applicationContext.noBackupFilesDir, FILE_NAME)
}
