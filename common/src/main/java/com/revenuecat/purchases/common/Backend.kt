//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.net.Uri
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.attribution.AttributionNetwork
import org.json.JSONException
import org.json.JSONObject

private const val HTTP_STATUS_CREATED = 201
private const val UNSUCCESSFUL_HTTP_STATUS_CODE = 300
const val HTTP_SERVER_ERROR_CODE = 500
const val HTTP_NOT_FOUND_ERROR_CODE = 404

const val ATTRIBUTES_ERROR_RESPONSE_KEY = "attributes_error_response"
const val ATTRIBUTE_ERRORS_KEY = "attribute_errors"

/** @suppress */
internal typealias PurchaserInfoCallback = Pair<(PurchaserInfo) -> Unit, (PurchasesError) -> Unit>

/** @suppress */
typealias PostReceiptCallback = Pair<PostReceiptDataSuccessCallback, PostReceiptDataErrorCallback>
/** @suppress */
typealias CallbackCacheKey = List<String>
/** @suppress */
typealias OfferingsCallback = Pair<(JSONObject) -> Unit, (PurchasesError) -> Unit>
/** @suppress */
typealias PostReceiptDataSuccessCallback = (PurchaserInfo, body: JSONObject) -> Unit
/** @suppress */
typealias PostReceiptDataErrorCallback = (PurchasesError, shouldConsumePurchase: Boolean, body: JSONObject?) -> Unit

