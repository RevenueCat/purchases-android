package com.revenuecat.purchases.common.caching

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.verboseLog
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class LocalTransactionMetadataCache(
    private val deviceCache: DeviceCache,
    private val json: Json = JsonTools.json,
) {
    private companion object Companion {
        const val CACHE_KEY = "local_transaction_metadata"
    }

    @Synchronized
    fun cacheLocalTransactionMetadata(
        purchaseToken: String,
        data: LocalTransactionMetadata.TransactionMetadata,
    ) {
        if (hasCachedData(purchaseToken)) {
            debugLog { "Purchase data already cached for token: $purchaseToken. Skipping cache." }
            return
        }

        val localTransactionMetadata = getCachedData() ?: LocalTransactionMetadata(purchaseDataByTokenHash = emptyMap())
        val updatedData = localTransactionMetadata.copy(
            purchaseDataByTokenHash = localTransactionMetadata.purchaseDataByTokenHash + (purchaseToken.sha1() to data),
        )

        try {
            val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), updatedData)
            deviceCache.putString(CACHE_KEY, jsonString)

            verboseLog { "Local transaction metadata cache updated" }
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to serialize local transaction metadata" }
        }
    }

    @Synchronized
    fun getLocalTransactionMetadata(purchaseToken: String): LocalTransactionMetadata.TransactionMetadata? {
        return getCachedData()?.purchaseDataByTokenHash?.get(purchaseToken.sha1())
    }

    @Synchronized
    fun clearLocalTransactionMetadata(purchaseToken: String) {
        val existingData = getCachedData() ?: return
        if (!existingData.purchaseDataByTokenHash.containsKey(purchaseToken.sha1())) {
            debugLog { "Transaction metadata not found when trying to clear it from local cache. Ignoring" }
            return
        }
        existingData
            .copy(
                purchaseDataByTokenHash = existingData
                    .purchaseDataByTokenHash
                    .filter { it.key != purchaseToken.sha1() },
            )
            .let { updatedData ->
                try {
                    val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), updatedData)
                    deviceCache.putString(CACHE_KEY, jsonString)
                    verboseLog { "Cleared cached data for purchaseToken" }
                } catch (e: SerializationException) {
                    errorLog(e) { "Failed to serialize updated local transaction metadata when clearing cached data." }
                }
            }
    }

    // Private helper methods

    private fun hasCachedData(purchaseToken: String): Boolean {
        return getLocalTransactionMetadata(purchaseToken) != null
    }

    private fun getCachedData(): LocalTransactionMetadata? {
        val jsonString = deviceCache.getJSONObjectOrNull(CACHE_KEY)?.toString()
            ?: return null

        return try {
            json.decodeFromString(LocalTransactionMetadata.serializer(), jsonString)
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to deserialize local transaction metadata. Clearing cache." }
            deviceCache.remove(CACHE_KEY)
            null
        }
    }
}
