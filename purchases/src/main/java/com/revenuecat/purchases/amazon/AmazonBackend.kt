package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.networking.Endpoint
import org.json.JSONObject

/** @suppress */
typealias PostAmazonReceiptCallback = Pair<(response: JSONObject) -> Unit, (PurchasesError) -> Unit>

/** @suppress */
typealias CallbackCacheKey = List<String>

internal class AmazonBackend(
    private val backendHelper: BackendHelper,
) {

    @get:Synchronized @set:Synchronized
    @Volatile var postAmazonReceiptCallbacks = mutableMapOf<CallbackCacheKey, MutableList<PostAmazonReceiptCallback>>()

    fun getAmazonReceiptData(
        receiptId: String,
        storeUserID: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        val cacheKey = listOfNotNull(
            receiptId,
            storeUserID,
        )

        val call = {
            backendHelper.performRequest(
                Endpoint.GetAmazonReceipt(storeUserID, receiptId),
                null,
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
                            onSuccess(body)
                        }
                    }
                },
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