class Backend(
    private val apiKey: String,
    private val dispatcher: Dispatcher,
    private val httpClient: HTTPClient
) {

    internal val authenticationHeaders = mapOf("Authorization" to "Bearer ${this.apiKey}")

    @get:Synchronized @set:Synchronized
    @Volatile var callbacks = mutableMapOf<CallbackCacheKey, MutableList<PurchaserInfoCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var postReceiptCallbacks = mutableMapOf<CallbackCacheKey, MutableList<PostReceiptCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var offeringsCallbacks = mutableMapOf<String, MutableList<OfferingsCallback>>()

    fun close() {
        this.dispatcher.close()
    }

    fun performRequest(
        path: String,
        body: Map<String, Any?>?,
        onError: (PurchasesError) -> Unit,
        onCompletedSuccessfully: () -> Unit,
        onCompletedWithErrors: (PurchasesError, Int, JSONObject) -> Unit
    ) {
        enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    path,
                    body,
                    authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                onError(error)
            }

            override fun onCompletion(result: HTTPClient.Result) {
                if (result.isSuccessful()) {
                    onCompletedSuccessfully()
                } else {
                    val error = result.toPurchasesError().also { errorLog(it) }
                    onCompletedWithErrors(error, result.responseCode, result.body)
                }
            }
        })
    }

    private fun enqueue(
        call: Dispatcher.AsyncCall,
        randomDelay: Boolean = false
    ) {
        if (!dispatcher.isClosed()) {
            dispatcher.enqueue(call, randomDelay)
        }
    }

    fun getPurchaserInfo(
        appUserID: String,
        appInBackground: Boolean,
        onSuccess: (PurchaserInfo) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val path = "/subscribers/" + encode(appUserID)
        val cacheKey = listOf(path)
        val call = object : Dispatcher.AsyncCall() {

            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/" + encode(appUserID),
                    null,
                    authenticationHeaders
                )
            }

            override fun onCompletion(result: HTTPClient.Result) {
                synchronized(this@Backend) {
                    callbacks.remove(cacheKey)
                }?.forEach { (onSuccess, onError) ->
                    try {
                        if (result.isSuccessful()) {
                            onSuccess(result.body.buildPurchaserInfo())
                        } else {
                            onError(result.toPurchasesError().also { errorLog(it) })
                        }
                    } catch (e: JSONException) {
                        onError(e.toPurchasesError().also { errorLog(it) })
                    }
                }
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    callbacks.remove(cacheKey)
                }?.forEach { (_, onError) ->
                    onError(error)
                }
            }
        }
        synchronized(this@Backend) {
            callbacks.addCallback(call, cacheKey, onSuccess to onError, randomDelay = appInBackground)
        }
    }

    fun postReceiptData(
        purchaseToken: String,
        appUserID: String,
        isRestore: Boolean,
        observerMode: Boolean,
        subscriberAttributes: Map<String, Map<String, Any?>>,
        productInfo: ProductInfo,
        onSuccess: PostReceiptDataSuccessCallback,
        onError: PostReceiptDataErrorCallback
    ) {
        val cacheKey = listOfNotNull(
            purchaseToken,
            appUserID,
            isRestore.toString(),
            observerMode.toString(),
            subscriberAttributes.toString(),
            productInfo.toString()
        )

        val body = mapOf(
            "fetch_token" to purchaseToken,
            "product_id" to productInfo.productID,
            "app_user_id" to appUserID,
            "is_restore" to isRestore,
            "presented_offering_identifier" to productInfo.offeringIdentifier,
            "observer_mode" to observerMode,
            "price" to productInfo.price,
            "currency" to productInfo.currency,
            "attributes" to subscriberAttributes.takeUnless { it.isEmpty() },
            "normal_duration" to productInfo.duration,
            "intro_duration" to productInfo.introDuration,
            "trial_duration" to productInfo.trialDuration
        ).filterValues { value -> value != null }

        val call = object : Dispatcher.AsyncCall() {

            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/receipts",
                    body,
                    authenticationHeaders
                )
            }

            override fun onCompletion(result: HTTPClient.Result) {
                synchronized(this@Backend) {
                    postReceiptCallbacks.remove(cacheKey)
                }?.forEach { (onSuccess, onError) ->
                    try {
                        if (result.isSuccessful()) {
                            onSuccess(result.body.buildPurchaserInfo(), result.body)
                        } else {
                            onError(
                                result.toPurchasesError().also { errorLog(it) },
                                result.responseCode < HTTP_SERVER_ERROR_CODE,
                                result.body
                            )
                        }
                    } catch (e: JSONException) {
                        onError(e.toPurchasesError().also { errorLog(it) }, false, null)
                    }
                }
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    postReceiptCallbacks.remove(cacheKey)
                }?.forEach { (_, onError) ->
                    onError(
                        error,
                        false,
                        null
                    )
                }
            }
        }
        synchronized(this@Backend) {
            postReceiptCallbacks.addCallback(call, cacheKey, onSuccess to onError)
        }
    }

    fun getOfferings(
        appUserID: String,
        appInBackground: Boolean,
        onSuccess: (JSONObject) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val path = "/subscribers/" + encode(appUserID) + "/offerings"
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    path,
                    null,
                    authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    offeringsCallbacks.remove(path)
                }?.forEach { (_, onError) ->
                    onError(error)
                }
            }

            override fun onCompletion(result: HTTPClient.Result) {
                synchronized(this@Backend) {
                    offeringsCallbacks.remove(path)
                }?.forEach { (onSuccess, onError) ->
                    if (result.isSuccessful()) {
                        try {
                            onSuccess(result.body)
                        } catch (e: JSONException) {
                            onError(e.toPurchasesError().also { errorLog(it) })
                        }
                    } else {
                        onError(result.toPurchasesError().also { errorLog(it) })
                    }
                }
            }
        }
        synchronized(this@Backend) {
            offeringsCallbacks.addCallback(call, path, onSuccess to onError, randomDelay = appInBackground)
        }
    }

    private fun encode(string: String): String {
        return Uri.encode(string)
    }

    fun postAttributionData(
        appUserID: String,
        network: AttributionNetwork,
        data: JSONObject,
        onSuccessHandler: () -> Unit
    ) {
        if (data.length() == 0) return

        val body = mapOf(
            "network" to network.serverValue,
            "data" to data
        )

        enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/" + encode(appUserID) + "/attribution",
                    body,
                    authenticationHeaders
                )
            }

            override fun onCompletion(result: HTTPClient.Result) {
                if (result.isSuccessful()) {
                    onSuccessHandler()
                }
            }
        })
    }

    fun createAlias(
        appUserID: String,
        newAppUserID: String,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/" + encode(appUserID) + "/alias",
                    mapOf("new_app_user_id" to newAppUserID),
                    authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                onErrorHandler(error)
            }

            override fun onCompletion(result: HTTPClient.Result) {
                if (result.isSuccessful()) {
                    onSuccessHandler()
                } else {
                    onErrorHandler(result.toPurchasesError().also { errorLog(it) })
                }
            }
        })
    }

    fun logIn(
        appUserID: String,
        newAppUserID: String,
        onSuccessHandler: (PurchaserInfo, Boolean) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/identify",
                    mapOf(
                        "new_app_user_id" to newAppUserID,
                        "app_user_id" to appUserID
                    ),
                    authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                onErrorHandler(error)
            }

            override fun onCompletion(result: HTTPClient.Result) {
                if (result.isSuccessful()) {
                    val created = result.responseCode == HTTP_STATUS_CREATED
                    if (result.body.length() > 0) {
                        val purchaserInfo = result.body.buildPurchaserInfo()
                        onSuccessHandler(purchaserInfo, created)
                    } else {
                        onErrorHandler(PurchasesError(PurchasesErrorCode.UnknownError)
                            .also { errorLog(it) })
                    }
                } else {
                    onErrorHandler(result.toPurchasesError().also { errorLog(it) })
                }
            }
        })
    }

    private fun HTTPClient.Result.isSuccessful(): Boolean {
        return responseCode < UNSUCCESSFUL_HTTP_STATUS_CODE
    }

    private fun <K, S, E> MutableMap<K, MutableList<Pair<S, E>>>.addCallback(
        call: Dispatcher.AsyncCall,
        cacheKey: K,
        functions: Pair<S, E>,
        randomDelay: Boolean = false
    ) {
        if (!containsKey(cacheKey)) {
            this[cacheKey] = mutableListOf(functions)
            enqueue(call, randomDelay)
        } else {
            this[cacheKey]!!.add(functions)
        }
    }
}
