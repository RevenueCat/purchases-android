//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.net.Uri
import com.revenuecat.purchases.attributes.SubscriberAttribute
import org.json.JSONException
import org.json.JSONObject

private const val UNSUCCESSFUL_HTTP_STATUS_CODE = 300
private const val HTTP_SERVER_ERROR_CODE = 500

/** @suppress */
internal typealias PurchaserInfoCallback = Pair<(PurchaserInfo) -> Unit, (PurchasesError) -> Unit>
/** @suppress */
internal typealias PostReceiptCallback = Pair<(PurchaserInfo, List<SubscriberAttributeError>) -> Unit, (PurchasesError, shouldConsumePurchase: Boolean, List<SubscriberAttributeError>) -> Unit>
/** @suppress */
internal typealias CallbackCacheKey = List<String>
/** @suppress */
internal typealias OfferingsCallback = Pair<(JSONObject) -> Unit, (PurchasesError) -> Unit>

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
        price: Double?,
        currency: String?,
        subscriberAttributes: Map<String, SubscriberAttribute>,
        onSuccess: (PurchaserInfo, attributeErrors: List<SubscriberAttributeError>) -> Unit,
        onError: (PurchasesError, errorIsFinishable: Boolean, attributeErrors: List<SubscriberAttributeError>) -> Unit
    ) {
        val cacheKey = listOfNotNull(
            purchaseToken,
            productID,
            appUserID,
            isRestore.toString(),
            offeringIdentifier,
            observerMode.toString(),
            price?.toString(),
            currency,
            subscriberAttributes.toString()
        )

        val body = mapOf(
            "fetch_token" to purchaseToken,
            "product_id" to productID,
            "app_user_id" to appUserID,
            "is_restore" to isRestore,
            "presented_offering_identifier" to offeringIdentifier,
            "observer_mode" to observerMode,
            "price" to price,
            "currency" to currency,
            "attributes" to subscriberAttributes.takeUnless { it.isEmpty() }?.toBackendMap()
        ).mapNotNull { (key, value) ->
            value?.let { key to it }
        }.toMap()

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
                            onSuccess(result.body!!.buildPurchaserInfo(), result.body!!.getAttributeErrors())
                        } else {
                            onError(result.toPurchasesError(), result.responseCode < HTTP_SERVER_ERROR_CODE, result.body?.getAttributeErrors() ?: emptyList())
                        }
                    } catch (e: JSONException) {
                        onError(e.toPurchasesError(), false, emptyList())
                    }
                }
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    postReceiptCallbacks.remove(cacheKey)
                }?.forEach { (_, onError) ->
                    onError(error, false, emptyList())
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

    fun postSubscriberAttributes(
        attributes: Map<String, SubscriberAttribute>,
        appUserID: String,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (PurchasesError, didBackendGetAttributes: Boolean, attributeErrors: List<SubscriberAttributeError>) -> Unit
    ) {
        enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/" + encode(appUserID) + "/attributes",
                    mapOf("attributes" to attributes.toBackendMap()),
                    authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                onErrorHandler(error, false, emptyList())
            }

            override fun onCompletion(result: HTTPClient.Result) {
                if (result.isSuccessful()) {
                    onSuccessHandler()
                } else {
                    val error = result.toPurchasesError()
                    var attributeErrors: List<SubscriberAttributeError> = emptyList()
                    if (error.code == PurchasesErrorCode.InvalidSubscriberAttributesError) {
                        attributeErrors = result.body!!.getAttributeErrors()
                    }
                    onErrorHandler(error, result.responseCode < HTTP_SERVER_ERROR_CODE, attributeErrors)
                }
            }
        })
    }

    private fun JSONObject.getAttributeErrors(): List<SubscriberAttributeError> {
        val attributesErrorsList = mutableListOf<SubscriberAttributeError>()
        if (has("attribute_errors")) {
            val jsonArray = getJSONArray("attribute_errors")
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                if (jsonObject.has("key_name") and jsonObject.has("message")) {
                    attributesErrorsList.add(SubscriberAttributeError(
                        jsonObject.getString("key_name"),
                        jsonObject.getString("message")
                    ))
                }
            }
        }
        return attributesErrorsList.toList()
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

internal fun Map<String, SubscriberAttribute>.toBackendMap(): Map<String, Map<String, Any?>> {
    return map { (key, subscriberAttribute) ->
        key to subscriberAttribute.toBackendMap()
    }.toMap()
}