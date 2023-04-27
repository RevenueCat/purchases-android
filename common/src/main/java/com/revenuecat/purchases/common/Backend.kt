//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.strings.NetworkStrings
import com.revenuecat.purchases.utils.filterNotNullValues
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

const val ATTRIBUTES_ERROR_RESPONSE_KEY = "attributes_error_response"
const val ATTRIBUTE_ERRORS_KEY = "attribute_errors"

/** @suppress */
internal typealias CustomerInfoCallback = Pair<(CustomerInfo) -> Unit, (PurchasesError, isServerError: Boolean) -> Unit>

/** @suppress */
typealias PostReceiptCallback = Pair<PostReceiptDataSuccessCallback, PostReceiptDataErrorCallback>
/** @suppress */
typealias CallbackCacheKey = List<String>
/** @suppress */
typealias OfferingsCallback = Pair<(JSONObject) -> Unit, (PurchasesError) -> Unit>
/** @suppress */
typealias PostReceiptDataSuccessCallback = (CustomerInfo, body: JSONObject) -> Unit
/** @suppress */
typealias PostReceiptDataErrorCallback = (
    PurchasesError,
    postReceiptErrorHandlingBehavior: PostReceiptErrorHandlingBehavior,
    body: JSONObject?
) -> Unit
/** @suppress */
typealias IdentifyCallback = Pair<(CustomerInfo, Boolean) -> Unit, (PurchasesError) -> Unit>
/** @suppress */
typealias DiagnosticsCallback = Pair<(JSONObject) -> Unit, (PurchasesError, Boolean) -> Unit>
/** @suppress */
typealias ProductEntitlementCallback = Pair<(ProductEntitlementMapping) -> Unit, (PurchasesError) -> Unit>

enum class PostReceiptErrorHandlingBehavior {
    SHOULD_BE_CONSUMED,
    SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME,
    SHOULD_NOT_CONSUME
}

