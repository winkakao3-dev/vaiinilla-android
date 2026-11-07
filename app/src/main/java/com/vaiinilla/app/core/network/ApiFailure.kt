package com.vaiinilla.app.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal const val DEFAULT_ERROR_MESSAGE = "No se pudo completar la operación. Intenta de nuevo."

/** Mensajes del servidor que nombran un detalle técnico (versión, token): se dicen por su código (STAFF-21). */
private val CODED_MESSAGES =
    mapOf(
        "ORDER_VERSION_CONFLICT" to "El pedido cambió mientras lo tenías abierto. Revísalo e inténtalo de nuevo.",
        "INVALID_PICKUP_TOKEN" to "Ese código de recogida no es válido. Pide al cliente que lo muestre de nuevo.",
        "UNAUTHENTICATED" to "Tu sesión caducó. Vuelve a iniciar sesión.",
        "ACCOUNT_REQUIRED" to "Para apartar necesitas una cuenta. Crea una o inicia sesión.",
        "INSUFFICIENT_WALLET_BALANCE" to "No te alcanza el saldo. Recarga tu cartera o paga en caja.",
        // El dueño apagó «Pago en caja» con la hoja de pago abierta.
        "CASH_AT_REGISTER_NOT_ALLOWED" to
            "Este negocio ya no recibe pagos en caja desde la app. Paga con tarjeta o saldo.",
        // R11: el boliche pide cuántos juegan para preparar la pista y los zapatos.
        "PLAYERS_REQUIRED" to "Indica cuántos van a jugar para rentar la pista.",
        // PLAYERS_OVER_CAPACITY no va aquí: el mensaje del servidor dice cuántos caben en esa pista.
    )

/**
 * Qué pasó con una llamada de red, sin importar cuál fue la excepción original. [text] ya es el texto
 * que se le puede mostrar a la persona; null significa que no hay uno propio y se usa el de respaldo.
 */
sealed interface ApiFailure {
    val text: String?

    fun message(fallback: String = DEFAULT_ERROR_MESSAGE): String = text ?: fallback

    data object NoConnection : ApiFailure {
        override val text = "No pudimos conectar con Vaiinilla. Revisa tu conexión e inténtalo de nuevo."
    }

    data object Timeout : ApiFailure {
        override val text = "La conexión tardó demasiado. Inténtalo de nuevo."
    }

    data object ServerUnreachable : ApiFailure {
        override val text = "No pudimos contactar al servidor. Inténtalo de nuevo en unos segundos."
    }

    /** El servidor respondió 5xx. */
    data class ServerError(
        val httpStatus: Int,
        val code: String,
        override val text: String,
    ) : ApiFailure

    /** El servidor respondió y rechazó la operación (4xx, incluido el 401 de sesión caducada). */
    data class Rejected(
        val httpStatus: Int,
        val code: String,
        val retryAfterSeconds: Long?,
        val details: JsonElement?,
        override val text: String?,
    ) : ApiFailure {
        val sessionExpired: Boolean get() = httpStatus == 401
    }

    /** La respuesta llegó pero no cumple el contrato (JSON malformado o con otra forma). */
    data object BadResponse : ApiFailure {
        override val text =
            "Recibimos una respuesta que esta versión no entiende. Actualiza la app e inténtalo de nuevo."
    }

    /** No hay sesión con un establecimiento activo, así que la llamada ni salió. */
    data class NotSignedIn(
        override val text: String?,
    ) : ApiFailure

    data class Unknown(
        override val text: String?,
    ) : ApiFailure
}

/** La red se cortó o no llegó al servidor: lo único que de verdad es «sin internet». */
val ApiFailure.isConnectionLoss: Boolean
    get() = this is ApiFailure.NoConnection || this is ApiFailure.Timeout || this is ApiFailure.ServerUnreachable

/** Resultado tipado de una llamada: nunca lanza y nunca deja pasar una excepción cruda a la pantalla. */
sealed interface ApiResult<out T> {
    data class Success<T>(
        val value: T,
    ) : ApiResult<T>

    data class Failure(
        val failure: ApiFailure,
    ) : ApiResult<Nothing>
}

/** El único lugar que decide qué clase de fallo es una excepción de red. */
object ApiErrorMapper {
    fun map(error: Throwable?): ApiFailure {
        if (error == null) return ApiFailure.Unknown(null)

        val causes = generateSequence(error) { it.cause }.toList()
        val combinedMessage = causes.joinToString(" ") { it.message.orEmpty() }.lowercase()
        val apiErrors = causes.filterIsInstance<ApiClientException>()
        val codedMessage = apiErrors.firstNotNullOfOrNull { CODED_MESSAGES[it.code] }
        val ownMessage = error.message?.trim().takeUnless { it.isNullOrEmpty() }

        return when {
            causes.any { it is UnknownHostException } ||
                "unable to resolve host" in combinedMessage ||
                "localhost.invalid" in combinedMessage -> ApiFailure.NoConnection

            causes.any { it is SocketTimeoutException } || "timeout" in combinedMessage -> ApiFailure.Timeout

            causes.any { it is ConnectException } ||
                "failed to connect" in combinedMessage ||
                "connection refused" in combinedMessage -> ApiFailure.ServerUnreachable

            codedMessage != null -> fromApi(apiErrors.first { CODED_MESSAGES[it.code] != null }, codedMessage)

            apiErrors.any { it.httpStatus in 500..599 } ->
                apiErrors.first { it.httpStatus in 500..599 }.let {
                    ApiFailure.ServerError(
                        it.httpStatus,
                        it.code,
                        "Tuvimos un problema en el servidor. Intenta de nuevo en unos momentos.",
                    )
                }

            // Un cuerpo JSON que no cumple el contrato revienta con offsets y tokens
            // internos ("Unexpected JSON token at offset …"): nunca llegan al usuario.
            causes.any { it is SerializationException } -> ApiFailure.BadResponse

            apiErrors.isNotEmpty() -> fromApi(apiErrors.first(), ownMessage)

            causes.any { it is MissingAccessTokenException } -> ApiFailure.NotSignedIn(ownMessage)

            else -> ApiFailure.Unknown(ownMessage)
        }
    }

    private fun fromApi(
        error: ApiClientException,
        text: String?,
    ): ApiFailure =
        if (error.httpStatus in 500..599) {
            ApiFailure.ServerError(error.httpStatus, error.code, text ?: "")
        } else {
            ApiFailure.Rejected(error.httpStatus, error.code, error.retryAfterSeconds, error.details, text)
        }
}

fun <T> Result<T>.toApiResult(): ApiResult<T> =
    fold(
        onSuccess = { ApiResult.Success(it) },
        onFailure = { ApiResult.Failure(ApiErrorMapper.map(it)) },
    )

/**
 * Traduce el error del servidor al error propio de cada repositorio y deja pasar cualquier otro tal cual.
 * Es lo único que cada `Remote*Repository` hace distinto con un fallo.
 */
internal inline fun <T> Result<T>.mapApiFailure(transform: (ApiClientException) -> Throwable): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { error -> Result.failure(if (error is ApiClientException) transform(error) else error) },
    )
