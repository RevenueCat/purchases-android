package com.revenuecat.purchases.common.caching

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.verboseLog
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class LocalTransactionMetadataStore(
    context: Context,
    private val apiKey: String,
    private val sharedPreferences: SharedPreferences = initializeSharedPreferences(context, apiKey),
    private val json: Json = JsonTools.json,
) {
    private companion object {
        const val KEY_PREFIX = "local_transaction_metadata_"

        fun initializeSharedPreferences(context: Context, apiKey: String): SharedPreferences =
            context.getSharedPreferences(
                "com.revenuecat.purchases.transaction_metadata.$apiKey",
                Context.MODE_PRIVATE,
            )
    }

    @Synchronized
    fun cacheLocalTransactionMetadata(
        purchaseToken: String,
        data: LocalTransactionMetadata,
    ) {
        val tokenHash = getTokenHash(purchaseToken)

        if (hasCachedData(tokenHash)) {
            debugLog { "Purchase data already cached for token: $purchaseToken. Skipping cache." }
            return
        }

        try {
            val key = "$KEY_PREFIX$tokenHash"
            val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), data)
            sharedPreferences.edit { putString(key, jsonString) }

            debugLog { "Local transaction metadata cache updated" }
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to serialize local transaction metadata" }
        }
    }

    public fun getLocalTransactionMetadata(purchaseToken: String): LocalTransactionMetadata? {
        val tokenHash = getTokenHash(purchaseToken)
        val key = "$KEY_PREFIX$tokenHash"
        val jsonString = sharedPreferences.getString(key, null) ?: return null

        return try {
            json.decodeFromString(LocalTransactionMetadata.serializer(), jsonString)
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to deserialize local transaction metadata. Clearing cache." }
            sharedPreferences.edit { remove(key) }
            null
        }
    }

    public fun getAllLocalTransactionMetadata(): List<LocalTransactionMetadata> {
        val allKeys = sharedPreferences.all.keys.filter { it.startsWith(KEY_PREFIX) }
        val result = mutableListOf<LocalTransactionMetadata>()

        for (key in allKeys) {
            val jsonString = sharedPreferences.getString(key, null)
            if (jsonString != null) {
                try {
                    val metadata = json.decodeFromString(LocalTransactionMetadata.serializer(), jsonString)
                    result.add(metadata)
                } catch (e: SerializationException) {
                    errorLog(e) { "Failed to deserialize transaction metadata for key: $key" }
                    sharedPreferences.edit { remove(key) }
                }
            }
        }

        return result
    }

    @Synchronized
    public fun clearLocalTransactionMetadata(purchaseTokens: Set<String>) {
        if (purchaseTokens.isEmpty()) {
            return
        }

        val tokenHashes = purchaseTokens.map { getTokenHash(it) }
        var removedCount = 0

        sharedPreferences.edit {
            tokenHashes.forEach { tokenHash ->
                val key = "$KEY_PREFIX$tokenHash"
                if (sharedPreferences.contains(key)) {
                    remove(key)
                    removedCount++
                }
            }
        }

        if (removedCount > 0) {
            verboseLog { "Cleared local transaction metadata for $removedCount token(s)" }
        } else {
            debugLog { "No transaction metadata found to clear from local cache." }
        }
    }

    // Private helper methods

    private fun hasCachedData(tokenHash: String): Boolean {
        return sharedPreferences.contains("$KEY_PREFIX$tokenHash")
    }

    private fun getTokenHash(purchaseToken: String): String {
        return purchaseToken.sha1()
    }
}
