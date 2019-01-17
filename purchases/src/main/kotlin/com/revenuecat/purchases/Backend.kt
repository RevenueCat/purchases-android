//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.net.Uri
import org.json.JSONException
import org.json.JSONObject

private const val UNSUCCESSFUL_HTTP_STATUS_CODE = 300

internal class Backend(
    private val apiKey: String,
    private val dispatcher: Dispatcher,
    private val httpClient: HTTPClient
) {

    internal val authenticationHeaders: MutableMap<String, String>

    var callbacks: MutableMap<List<String>, MutableList<Pair<(PurchaserInfo) -> Unit, (PurchasesError) -> Unit>>> =
        mutableMapOf()
    var entitlementsCallbacks =
        mutableMapOf<List<String>, MutableList<Pair<(Map<String, Entitlement>) -> Unit, (PurchasesError) -> Unit>>>()

    private abstract inner class PurchaserInfoReceivingCall internal constructor(
        private val cacheKey: List<String>
    ) : Dispatcher.AsyncCall() {

        override fun onCompletion(result: HTTPClient.Result) {
            callbacks.remove(cacheKey)?.forEach { (onSuccess, onError) ->
                if (result.isSuccessful()) {
                    try {
                        onSuccess(result.body!!.buildPurchaserInfo())
                    } catch (e: JSONException) {
                        log("Error parsing JSON ${e.localizedMessage}")
                        onError(
                            PurchasesError(
                                Purchases.ErrorDomains.REVENUECAT_BACKEND,
                                result.responseCode,
                                e.localizedMessage
                            )
                        )
                    }
                } else {
                    onError(
                        PurchasesError(
                            Purchases.ErrorDomains.REVENUECAT_BACKEND,
                            result.responseCode,
                            try {
                                "Server error: ${result.body!!.getString("message")}"
                            } catch (jsonException: JSONException) {
                                "Unexpected error from backend ${result.responseCode}"
                            }
                        )
                    )
                }
            }
        }

        override fun onError(code: Int, message: String) {
            callbacks.remove(cacheKey)?.forEach { (_, onError) ->
                onError(PurchasesError(Purchases.ErrorDomains.REVENUECAT_BACKEND, code, message))
            }
        }
    }

    init {
        this.authenticationHeaders = HashMap()
        this.authenticationHeaders["Authorization"] = "Bearer " + this.apiKey
    }

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
        if (!callbacks.containsKey(cacheKey)) {
            callbacks[cacheKey] = mutableListOf(onSuccess to onError)
            enqueue(object : PurchaserInfoReceivingCall(cacheKey) {
                override fun call(): HTTPClient.Result {
                    return httpClient.performRequest(
                        "/subscribers/" + encode(appUserID),
                        null as Map<*, *>?,
                        authenticationHeaders
                    )
                }
            })
        } else {
            callbacks[cacheKey]!!.add(onSuccess to onError)
        }
    }

    fun postReceiptData(
        purchaseToken: String,
        appUserID: String,
        productID: String,
        isRestore: Boolean,
        onSuccess: (PurchaserInfo) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val cacheKey = listOf(purchaseToken, productID, appUserID, isRestore.toString())
        if (!callbacks.containsKey(cacheKey)) {
            callbacks[cacheKey] = mutableListOf(onSuccess to onError)

            val body = HashMap<String, Any?>()

            body["fetch_token"] = purchaseToken
            body["product_id"] = productID
            body["app_user_id"] = appUserID
            body["is_restore"] = isRestore

            enqueue(object : PurchaserInfoReceivingCall(cacheKey) {
                override fun call(): HTTPClient.Result {
                    return httpClient.performRequest("/receipts", body, authenticationHeaders)
                }
            })
        } else {
            callbacks[cacheKey]!!.add(onSuccess to onError)
        }
    }

    fun getEntitlements(
        appUserID: String,
        onSuccess: (Map<String, Entitlement>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val path = "/subscribers/" + encode(appUserID) + "/products"
        val cacheKey = listOf(path)

        if (!entitlementsCallbacks.containsKey(cacheKey)) {
            entitlementsCallbacks[cacheKey] = mutableListOf(onSuccess to onError)

            enqueue(object : Dispatcher.AsyncCall() {
                override fun call(): HTTPClient.Result {
                    return httpClient.performRequest(
                        path,
                        null as Map<*, *>?,
                        authenticationHeaders
                    )
                }

                override fun onError(code: Int, message: String) {
                    entitlementsCallbacks.remove(cacheKey)?.forEach { (_, onError) ->
                        onError(
                            PurchasesError(
                                Purchases.ErrorDomains.REVENUECAT_BACKEND,
                                code,
                                message
                            )
                        )
                    }
                }

                override fun onCompletion(result: HTTPClient.Result) {
                    entitlementsCallbacks.remove(cacheKey)?.forEach { (onSuccess, onError) ->
                        if (result.isSuccessful()) {
                            try {
                                val entitlementsResponse =
                                    result.body!!.getJSONObject("entitlements")
                                onSuccess(entitlementsResponse.buildEntitlementsMap())
                            } catch (e: JSONException) {
                                onError(
                                    PurchasesError(
                                        Purchases.ErrorDomains.REVENUECAT_BACKEND,
                                        result.responseCode,
                                        "Error parsing products JSON " + e.localizedMessage
                                    )
                                )
                            }
                        } else {
                            onError(
                                PurchasesError(
                                    Purchases.ErrorDomains.REVENUECAT_BACKEND,
                                    result.responseCode,
                                    "Backend error"
                                )
                            )
                        }
                    }
                }
            })
        } else {
            entitlementsCallbacks[cacheKey]!!.add(onSuccess to onError)
        }
    }

    private fun encode(string: String): String {
        return Uri.encode(string)
    }

    fun postAttributionData(
        appUserID: String,
        network: Purchases.AttributionNetwork,
        data: JSONObject
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
        })
    }

    fun createAlias(
        appUserID: String,
        newAppUserID: String,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        val body = mapOf(
            "new_app_user_id" to newAppUserID
        )

        enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/" + encode(appUserID) + "/alias",
                    body,
                    authenticationHeaders
                )
            }

            override fun onError(code: Int, message: String) {
                onErrorHandler(
                    PurchasesError(
                        Purchases.ErrorDomains.REVENUECAT_BACKEND,
                        code,
                        message
                    )
                )
            }

            override fun onCompletion(result: HTTPClient.Result) {
                if (result.isSuccessful()) {
                    onSuccessHandler()
                } else {
                    onErrorHandler(
                        PurchasesError(
                            Purchases.ErrorDomains.REVENUECAT_BACKEND,
                            result.responseCode,
                            "Backend error"
                        )
                    )
                }
            }
        })
    }

    private fun HTTPClient.Result.isSuccessful(): Boolean {
        return responseCode < UNSUCCESSFUL_HTTP_STATUS_CODE
    }

}
