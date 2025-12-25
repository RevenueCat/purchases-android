package com.revenuecat.purchases.common.caching

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.verboseLog
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class CachedPurchaseDataCache(
    private val deviceCache: DeviceCache,
    private val json: Json = JsonTools.json,
) {
    private companion object {
        const val CACHE_KEY_PREFIX = "cached_purchase_data"
    }

    @Synchronized
    fun cachePurchaseData(
        purchaseToken: String,
        data: CachedPurchaseData,
    ) {
        if (hasCachedData(purchaseToken)) {
            debugLog { "Purchase data already cached for token: $purchaseToken. Skipping cache." }
            return
        }

        val cacheKey = buildCacheKey(purchaseToken)

        try {
            val jsonString = json.encodeToString(CachedPurchaseData.serializer(), data)
            deviceCache.putString(cacheKey, jsonString)

            verboseLog { "Cached purchase data with key: $cacheKey" }
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to serialize purchase data for key: $cacheKey" }
        }
    }

    @Synchronized
    fun getCachedPurchaseData(purchaseToken: String): CachedPurchaseData? {
        val tokenKey = buildCacheKey(purchaseToken)
        return getCachedDataByKey(tokenKey)
    }

    @Synchronized
    fun clearCachedPurchaseData(purchaseToken: String) {
        val tokenKey = buildCacheKey(purchaseToken)
        deviceCache.remove(tokenKey)
        verboseLog { "Cleared cached data for purchaseToken" }
    }

    // Private helper methods

    private fun hasCachedData(purchaseToken: String): Boolean {
        return getCachedPurchaseData(purchaseToken) != null
    }

    private fun getCachedDataByKey(cacheKey: String): CachedPurchaseData? {
        val jsonString = deviceCache.getJSONObjectOrNull(cacheKey)?.toString()
            ?: return null

        return try {
            json.decodeFromString(CachedPurchaseData.serializer(), jsonString)
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to deserialize purchase data from key: $cacheKey" }
            deviceCache.remove(cacheKey)
            null
        }
    }

    private fun buildCacheKey(purchaseToken: String): String {
        return deviceCache.newKey("$CACHE_KEY_PREFIX.token.${purchaseToken.sha1()}")
    }
}
