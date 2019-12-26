//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.net.Uri
import org.json.JSONException
import org.json.JSONObject

private const val UNSUCCESSFUL_HTTP_STATUS_CODE = 300
private const val HTTP_SERVER_ERROR_CODE = 500

/** @suppress */
typealias PurchaserInfoCallback = Pair<(PurchaserInfo) -> Unit, (PurchasesError) -> Unit>
/** @suppress */
typealias PostReceiptCallback = Pair<(PurchaserInfo) -> Unit, (PurchasesError, shouldConsumePurchase: Boolean) -> Unit>
/** @suppress */
typealias CallbackCacheKey = List<String>
/** @suppress */
typealias OfferingsCallback = Pair<(JSONObject) -> Unit, (PurchasesError) -> Unit>

internal class Backend(
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

    private fun enqueue(call: Dispatcher.AsyncCall) {
        if (!dispatcher.isClosed()) {
            dispatcher.enqueue(call)
        }
    }

    fun getPurchaserInfo(
        appUserID: String,
        onSuccess: (PurchaserInfo) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val path = "/subscribers/" + encode(appUserID)
        val cacheKey = listOf(path)
        val call = object : Dispatcher.AsyncCall() {

            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/" + encode(appUserID),
                    null as Map<*, *>?,
                    authenticationHeaders
                )
            }

            override fun onCompletion(result: HTTPClient.Result) {
                synchronized(this@Backend) {
                    callbacks.remove(cacheKey)
                }?.forEach { (onSuccess, onError) ->
                    try {
                        if (result.isSuccessful()) {
                            onSuccess(result.body!!.buildPurchaserInfo())
                        } else {
                            onError(result.toPurchasesError())
                        }
                    } catch (e: JSONException) {
                        onError(e.toPurchasesError())
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
            callbacks.addCallback(call, cacheKey, onSuccess to onError)
        }
    }

    fun postReceiptData(
        purchaseToken: String,
        appUserID: String,
        productID: String,
        isRestore: Boolean,
        offeringIdentifier: String?,
        observerMode: Boolean,
        onSuccess: (PurchaserInfo) -> Unit,
        onError: (PurchasesError, errorIsFinishable: Boolean) -> Unit
    ) {
        val cacheKey = listOfNotNull(
            purchaseToken,
            productID,
            appUserID,
            isRestore.toString(),
            offeringIdentifier,
            observerMode.toString()
        )

        val body = mapOf(
            "fetch_token" to purchaseToken,
            "product_id" to productID,
            "app_user_id" to appUserID,
            "is_restore" to isRestore,
            "presented_offering_identifier" to offeringIdentifier,
            "observer_mode" to observerMode
        )

        val call = object : Dispatcher.AsyncCall() {

            override fun call(): HTTPClient.Result {
                return httpClient.performRequest("/receipts", body, authenticationHeaders)
            }

            override fun onCompletion(result: HTTPClient.Result) {
                synchronized(this@Backend) {
                    postReceiptCallbacks.remove(cacheKey)
                }?.forEach { (onSuccess, onError) ->
                    try {
                        if (result.isSuccessful()) {
                            onSuccess(result.body!!.buildPurchaserInfo())
                        } else {
                            onError(result.toPurchasesError(), result.responseCode < HTTP_SERVER_ERROR_CODE)
                        }
                    } catch (e: JSONException) {
                        onError(e.toPurchasesError(), false)
                    }
                }
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    postReceiptCallbacks.remove(cacheKey)
                }?.forEach { (_, onError) ->
                    onError(error, false)
                }
            }
        }
        synchronized(this@Backend) {
            postReceiptCallbacks.addCallback(call, cacheKey, onSuccess to onError)
        }
    }

    fun getOfferings(
        appUserID: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val path = "/subscribers/" + encode(appUserID) + "/offerings"
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    path,
                    null as Map<*, *>?,
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
                            onSuccess(result.body!!)
                        } catch (e: JSONException) {
                            onError(e.toPurchasesError())
                        }
                    } else {
                        onError(result.toPurchasesError())
                    }
                }
            }
        }
        synchronized(this@Backend) {
            offeringsCallbacks.addCallback(call, path, onSuccess to onError)
        }
    }

    private fun encode(string: String): String {
        return Uri.encode(string)
    }

    fun postAttributionData(
        appUserID: String,
        network: Purchases.AttributionNetwork,
        data: JSONObject,
        onSuccessHandler: () -> Unit
    ) {
        if (data.length() == 0) return

        val body = JSONObject()
        try {
            body.put("network", network.serverValue)
            body.put("data", data)
        } catch (e: JSONException) {
            return
        }

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
                    onErrorHandler(result.toPurchasesError())
                }
            }
        })
    }

    fun getManageSubscriptionURL(
        productID: String,
        appUserID: String,
        onSuccessHandler: (Uri) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/${encode(appUserID)}/subscriptions/$productID/manage",
                    null as Map<*, *>?,
                    authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                onErrorHandler(error)
            }

            override fun onCompletion(result: HTTPClient.Result) {
                if (result.isSuccessful()) {
                    onSuccessHandler(Uri.parse(result.body!!["url"] as String))
                } else {
                    onErrorHandler(result.toPurchasesError())
                }
            }
        })

    }

    private fun HTTPClient.Result.isSuccessful(): Boolean {
        return responseCode < UNSUCCESSFUL_HTTP_STATUS_CODE
    }

    private fun <K, S, E> MutableMap<K, MutableList<Pair<S, E>>>.addCallback(call: Dispatcher.AsyncCall, cacheKey: K, functions: Pair<S, E>) {
        if (!containsKey(cacheKey)) {
            this[cacheKey] = mutableListOf(functions)
            enqueue(call)
        } else {
            this[cacheKey]!!.add(functions)
        }
    }

}