class Backend(
    private val appConfig: AppConfig,
    private val dispatcher: Dispatcher,
    private val diagnosticsDispatcher: Dispatcher,
    private val httpClient: HTTPClient,
    private val backendHelper: BackendHelper
) {

    val verificationMode: SignatureVerificationMode
        get() = httpClient.signingManager.signatureVerificationMode

    @get:Synchronized @set:Synchronized
    @Volatile var callbacks = mutableMapOf<CallbackCacheKey, MutableList<CustomerInfoCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var postReceiptCallbacks = mutableMapOf<CallbackCacheKey, MutableList<PostReceiptCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var offeringsCallbacks = mutableMapOf<String, MutableList<OfferingsCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var identifyCallbacks = mutableMapOf<CallbackCacheKey, MutableList<IdentifyCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var diagnosticsCallbacks = mutableMapOf<CallbackCacheKey, MutableList<DiagnosticsCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var productEntitlementCallbacks = mutableMapOf<String, MutableList<ProductEntitlementCallback>>()

    fun close() {
        this.dispatcher.close()
    }

    fun getCustomerInfo(
        appUserID: String,
        appInBackground: Boolean,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError, isServerError: Boolean) -> Unit
    ) {
        val endpoint = Endpoint.GetCustomerInfo(appUserID)
        val path = endpoint.getPath()
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
                    endpoint,
                    null,
                    backendHelper.authenticationHeaders
                )
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    callbacks.remove(cacheKey)
                }?.forEach { (onSuccess, onError) ->
                    try {
                        if (result.isSuccessful()) {
                            onSuccess(CustomerInfoFactory.buildCustomerInfo(result))
                        } else {
                            onError(
                                result.toPurchasesError().also { errorLog(it) },
                                RCHTTPStatusCodes.isServerError(result.responseCode)
                            )
                        }
                    } catch (e: JSONException) {
                        val isServerError = false
                        onError(e.toPurchasesError().also { errorLog(it) }, isServerError)
                    }
                }
            }

            override fun onError(error: PurchasesError) {
                val isServerError = false
                synchronized(this@Backend) {
                    callbacks.remove(cacheKey)
                }?.forEach { (_, onError) ->
                    onError(error, isServerError)
                }
            }
        }
        synchronized(this@Backend) {
            val delay = if (appInBackground) Delay.DEFAULT else Delay.NONE
            callbacks.addCallback(call, dispatcher, cacheKey, onSuccess to onError, delay)
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
        @SuppressWarnings("UnusedPrivateMember")
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
            "platform_product_ids" to receiptInfo.platformProductIds?.map { it.asMap },
            "app_user_id" to appUserID,
            "is_restore" to isRestore,
            "presented_offering_identifier" to receiptInfo.offeringIdentifier,
            "observer_mode" to observerMode,
            "price" to receiptInfo.price,
            "currency" to receiptInfo.currency,
            "attributes" to subscriberAttributes.takeUnless { it.isEmpty() },
            "normal_duration" to receiptInfo.duration,
            "store_user_id" to storeAppUserID,
            "pricing_phases" to receiptInfo.pricingPhases?.map { it.toMap() },
            "proration_mode" to receiptInfo.prorationMode?.name
        ).filterNotNullValues()

        val extraHeaders = mapOf(
            "price_string" to receiptInfo.storeProduct?.price?.formatted,
            "marketplace" to marketplace
        ).filterNotNullValues()

        val call = object : Dispatcher.AsyncCall() {

            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    Endpoint.PostReceipt,
                    body,
                    backendHelper.authenticationHeaders + extraHeaders
                )
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    postReceiptCallbacks.remove(cacheKey)
                }?.forEach { (onSuccess, onError) ->
                    try {
                        if (result.isSuccessful()) {
                            onSuccess(CustomerInfoFactory.buildCustomerInfo(result), result.body)
                        } else {
                            val purchasesError = result.toPurchasesError().also { errorLog(it) }
                            val errorHandlingBehavior = determinePostReceiptErrorHandlingBehavior(
                                result.responseCode, purchasesError
                            )
                            onError(
                                purchasesError,
                                errorHandlingBehavior,
                                result.body
                            )
                        }
                    } catch (e: JSONException) {
                        onError(
                            e.toPurchasesError().also { errorLog(it) },
                            PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
                            null
                        )
                    }
                }
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    postReceiptCallbacks.remove(cacheKey)
                }?.forEach { (_, onError) ->
                    onError(
                        error,
                        PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
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
        val endpoint = Endpoint.GetOfferings(appUserID)
        val path = endpoint.getPath()
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    endpoint,
                    null,
                    backendHelper.authenticationHeaders
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
            val delay = if (appInBackground) Delay.DEFAULT else Delay.NONE
            offeringsCallbacks.addCallback(call, dispatcher, path, onSuccess to onError, delay)
        }
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
                    Endpoint.LogIn,
                    mapOf(
                        "new_app_user_id" to newAppUserID,
                        "app_user_id" to appUserID
                    ),
                    backendHelper.authenticationHeaders
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
                            val customerInfo = CustomerInfoFactory.buildCustomerInfo(result)
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
        onErrorHandler: (PurchasesError, Boolean) -> Unit
    ) {
        val cacheKey = diagnosticsList.map { it.hashCode().toString() }

        val body = mapOf("entries" to JSONArray(diagnosticsList))
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.diagnosticsURL,
                    Endpoint.PostDiagnostics,
                    body,
                    backendHelper.authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    diagnosticsCallbacks.remove(cacheKey)
                }?.forEach { (_, onErrorHandler) ->
                    onErrorHandler(error, error.code == PurchasesErrorCode.NetworkError)
                }
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    diagnosticsCallbacks.remove(cacheKey)
                }?.forEach { (onSuccessHandler, onErrorHandler) ->
                    if (result.isSuccessful()) {
                        onSuccessHandler(result.body)
                    } else {
                        val error = result.toPurchasesError()
                        val shouldRetry = RCHTTPStatusCodes.isServerError(result.responseCode) ||
                            error.code == PurchasesErrorCode.NetworkError
                        onErrorHandler(error, shouldRetry)
                    }
                }
            }
        }
        synchronized(this@Backend) {
            diagnosticsCallbacks.addCallback(
                call,
                diagnosticsDispatcher,
                cacheKey,
                onSuccessHandler to onErrorHandler,
                Delay.LONG
            )
        }
    }

    fun getProductEntitlementMapping(
        onSuccessHandler: (ProductEntitlementMapping) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        val endpoint = Endpoint.GetProductEntitlementMapping
        val path = endpoint.getPath()
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    endpoint,
                    null,
                    backendHelper.authenticationHeaders
                )
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    productEntitlementCallbacks.remove(path)
                }?.forEach { (_, onError) ->
                    onError(error)
                }
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    productEntitlementCallbacks.remove(path)
                }?.forEach { (onSuccess, onError) ->
                    if (result.isSuccessful()) {
                        try {
                            onSuccess(ProductEntitlementMapping.fromJson(result.body))
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
            productEntitlementCallbacks.addCallback(
                call,
                dispatcher,
                path,
                onSuccessHandler to onErrorHandler,
                Delay.DEFAULT
            )
        }
    }

    fun clearCaches() {
        httpClient.clearCaches()
    }

    private fun determinePostReceiptErrorHandlingBehavior(
        responseCode: Int,
        purchasesError: PurchasesError
    ) = if (RCHTTPStatusCodes.isServerError(responseCode)) {
        PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME
    } else if (purchasesError.code == PurchasesErrorCode.UnsupportedError) {
        PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME
    } else {
        PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED
    }

    private fun <K, S, E> MutableMap<K, MutableList<Pair<S, E>>>.addCallback(
        call: Dispatcher.AsyncCall,
        dispatcher: Dispatcher,
        cacheKey: K,
        functions: Pair<S, E>,
        delay: Delay = Delay.NONE
    ) {
        if (!containsKey(cacheKey)) {
            this[cacheKey] = mutableListOf(functions)
            backendHelper.enqueue(call, dispatcher, delay)
        } else {
            debugLog(String.format(NetworkStrings.SAME_CALL_ALREADY_IN_PROGRESS, cacheKey))
            this[cacheKey]!!.add(functions)
        }
    }
}

internal fun PricingPhase.toMap(): Map<String, Any?> {
    return mapOf(
        "billingPeriod" to this.billingPeriod.iso8601,
        "billingCycleCount" to this.billingCycleCount,
        "recurrenceMode" to this.recurrenceMode.identifier,
        "formattedPrice" to this.price.formatted,
        "priceAmountMicros" to this.price.amountMicros,
        "priceCurrencyCode" to this.price.currencyCode,
    )
}
