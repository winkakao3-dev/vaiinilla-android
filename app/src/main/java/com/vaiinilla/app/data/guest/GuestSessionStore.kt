package com.vaiinilla.app.data.guest

import android.content.Context
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guest venue + cart persistence. Preference keys are UX/local only — never authorization.
 */
@Singleton
class GuestSessionStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        fun readVenue(): GuestVenueContext? {
            val establishmentId = prefs.getString(KEY_ESTABLISHMENT_ID, null) ?: return null
            val slug = prefs.getString(KEY_SLUG, null) ?: return null
            val name = prefs.getString(KEY_NAME, null) ?: return null
            val label = prefs.getString(KEY_ID_LABEL, "Identificador") ?: "Identificador"
            val required = prefs.getBoolean(KEY_ID_REQUIRED, false)
            val spaceId = prefs.getInt(KEY_SPACE_ID, -1).takeIf { it >= 0 }
            val space =
                spaceId?.let {
                    PublicSpace(
                        id = it,
                        name = prefs.getString(KEY_SPACE_NAME, "") ?: "",
                        type = prefs.getString(KEY_SPACE_TYPE, "mesa") ?: "mesa",
                    )
                }
            return GuestVenueContext(
                establishment =
                    PublicEstablishment(
                        id = establishmentId,
                        name = name,
                        slug = slug,
                        clientIdLabel = label,
                        clientIdRequired = required,
                    ),
                space = space,
            )
        }

        fun refreshSelectedVenueMetadata(establishment: PublicEstablishment): GuestVenueContext? {
            val current = readVenue() ?: return null
            if (current.establishment.id != establishment.id) return null
            val refreshed = current.copy(establishment = establishment)
            if (refreshed != current) saveVenue(refreshed)
            return refreshed
        }

        fun saveVenue(context: GuestVenueContext) {
            prefs
                .edit()
                .putString(KEY_ESTABLISHMENT_ID, context.establishment.id)
                .putString(KEY_SLUG, context.establishment.slug)
                .putString(KEY_NAME, context.establishment.name)
                .putString(KEY_ID_LABEL, context.establishment.clientIdLabel)
                .putBoolean(KEY_ID_REQUIRED, context.establishment.clientIdRequired)
                .apply {
                    val space = context.space
                    if (space == null) {
                        remove(KEY_SPACE_ID)
                        remove(KEY_SPACE_NAME)
                        remove(KEY_SPACE_TYPE)
                    } else {
                        putInt(KEY_SPACE_ID, space.id)
                        putString(KEY_SPACE_NAME, space.name)
                        putString(KEY_SPACE_TYPE, space.type)
                    }
                }.apply()
        }

        fun clearVenue() {
            prefs
                .edit()
                .remove(KEY_ESTABLISHMENT_ID)
                .remove(KEY_SLUG)
                .remove(KEY_NAME)
                .remove(KEY_ID_LABEL)
                .remove(KEY_ID_REQUIRED)
                .remove(KEY_SPACE_ID)
                .remove(KEY_SPACE_NAME)
                .remove(KEY_SPACE_TYPE)
                .apply()
        }

        fun clearAll() {
            prefs.edit().clear().apply()
        }

        fun readPendingCreateIdempotency(fingerprint: String): String? {
            val storedFingerprint = prefs.getString(KEY_CREATE_IDEMPOTENCY_FINGERPRINT, null)
            if (storedFingerprint != fingerprint) return null
            return prefs.getString(KEY_CREATE_IDEMPOTENCY_KEY, null)
        }

        fun savePendingCreateIdempotency(
            fingerprint: String,
            idempotencyKey: String,
        ) {
            prefs
                .edit()
                .putString(KEY_CREATE_IDEMPOTENCY_FINGERPRINT, fingerprint)
                .putString(KEY_CREATE_IDEMPOTENCY_KEY, idempotencyKey)
                .apply()
        }

        fun clearPendingCreateIdempotency() {
            prefs
                .edit()
                .remove(KEY_CREATE_IDEMPOTENCY_FINGERPRINT)
                .remove(KEY_CREATE_IDEMPOTENCY_KEY)
                .apply()
        }

        fun readPendingStripeRetryIdempotency(orderId: String): String? = prefs.getString("stripe_retry:$orderId", null)

        fun savePendingStripeRetryIdempotency(
            orderId: String,
            idempotencyKey: String,
        ) {
            prefs.edit().putString("stripe_retry:$orderId", idempotencyKey).apply()
        }

        fun clearPendingStripeRetryIdempotency(orderId: String) {
            prefs.edit().remove("stripe_retry:$orderId").apply()
        }

        fun readPendingStripeConfirmationOrderId(): String? =
            prefs.getString(KEY_PENDING_STRIPE_CONFIRMATION_ORDER_ID, null)

        fun savePendingStripeConfirmationOrderId(orderId: String) {
            prefs.edit().putString(KEY_PENDING_STRIPE_CONFIRMATION_ORDER_ID, orderId).apply()
        }

        fun clearPendingStripeConfirmationOrderId(orderId: String) {
            if (readPendingStripeConfirmationOrderId() == orderId) {
                prefs.edit().remove(KEY_PENDING_STRIPE_CONFIRMATION_ORDER_ID).apply()
            }
        }

        fun cartStorageKey(
            establishmentId: String,
            spaceId: Int?,
        ): String = "$establishmentId:${spaceId ?: "none"}"

        fun readCartSnapshot(storageKey: String): List<GuestCartLineSnapshot> {
            val raw = prefs.getString(cartPrefKey(storageKey), null) ?: return emptyList()
            return runCatching { json.decodeFromString<List<GuestCartLineSnapshot>>(raw) }
                .getOrDefault(emptyList())
        }

        fun saveCartSnapshot(
            storageKey: String,
            lines: List<CartLine>,
        ) {
            val snapshot =
                lines.map { line ->
                    GuestCartLineSnapshot(
                        productId = line.product.id,
                        quantity = line.quantity,
                        selectedOptionIds = line.selectedOptionIds.sorted(),
                    )
                }
            prefs
                .edit()
                .putString(cartPrefKey(storageKey), json.encodeToString(snapshot))
                .apply()
        }

        fun clearCart(storageKey: String) {
            prefs.edit().remove(cartPrefKey(storageKey)).apply()
        }

        fun restoreCartLines(
            snapshot: List<GuestCartLineSnapshot>,
            products: List<Product>,
        ): List<CartLine> {
            val byId = products.associateBy { it.id }
            return snapshot.mapNotNull { item ->
                val product = byId[item.productId] ?: return@mapNotNull null
                CartLine(
                    product = product,
                    quantity = item.quantity.coerceIn(1, 20),
                    selectedOptionIds = item.selectedOptionIds.toSet(),
                )
            }
        }

        private fun cartPrefKey(storageKey: String) = "cart:$storageKey"

        private companion object {
            const val PREFS = "vaiinilla_guest_session"
            const val KEY_ESTABLISHMENT_ID = "establishment_id"
            const val KEY_SLUG = "slug"
            const val KEY_NAME = "name"
            const val KEY_ID_LABEL = "id_label"
            const val KEY_ID_REQUIRED = "id_required"
            const val KEY_SPACE_ID = "space_id"
            const val KEY_SPACE_NAME = "space_name"
            const val KEY_SPACE_TYPE = "space_type"
            const val KEY_CREATE_IDEMPOTENCY_FINGERPRINT = "create_idempotency_fingerprint"
            const val KEY_CREATE_IDEMPOTENCY_KEY = "create_idempotency_key"
            const val KEY_PENDING_STRIPE_CONFIRMATION_ORDER_ID = "pending_stripe_confirmation_order_id"
        }
    }

@Serializable
data class GuestCartLineSnapshot(
    val productId: Int,
    val quantity: Int,
    val selectedOptionIds: List<Int>,
)
