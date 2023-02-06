//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.net.Uri
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.strings.NetworkStrings
import com.revenuecat.purchases.utils.filterNotNullValues
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URL

const val ATTRIBUTES_ERROR_RESPONSE_KEY = "attributes_error_response"
const val ATTRIBUTE_ERRORS_KEY = "attribute_errors"

/** @suppress */
internal typealias CustomerInfoCallback = Pair<(CustomerInfo) -> Unit, (PurchasesError) -> Unit>

/** @suppress */
typealias PostReceiptCallback = Pair<PostReceiptDataSuccessCallback, PostReceiptDataErrorCallback>
/** @suppress */
typealias CallbackCacheKey = List<String>
/** @suppress */
typealias OfferingsCallback = Pair<(JSONObject) -> Unit, (PurchasesError) -> Unit>
/** @suppress */
typealias PostReceiptDataSuccessCallback = (CustomerInfo, body: JSONObject) -> Unit
/** @suppress */
typealias PostReceiptDataErrorCallback = (PurchasesError, shouldConsumePurchase: Boolean, body: JSONObject?) -> Unit
/** @suppress */
typealias IdentifyCallback = Pair<(CustomerInfo, Boolean) -> Unit, (PurchasesError) -> Unit>
/** @suppress */
typealias TelemetryCallback = Pair<(JSONObject) -> Unit, (PurchasesError) -> Unit>

