package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.utils.toMap
import org.json.JSONObject

const val RECEIPTS_TO_SKUS_KEY = "receiptsToSkus"

internal class AmazonCache(
    private val deviceCache: DeviceCache,
) {

    internal val amazonPostedTokensKey: String by lazy {
        deviceCache.newKey("amazon.tokens")
    }

    @Synchronized
    fun cacheSkusByToken(receiptsToSkus: Map<String, String>) {
        log(LogIntent.DEBUG, AmazonStrings.CACHING_RECEIPT_TERM_SKUS.format(receiptsToSkus))

        val currentlyCached = getReceiptSkus()

        val toCache = currentlyCached + receiptsToSkus

        val innerJSONObject = JSONObject(toCache)
        val jsonToCache = JSONObject().also { it.put(RECEIPTS_TO_SKUS_KEY, innerJSONObject) }
        deviceCache.putString(amazonPostedTokensKey, jsonToCache.toString())
    }

    @Synchronized
    fun getReceiptSkus(): Map<String, String> {
        val receiptToSkusJSONObject =
            deviceCache.getJSONObjectOrNull(amazonPostedTokensKey)?.getJSONObject(RECEIPTS_TO_SKUS_KEY)

        return receiptToSkusJSONObject?.toMap() ?: emptyMap()
    }

    @Synchronized
    fun addSuccessfullyPostedToken(token: String) {
        deviceCache.addSuccessfullyPostedToken(token)
    }
}
