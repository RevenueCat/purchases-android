package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.toPurchasesError
import org.json.JSONException
import org.json.JSONObject

/** @suppress */
typealias PostAmazonReceiptCallback = Pair<(response: JSONObject) -> Unit, (PurchasesError) -> Unit>
/** @suppress */
typealias CallbackCacheKey = List<String>

class AmazonBackend(
    private val backend: Backend
) {

    @get:Synchronized @set:Synchronized
    @Volatile var postAmazonReceiptCallbacks = mutableMapOf<CallbackCacheKey, MutableList<PostAmazonReceiptCallback>>()

    fun getAmazonReceiptData(
        receiptId: String,
        appUserID: String,
        storeUserID: String,
        sku: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val cacheKey = listOfNotNull(
            receiptId,
            appUserID,
            storeUserID,
            sku
        )

        val body = mapOf(
            "fetch_token" to receiptId,
            "product_id" to sku,
            "app_user_id" to appUserID,
            "store_user_id" to storeUserID
        )

        val call = {
            backend.performRequest(
                "/receipts/amazon",
                body,
                { error ->
                    synchronized(this@AmazonBackend) {
                        postAmazonReceiptCallbacks.remove(cacheKey)
                    }?.forEach { (_, onError) ->
                        onError(error)
                    }
                },
                { error, _, body ->
                    synchronized(this@AmazonBackend) {
                        postAmazonReceiptCallbacks.remove(cacheKey)
                    }?.forEach { (onSuccess, onError) ->
                        if (error != null) {
                            onError(error)
                        } else {
                            try {
                                onSuccess(body)
                            } catch (e: JSONException) {
                                onError(e.toPurchasesError().also { errorLog(it) })
                            }
                        }
                    }
                }
            )
        }

        val functions = onSuccess to onError
        synchronized(this@AmazonBackend) {
            if (!postAmazonReceiptCallbacks.containsKey(cacheKey)) {
                postAmazonReceiptCallbacks[cacheKey] = mutableListOf(functions)
                call.invoke()
            } else {
                postAmazonReceiptCallbacks[cacheKey]!!.add(functions)
            }
        }
    }
}
