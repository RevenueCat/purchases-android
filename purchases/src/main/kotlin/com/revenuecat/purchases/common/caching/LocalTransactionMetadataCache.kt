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
    private companion object {
        const val CACHE_KEY = "local_transaction_metadata"
    }

    private var cachedData: LocalTransactionMetadata? = null

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
            purchaseDataByTokenHash = localTransactionMetadata.purchaseDataByTokenHash +
                (getTokenHash(purchaseToken) to data),
        )

        try {
            val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), updatedData)
            deviceCache.putString(CACHE_KEY, jsonString)
            cachedData = updatedData

            debugLog { "Local transaction metadata cache updated" }
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to serialize local transaction metadata" }
        }
    }

    @Synchronized
    fun getLocalTransactionMetadata(purchaseToken: String): LocalTransactionMetadata.TransactionMetadata? {
        return getCachedData()?.purchaseDataByTokenHash?.get(getTokenHash(purchaseToken))
    }

    @Synchronized
    fun clearLocalTransactionMetadata(purchaseTokens: List<String>) {
        val existingData = getCachedData() ?: return

        val tokenHashesToClear = purchaseTokens.map { getTokenHash(it) }.toSet()
        val existingTokenHashes = existingData.purchaseDataByTokenHash.keys

        val tokensNotFound = tokenHashesToClear - existingTokenHashes
        if (tokensNotFound.isNotEmpty()) {
            debugLog {
                "Some transaction metadata not found in cache when trying to clear: ${tokensNotFound.size} tokens. " +
                    "Ignoring."
            }
        }

        val tokensToRemove = tokenHashesToClear.intersect(existingTokenHashes)
        if (tokensToRemove.isEmpty()) {
            debugLog { "No transaction metadata found to clear from local cache." }
            return
        }

        existingData
            .copy(
                purchaseDataByTokenHash = existingData
                    .purchaseDataByTokenHash
                    .filter { it.key !in tokensToRemove },
            )
            .let { updatedData ->
                try {
                    val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), updatedData)
                    deviceCache.putString(CACHE_KEY, jsonString)
                    cachedData = updatedData
                    verboseLog { "Cleared local transaction metadata for ${tokensToRemove.size} token(s)" }
                } catch (e: SerializationException) {
                    errorLog(e) { "Failed to serialize updated local transaction metadata when clearing cached data." }
                }
            }
    }

    // Private helper methods

    private fun hasCachedData(purchaseToken: String): Boolean {
        return getLocalTransactionMetadata(purchaseToken) != null
    }

    @Suppress("ReturnCount")
    private fun getCachedData(): LocalTransactionMetadata? {
        cachedData?.let {
            return it
        }
        val jsonString = deviceCache.getJSONObjectOrNull(CACHE_KEY)?.toString()
            ?: return null

        return try {
            val localTransactionMetadata = json.decodeFromString(LocalTransactionMetadata.serializer(), jsonString)
            cachedData = localTransactionMetadata
            localTransactionMetadata
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to deserialize local transaction metadata. Clearing cache." }
            deviceCache.remove(CACHE_KEY)
            null
        }
    }

    private fun getTokenHash(purchaseToken: String): String {
        return purchaseToken.sha1()
    }
}