class Backend(
    private val apiKey: String,
    private val appConfig: AppConfig,
    private val dispatcher: Dispatcher,
    private val diagnosticsDispatcher: Dispatcher,
    private val httpClient: HTTPClient
) {

    internal val authenticationHeaders = mapOf("Authorization" to "Bearer ${this.apiKey}")

    @get:Synchronized @set:Synchronized
    @Volatile var callbacks = mutableMapOf<CallbackCacheKey, MutableList<CustomerInfoCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var postReceiptCallbacks = mutableMapOf<CallbackCacheKey, MutableList<PostReceiptCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var offeringsCallbacks = mutableMapOf<String, MutableList<OfferingsCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var identifyCallbacks = mutableMapOf<CallbackCacheKey, MutableList<IdentifyCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var telemetryCallbacks = mutableMapOf<CallbackCacheKey, MutableList<TelemetryCallback>>()

    fun close() {
        this.dispatcher.close()
    }

    fun performRequest(
        path: String,
        body: Map<String, Any?>?,
        onError: (PurchasesError) -> Unit,
        onCompleted: (PurchasesError?, Int, JSONObject) -> Unit
    ) {
        enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    path,
                    body,
                    authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                onError(error)
            }

            override fun onCompletion(result: HTTPResult) {
                val error = if (!result.isSuccessful()) {
                    result.toPurchasesError().also { errorLog(it) }
                } else {
                    null
                }
                onCompleted(error, result.responseCode, result.body)
            }
        }, dispatcher)
    }

    fun getCustomerInfo(
        appUserID: String,
        appInBackground: Boolean,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val path = "/subscribers/" + encode(appUserID)
        val cacheKey = synchronized(this@Backend) {
            // If there is any enqueued `postReceiptData` we don't want this new
            // `getCustomerInfo` to share the same cache key.
            // If it did, future `getCustomerInfo` would receive a cached value
            // instead of an up-to-date `CustomerInfo` after those post receipt operations finish.
            if (postReceiptCallbacks.isEmpty()) {
                listOf(path)
            } else {
                listOf(path) + "${callbacks.count()}"
            }
        }
        val call = object : Dispatcher.AsyncCall() {

            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    path,
                    null,
                    authenticationHeaders
                )
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    callbacks.remove(cacheKey)
                }?.forEach { (onSuccess, onError) ->
                    try {
                        if (result.isSuccessful()) {
                            onSuccess(result.body.buildCustomerInfo())
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
            callbacks.addCallback(call, dispatcher, cacheKey, onSuccess to onError, randomDelay = appInBackground)
        }
    }

    @SuppressWarnings("LongParameterList", "ForbiddenComment")
    fun postReceiptData(
        purchaseToken: String,
        appUserID: String,
        isRestore: Boolean,
        observerMode: Boolean,
        subscriberAttributes: Map<String, Map<String, Any?>>,
        receiptInfo: ReceiptInfo,
        storeAppUserID: String?,
        marketplace: String? = null,
        onSuccess: PostReceiptDataSuccessCallback,
        onError: PostReceiptDataErrorCallback
    ) {
        val cacheKey = listOfNotNull(
            purchaseToken,
            appUserID,
            isRestore.toString(),
            observerMode.toString(),
            subscriberAttributes.toString(),
            receiptInfo.toString(),
            storeAppUserID
        )

        val body = mapOf(
            "fetch_token" to purchaseToken,
            "product_ids" to receiptInfo.productIDs,
            "app_user_id" to appUserID,
            "is_restore" to isRestore,
            "presented_offering_identifier" to receiptInfo.offeringIdentifier,
            "observer_mode" to observerMode,
            "price" to receiptInfo.price,
            "currency" to receiptInfo.currency,
            "attributes" to subscriberAttributes.takeUnless { it.isEmpty() },
            "normal_duration" to receiptInfo.duration,
            "intro_duration" to receiptInfo.introDuration,
            "trial_duration" to receiptInfo.trialDuration,
            "store_user_id" to storeAppUserID
        ).filterValues { value -> value != null }

        val extraHeaders = receiptInfo.storeProduct?.price?.let { priceString ->
            mapOf("price_string" to priceString, "marketplace" to marketplace).filterNotNullValues()
        } ?: mapOf()

        val call = object : Dispatcher.AsyncCall() {

            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    "/receipts",
                    body,
                    authenticationHeaders + extraHeaders
                )
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    postReceiptCallbacks.remove(cacheKey)
                }?.forEach { (onSuccess, onError) ->
                    try {
                        if (result.isSuccessful()) {
                            onSuccess(result.body.buildCustomerInfo(), result.body)
                        } else {
                            val purchasesError = result.toPurchasesError().also { errorLog(it) }
                            onError(
                                purchasesError,
                                result.responseCode < RCHTTPStatusCodes.ERROR &&
                                    purchasesError.code != PurchasesErrorCode.UnsupportedError,
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
            postReceiptCallbacks.addCallback(call, dispatcher, cacheKey, onSuccess to onError)
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
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
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

            override fun onCompletion(result: HTTPResult) {
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
            offeringsCallbacks.addCallback(call, dispatcher, path, onSuccess to onError, randomDelay = appInBackground)
        }
    }

    private fun encode(string: String): String {
        return Uri.encode(string)
    }

    fun logIn(
        appUserID: String,
        newAppUserID: String,
        onSuccessHandler: (CustomerInfo, Boolean) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        val cacheKey = listOfNotNull(
            appUserID,
            newAppUserID
        )
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    "/subscribers/identify",
                    mapOf(
                        "new_app_user_id" to newAppUserID,
                        "app_user_id" to appUserID
                    ),
                    authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    identifyCallbacks.remove(cacheKey)
                }?.forEach { (_, onErrorHandler) ->
                    onErrorHandler(error)
                }
            }

            override fun onCompletion(result: HTTPResult) {
                if (result.isSuccessful()) {
                    synchronized(this@Backend) {
                        identifyCallbacks.remove(cacheKey)
                    }?.forEach { (onSuccessHandler, onErrorHandler) ->
                        val created = result.responseCode == RCHTTPStatusCodes.CREATED
                        if (result.body.length() > 0) {
                            val customerInfo = result.body.buildCustomerInfo()
                            onSuccessHandler(customerInfo, created)
                        } else {
                            onErrorHandler(PurchasesError(PurchasesErrorCode.UnknownError)
                                .also { errorLog(it) })
                        }
                    }
                } else {
                    onError(result.toPurchasesError().also { errorLog(it) })
                }
            }
        }
        synchronized(this@Backend) {
            identifyCallbacks.addCallback(call, dispatcher, cacheKey, onSuccessHandler to onErrorHandler)
        }
    }

    fun postDiagnostics(
        diagnosticsList: List<JSONObject>,
        onSuccessHandler: (JSONObject) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        val cacheKey = diagnosticsList.map { it.hashCode().toString() }

        val body = mapOf("entries" to JSONArray(diagnosticsList))
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    URL("https://api-telemetry.revenuecat.com/"),
                    "/telemetry",
                    body,
                    authenticationHeaders,
                    gzipRequest = true
                )
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    telemetryCallbacks.remove(cacheKey)
                }?.forEach { (_, onErrorHandler) ->
                    onErrorHandler(error)
                }
            }

            override fun onCompletion(result: HTTPResult) {
                if (result.isSuccessful()) {
                    synchronized(this@Backend) {
                        telemetryCallbacks.remove(cacheKey)
                    }?.forEach { (onSuccessHandler, onErrorHandler) ->
                        try {
                            onSuccessHandler(result.body)
                        } catch (e: JSONException) {
                            onErrorHandler(e.toPurchasesError())
                        }
                    }
                } else {
                    onError(result.toPurchasesError())
                }
            }
        }
        synchronized(this@Backend) {
            telemetryCallbacks.addCallback(call, diagnosticsDispatcher, cacheKey, onSuccessHandler to onErrorHandler)
        }
    }

    fun clearCaches() {
        httpClient.clearCaches()
    }

    private fun HTTPResult.isSuccessful(): Boolean {
        return responseCode < RCHTTPStatusCodes.UNSUCCESSFUL
    }

    private fun enqueue(
        call: Dispatcher.AsyncCall,
        dispatcher: Dispatcher,
        randomDelay: Boolean = false
    ) {
        if (!dispatcher.isClosed()) {
            dispatcher.enqueue(call, randomDelay)
        }
    }

    private fun <K, S, E> MutableMap<K, MutableList<Pair<S, E>>>.addCallback(
        call: Dispatcher.AsyncCall,
        dispatcher: Dispatcher,
        cacheKey: K,
        functions: Pair<S, E>,
        randomDelay: Boolean = false
    ) {
        if (!containsKey(cacheKey)) {
            this[cacheKey] = mutableListOf(functions)
            enqueue(call, dispatcher, randomDelay)
        } else {
            debugLog(String.format(NetworkStrings.SAME_CALL_ALREADY_IN_PROGRESS, cacheKey))
            this[cacheKey]!!.add(functions)
        }
    }
}
