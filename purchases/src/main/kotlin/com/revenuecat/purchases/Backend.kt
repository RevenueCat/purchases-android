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

typealias PurchaserInfoCallback = Pair<(PurchaserInfo) -> Unit, (PurchasesError) -> Unit>
typealias PostReceiptCallback = Pair<(PurchaserInfo) -> Unit, (PurchasesError, shouldConsumePurchase: Boolean) -> Unit>
typealias CallbackCacheKey = List<String>
typealias EntitlementMapCallback = Pair<(Map<String, Entitlement>) -> Unit, (PurchasesError) -> Unit>

internal class Backend(
    private val apiKey: String,
    private val dispatcher: Dispatcher,
    private val httpClient: HTTPClient
) {

    internal val authenticationHeaders = mapOf("Authorization" to "Bearer ${this.apiKey}")

    var callbacks = mutableMapOf<CallbackCacheKey, MutableList<PurchaserInfoCallback>>()
    var postReceiptCallbacks = mutableMapOf<CallbackCacheKey, MutableList<PostReceiptCallback>>()
    var entitlementsCallbacks = mutableMapOf<String, MutableList<EntitlementMapCallback>>()

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
                synchronized(this) {
                    callbacks.remove(cacheKey)?.forEach { (onSuccess, onError) ->
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
            }

            override fun onError(error: PurchasesError) {
                synchronized(this) {
                    callbacks.remove(cacheKey)?.forEach { (_, onError) ->
                        onError(error)
                    }
                }
            }
        }
        callbacks.addCallback(call, cacheKey, onSuccess to onError)
    }

    fun postReceiptData(
        purchaseToken: String,
        appUserID: String,
        productID: String,
        isRestore: Boolean,
        onSuccess: (PurchaserInfo) -> Unit,
        onError: (PurchasesError, shouldConsumePurchase: Boolean) -> Unit
    ) {
        val cacheKey = listOf(purchaseToken, productID, appUserID, isRestore.toString())

        val body = mapOf(
            "fetch_token" to purchaseToken,
            "product_id" to productID,
            "app_user_id" to appUserID,
            "is_restore" to isRestore
        )

        val call = object : Dispatcher.AsyncCall() {

            override fun call(): HTTPClient.Result {
                return httpClient.performRequest("/receipts", body, authenticationHeaders)
            }

            override fun onCompletion(result: HTTPClient.Result) {
                synchronized(this) {
                    postReceiptCallbacks.remove(cacheKey)?.forEach { (onSuccess, onError) ->
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
            }

            override fun onError(error: PurchasesError) {
                synchronized(this) {
                    postReceiptCallbacks.remove(cacheKey)?.forEach { (_, onError) ->
                        onError(error, false)
                    }
                }
            }
        }

        postReceiptCallbacks.addCallback(call, cacheKey, onSuccess to onError)
    }

    fun getEntitlements(
        appUserID: String,
        onSuccess: (Map<String, Entitlement>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val path = "/subscribers/" + encode(appUserID) + "/products"
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    path,
                    null as Map<*, *>?,
                    authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                synchronized(this) {
                    entitlementsCallbacks.remove(path)?.forEach { (_, onError) ->
                        onError(error)
                    }
                }
            }

            override fun onCompletion(result: HTTPClient.Result) {
                synchronized(this) {
                    entitlementsCallbacks.remove(path)?.forEach { (onSuccess, onError) ->
                        if (result.isSuccessful()) {
                            try {
                                onSuccess(result.body!!.getJSONObject("entitlements").buildEntitlementsMap())
                            } catch (e: JSONException) {
                                onError(e.toPurchasesError())
                            }
                        } else {
                            onError(result.toPurchasesError())
                        }
                    }
                }
            }
        }
        entitlementsCallbacks.addCallback(call, path, onSuccess to onError)
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

    private fun HTTPClient.Result.isSuccessful(): Boolean {
        return responseCode < UNSUCCESSFUL_HTTP_STATUS_CODE
    }

    private fun <K, S, E> MutableMap<K, MutableList<Pair<S, E>>>.addCallback(call: Dispatcher.AsyncCall, cacheKey: K, functions: Pair<S, E>) {
        synchronized(this) {
            if (!containsKey(cacheKey)) {
                this[cacheKey] = mutableListOf(functions)
                enqueue(call)
            } else {
                this[cacheKey]!!.add(functions)
            }
        }
    }

}
