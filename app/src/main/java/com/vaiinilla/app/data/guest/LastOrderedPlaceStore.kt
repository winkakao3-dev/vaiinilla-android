package com.vaiinilla.app.data.guest

import android.content.Context
import androidx.core.content.edit
import com.vaiinilla.app.domain.model.LastOrderedPlace
import com.vaiinilla.app.domain.model.PublicEstablishment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Guarda `LastOrderedPlace` en el teléfono. Se borra al cerrar sesión. */
@Singleton
class LastOrderedPlaceStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(): LastOrderedPlace? {
            val raw = prefs.getString(KEY, null) ?: return null
            return runCatching { json.decodeFromString<StoredPlace>(raw).toDomain() }.getOrNull()
        }

        fun record(place: LastOrderedPlace) {
            prefs.edit { putString(KEY, json.encodeToString(StoredPlace.from(place))) }
        }

        fun clear() {
            prefs.edit { remove(KEY) }
        }

        @Serializable
        private data class StoredPlace(
            val id: String,
            val name: String,
            val slug: String,
            val clientIdLabel: String,
            val clientIdRequired: Boolean,
            val photoUrl: String? = null,
            val logoUrl: String? = null,
            val whatsappUrl: String? = null,
            val firstItemName: String,
            val otherItemsCount: Int,
            val orderedAtEpochMs: Long,
        ) {
            fun toDomain() =
                LastOrderedPlace(
                    establishment =
                        PublicEstablishment(
                            id = id,
                            name = name,
                            slug = slug,
                            clientIdLabel = clientIdLabel,
                            clientIdRequired = clientIdRequired,
                            photoUrl = photoUrl,
                            logoUrl = logoUrl,
                            whatsappUrl = whatsappUrl,
                        ),
                    firstItemName = firstItemName,
                    otherItemsCount = otherItemsCount,
                    orderedAt = Instant.ofEpochMilli(orderedAtEpochMs),
                )

            companion object {
                fun from(place: LastOrderedPlace) =
                    StoredPlace(
                        id = place.establishment.id,
                        name = place.establishment.name,
                        slug = place.establishment.slug,
                        clientIdLabel = place.establishment.clientIdLabel,
                        clientIdRequired = place.establishment.clientIdRequired,
                        photoUrl = place.establishment.photoUrl,
                        logoUrl = place.establishment.logoUrl,
                        whatsappUrl = place.establishment.whatsappUrl,
                        firstItemName = place.firstItemName,
                        otherItemsCount = place.otherItemsCount,
                        orderedAtEpochMs = place.orderedAt.toEpochMilli(),
                    )
            }
        }

        private companion object {
            const val PREFS = "last_ordered_place"
            const val KEY = "v1"
        }
    }
