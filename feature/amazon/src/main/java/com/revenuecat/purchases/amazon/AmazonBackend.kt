package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BuildConfig
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.models.ProductDetails
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

    fun postAmazonReceiptData(
        receiptId: String,
        appUserID: String,
        storeUserID: String,
        productDetails: ProductDetails,
        onSuccessHandler: (JSONObject) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        val cacheKey = listOfNotNull(
            receiptId,
            appUserID,
            storeUserID,
            productDetails.toString()
        )

        val body = mapOf(
            "fetch_token" to receiptId,
            "product_id" to productDetails.sku,
            "app_user_id" to appUserID,
            "store_user_id" to storeUserID,
            "shared_secret" to BuildConfig.AMAZON_SHARED_SECRET
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

        val functions = onSuccessHandler to onErrorHandler
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
