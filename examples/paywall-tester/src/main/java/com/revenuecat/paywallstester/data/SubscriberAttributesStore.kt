package com.revenuecat.paywallstester.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Remembers the subscriber attributes set from this app, per app user ID, since the SDK offers no way to read
 * them back.
 */
internal class SubscriberAttributesStore(
    private val sharedPreferences: SharedPreferences,
) {
    internal companion object {
        private const val SHARED_PREFERENCES_NAME = "com.revenuecat.paywallstester"
        private const val KEY_PREFIX = "com.revenuecat.paywallstester.subscriber_attributes."
        private val serializer = MapSerializer(String.serializer(), String.serializer())
    }

    constructor(context: Context) : this(context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE))

    fun attributes(appUserID: String): Map<String, String> =
        sharedPreferences.getString(KEY_PREFIX + appUserID, null)
            ?.let { Json.decodeFromString(serializer, it) }
            ?: emptyMap()

    fun set(appUserID: String, key: String, value: String): Map<String, String> =
        save(appUserID, attributes(appUserID) + (key to value))

    fun remove(appUserID: String, key: String): Map<String, String> =
        save(appUserID, attributes(appUserID) - key)

    fun clear(appUserID: String): Map<String, String> = save(appUserID, emptyMap())

    private fun save(appUserID: String, attributes: Map<String, String>): Map<String, String> {
        sharedPreferences.edit {
            if (attributes.isEmpty()) {
                remove(KEY_PREFIX + appUserID)
            } else {
                putString(KEY_PREFIX + appUserID, Json.encodeToString(serializer, attributes))
            }
        }
        return attributes
    }
}
