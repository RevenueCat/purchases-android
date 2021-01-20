package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import org.json.JSONObject

const val RECEIPTS_TO_TERM_SKUS_KEY = "receiptsToTermSkus"

internal class AmazonCache(
    private val deviceCache: DeviceCache
) {

    internal val amazonPostedTokensKey: String by lazy {
        deviceCache.newKey("amazon.tokens")
    }

    @Synchronized
    fun setReceiptTermSkus(receiptsToTermSkus: Map<String, String>) {
        log(LogIntent.DEBUG, AmazonStrings.CACHING_RECEIPT_TERM_SKUS.format(receiptsToTermSkus))

        val currentlyCached = getReceiptTermSkus()

        val toCache = currentlyCached + receiptsToTermSkus

        val innerJSONObject = JSONObject(toCache)
        val jsonToCache = JSONObject().also { it.put(RECEIPTS_TO_TERM_SKUS_KEY, innerJSONObject) }
        deviceCache.putString(amazonPostedTokensKey, jsonToCache.toString())
    }

    @Synchronized
    fun getReceiptTermSkus(): Map<String, String> {
        val receiptTermSkusJSONObject =
            deviceCache.getJSONObjectOrNull(amazonPostedTokensKey)?.getJSONObject(RECEIPTS_TO_TERM_SKUS_KEY)

        return receiptTermSkusJSONObject?.keys()?.asSequence()?.map { jsonKey ->
            jsonKey to receiptTermSkusJSONObject[jsonKey] as String
        }?.toMap() ?: emptyMap()
    }

    @Synchronized
    fun addSuccessfullyPostedToken(token: String) {
        deviceCache.addSuccessfullyPostedToken(token)
    }
}
