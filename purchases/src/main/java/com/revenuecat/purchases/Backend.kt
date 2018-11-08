package com.revenuecat.purchases

import android.net.Uri

import org.json.JSONException
import org.json.JSONObject

import java.util.HashMap

internal class Backend(
    private val apiKey: String,
    private val dispatcher: Dispatcher,
    private val httpClient: HTTPClient,
    private val purchaserInfoFactory: PurchaserInfo.Factory,
    private val entitlementFactory: Entitlement.Factory
) {

    internal val authenticationHeaders: MutableMap<String, String>

    abstract class BackendResponseHandler {
        abstract fun onReceivePurchaserInfo(info: PurchaserInfo)
        abstract fun onError(code: Int, message: String?)
    }

    abstract class EntitlementsResponseHandler {
        abstract fun onReceiveEntitlements(entitlements: Map<String, Entitlement>)
        abstract fun onError(code: Int, message: String)
    }

    abstract class AliasResponseHandler {
        abstract fun onSuccess()
        abstract fun onError(code: Int, message: String)
    }

    private abstract inner class PurchaserInfoReceivingCall internal constructor(private val handler: BackendResponseHandler) :
        Dispatcher.AsyncCall() {

        public override fun onCompletion(result: HTTPClient.Result) {
            if (result.responseCode < 300) {
                try {
                    handler.onReceivePurchaserInfo(purchaserInfoFactory.build(result.body!!))
                } catch (e: JSONException) {
                    handler.onError(result.responseCode, e.message)
                }

            } else {
                var errorMessage: String? = null
                try {
                    val message = result.body!!.getString("message")
                    errorMessage = "Server error: $message"
                } catch (jsonException: JSONException) {
                    errorMessage = "Unexpected server error " + result.responseCode
                }

                handler.onError(result.responseCode, errorMessage)
            }
        }

        internal override fun onError(code: Int, message: String) {
            handler.onError(code, message)
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
        if (!dispatcher.isClosed) {
            dispatcher.enqueue(call)
        }
    }

    fun getSubscriberInfo(appUserID: String, handler: BackendResponseHandler) {
        enqueue(object : PurchaserInfoReceivingCall(handler) {
            @Throws(HTTPClient.HTTPErrorException::class)
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/" + encode(appUserID),
                    null as Map<*, *>?,
                    authenticationHeaders
                )
            }
        })
    }

    fun postReceiptData(
        purchaseToken: String,
        appUserID: String,
        productID: String,
        isRestore: Boolean?,
        handler: BackendResponseHandler
    ) {
        val body = HashMap<String, Any?>()

        body["fetch_token"] = purchaseToken
        body["product_id"] = productID
        body["app_user_id"] = appUserID
        body["is_restore"] = isRestore

        enqueue(object : PurchaserInfoReceivingCall(handler) {
            @Throws(HTTPClient.HTTPErrorException::class)
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest("/receipts", body, authenticationHeaders)
            }
        })
    }

    fun getEntitlements(appUserID: String, handler: EntitlementsResponseHandler) {
        enqueue(object : Dispatcher.AsyncCall() {
            @Throws(HTTPClient.HTTPErrorException::class)
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/" + encode(appUserID) + "/products",
                    null as Map<*, *>?,
                    authenticationHeaders
                )
            }

            internal override fun onError(code: Int, message: String) {
                handler.onError(code, message)
            }

            internal override fun onCompletion(result: HTTPClient.Result) {
                if (result.responseCode < 300) {
                    try {
                        val entitlementsResponse = result.body!!.getJSONObject("entitlements")
                        val entitlementMap = entitlementFactory.build(entitlementsResponse)
                        handler.onReceiveEntitlements(entitlementMap)
                    } catch (e: JSONException) {
                        handler.onError(
                            result.responseCode,
                            "Error parsing products JSON " + e.localizedMessage
                        )
                    }

                } else {
                    handler.onError(result.responseCode, "Backend error")
                }
            }
        })
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
            @Throws(HTTPClient.HTTPErrorException::class)
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/" + encode(appUserID) + "/attribution",
                    body,
                    authenticationHeaders
                )
            }
        })
    }

    fun alias(
        appUserID: String,
        newAppUserID: String,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (Int, String) -> Unit
    ) {
        val body = mapOf(
            "new_app_user_id" to newAppUserID
        )

        enqueue(object : Dispatcher.AsyncCall() {
            @Throws(HTTPClient.HTTPErrorException::class)
            override fun call(): HTTPClient.Result {
                return httpClient.performRequest(
                    "/subscribers/" + encode(appUserID) + "/alias",
                    body,
                    authenticationHeaders
                )
            }

            override fun onError(code: Int, message: String) {
                onErrorHandler(code, message)
            }

            override fun onCompletion(result: HTTPClient.Result) {
                if (result.responseCode < 300) {
                    try {
                        onSuccessHandler()
                    } catch (e: JSONException) {
                        onErrorHandler(result.responseCode, "Backend error")
                    }
                } else {
                    onErrorHandler(result.responseCode, "Backend error")
                }
            }
        })
    }
}
