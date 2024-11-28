//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.PostReceiptResponse
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.networking.buildPostReceiptResponse
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterRoot
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.paywalls.events.PaywallEventRequest
import com.revenuecat.purchases.paywalls.events.PaywallPostReceiptData
import com.revenuecat.purchases.strings.NetworkStrings
import com.revenuecat.purchases.utils.asMap
import com.revenuecat.purchases.utils.filterNotNullValues
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal const val ATTRIBUTES_ERROR_RESPONSE_KEY = "attributes_error_response"
internal const val ATTRIBUTE_ERRORS_KEY = "attribute_errors"

/** @suppress */
internal typealias CustomerInfoCallback = Pair<(CustomerInfo) -> Unit, (PurchasesError, isServerError: Boolean) -> Unit>

/** @suppress */
internal typealias PostReceiptCallback = Pair<PostReceiptDataSuccessCallback, PostReceiptDataErrorCallback>

/** @suppress */
internal typealias CallbackCacheKey = List<String>

/** @suppress */
internal typealias OfferingsCallback = Pair<(JSONObject) -> Unit, (PurchasesError, isServerError: Boolean) -> Unit>

/** @suppress */
internal typealias PostReceiptDataSuccessCallback = (PostReceiptResponse) -> Unit

/** @suppress */
internal typealias PostReceiptDataErrorCallback = (
    PurchasesError,
    postReceiptErrorHandlingBehavior: PostReceiptErrorHandlingBehavior,
    body: JSONObject?,
) -> Unit

/** @suppress */
internal typealias IdentifyCallback = Pair<(CustomerInfo, Boolean) -> Unit, (PurchasesError) -> Unit>

/** @suppress */
internal typealias DiagnosticsCallback = Pair<(JSONObject) -> Unit, (PurchasesError, Boolean) -> Unit>

/** @suppress */
internal typealias PaywallEventsCallback = Pair<() -> Unit, (PurchasesError, Boolean) -> Unit>

/** @suppress */
internal typealias ProductEntitlementCallback = Pair<(ProductEntitlementMapping) -> Unit, (PurchasesError) -> Unit>

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal typealias CustomerCenterCallback = Pair<(CustomerCenterConfigData) -> Unit, (PurchasesError) -> Unit>

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal typealias RedeemWebPurchaseCallback = (RedeemWebPurchaseListener.Result) -> Unit

internal enum class PostReceiptErrorHandlingBehavior {
    SHOULD_BE_MARKED_SYNCED,
    SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME,
    SHOULD_NOT_CONSUME,
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("TooManyFunctions")
internal class Backend(
    private val appConfig: AppConfig,
    private val dispatcher: Dispatcher,
    private val eventsDispatcher: Dispatcher,
    private val httpClient: HTTPClient,
    private val backendHelper: BackendHelper,
) {
    companion object {
        private const val APP_USER_ID = "app_user_id"
        private const val FETCH_TOKEN = "fetch_token"
        private const val NEW_APP_USER_ID = "new_app_user_id"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal val json = Json {
            ignoreUnknownKeys = true
        }
    }

    val verificationMode: SignatureVerificationMode
        get() = httpClient.signingManager.signatureVerificationMode

    @get:Synchronized @set:Synchronized
    @Volatile var callbacks = mutableMapOf<BackgroundAwareCallbackCacheKey, MutableList<CustomerInfoCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var postReceiptCallbacks = mutableMapOf<CallbackCacheKey, MutableList<PostReceiptCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var offeringsCallbacks = mutableMapOf<BackgroundAwareCallbackCacheKey, MutableList<OfferingsCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var identifyCallbacks = mutableMapOf<CallbackCacheKey, MutableList<IdentifyCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var diagnosticsCallbacks = mutableMapOf<CallbackCacheKey, MutableList<DiagnosticsCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var paywallEventsCallbacks = mutableMapOf<CallbackCacheKey, MutableList<PaywallEventsCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var productEntitlementCallbacks = mutableMapOf<String, MutableList<ProductEntitlementCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var customerCenterCallbacks = mutableMapOf<String, MutableList<CustomerCenterCallback>>()

    @get:Synchronized @set:Synchronized
    @Volatile var redeemWebPurchaseCallbacks = mutableMapOf<String, MutableList<RedeemWebPurchaseCallback>>()

    fun close() {
        this.dispatcher.close()
    }

    fun getCustomerInfo(
        appUserID: String,
        appInBackground: Boolean,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError, isServerError: Boolean) -> Unit,
    ) {
        val endpoint = Endpoint.GetCustomerInfo(appUserID)
        val path = endpoint.getPath()
        val cacheKey = synchronized(this@Backend) {
            // If there is any enqueued `postReceiptData` we don't want this new
            // `getCustomerInfo` to share the same cache key.
            // If it did, future `getCustomerInfo` would receive a cached value
            // instead of an up-to-date `CustomerInfo` after those post receipt operations finish.
            if (postReceiptCallbacks.isEmpty()) {
                BackgroundAwareCallbackCacheKey(listOf(path), appInBackground)
            } else {
                BackgroundAwareCallbackCacheKey(listOf(path) + "${callbacks.count()}", appInBackground)
            }
        }
        val call = object : Dispatcher.AsyncCall() {

            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    endpoint,
                    body = null,
                    postFieldsToSign = null,
                    backendHelper.authenticationHeaders,
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
                                RCHTTPStatusCodes.isServerError(result.responseCode),
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
            callbacks.addBackgroundAwareCallback(call, dispatcher, cacheKey, onSuccess to onError, delay)
        }
    }

    @SuppressWarnings("LongParameterList", "ForbiddenComment")
    fun postReceiptData(
        purchaseToken: String,
        appUserID: String,
        isRestore: Boolean,
        finishTransactions: Boolean,
        subscriberAttributes: Map<String, Map<String, Any?>>,
        receiptInfo: ReceiptInfo,
        storeAppUserID: String?,
        @SuppressWarnings("UnusedPrivateMember")
        marketplace: String? = null,
        initiationSource: PostReceiptInitiationSource,
        paywallPostReceiptData: PaywallPostReceiptData?,
        onSuccess: PostReceiptDataSuccessCallback,
        onError: PostReceiptDataErrorCallback,
    ) {
        val cacheKey = listOfNotNull(
            purchaseToken,
            appUserID,
            isRestore.toString(),
            finishTransactions.toString(),
            subscriberAttributes.toString(),
            receiptInfo.toString(),
            storeAppUserID,
        )

        val body = mapOf(
            FETCH_TOKEN to purchaseToken,
            "product_ids" to receiptInfo.productIDs,
            "platform_product_ids" to receiptInfo.platformProductIds?.map { it.asMap },
            APP_USER_ID to appUserID,
            "is_restore" to isRestore,
            "presented_offering_identifier" to receiptInfo.presentedOfferingContext?.offeringIdentifier,
            "presented_placement_identifier" to receiptInfo.presentedOfferingContext?.placementIdentifier,
            "applied_targeting_rule" to receiptInfo.presentedOfferingContext?.targetingContext?.let {
                return@let mapOf("revision" to it.revision, "rule_id" to it.ruleId)
            },
            "observer_mode" to !finishTransactions,
            "price" to receiptInfo.price,
            "currency" to receiptInfo.currency,
            "attributes" to subscriberAttributes.takeUnless { it.isEmpty() || appConfig.customEntitlementComputation },
            "normal_duration" to receiptInfo.duration,
            "store_user_id" to storeAppUserID,
            "pricing_phases" to receiptInfo.pricingPhases?.map { it.toMap() },
            "proration_mode" to (receiptInfo.replacementMode as? GoogleReplacementMode)?.asLegacyProrationMode?.name,
            "initiation_source" to initiationSource.postReceiptFieldValue,
            "paywall" to paywallPostReceiptData?.toMap(),
        ).filterNotNullValues()

        val postFieldsToSign = listOf(
            APP_USER_ID to appUserID,
            FETCH_TOKEN to purchaseToken,
        )

        val extraHeaders = mapOf(
            "price_string" to receiptInfo.storeProduct?.price?.formatted,
            "marketplace" to marketplace,
        ).filterNotNullValues()

        val call = object : Dispatcher.AsyncCall() {

            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    Endpoint.PostReceipt,
                    body,
                    postFieldsToSign,
                    backendHelper.authenticationHeaders + extraHeaders,
                )
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    postReceiptCallbacks.remove(cacheKey)
                }?.forEach { (onSuccess, onError) ->
                    try {
                        if (result.isSuccessful()) {
                            onSuccess(buildPostReceiptResponse(result))
                        } else {
                            val purchasesError = result.toPurchasesError().also { errorLog(it) }
                            val errorHandlingBehavior = determinePostReceiptErrorHandlingBehavior(
                                result.responseCode,
                                purchasesError,
                            )
                            onError(
                                purchasesError,
                                errorHandlingBehavior,
                                result.body,
                            )
                        }
                    } catch (e: JSONException) {
                        onError(
                            e.toPurchasesError().also { errorLog(it) },
                            PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
                            null,
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
                        null,
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
        onError: (PurchasesError, isServerError: Boolean) -> Unit,
    ) {
        val endpoint = Endpoint.GetOfferings(appUserID)
        val path = endpoint.getPath()
        val cacheKey = BackgroundAwareCallbackCacheKey(listOf(path), appInBackground)
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    endpoint,
                    body = null,
                    postFieldsToSign = null,
                    backendHelper.authenticationHeaders,
                )
            }

            override fun onError(error: PurchasesError) {
                val isServerError = false
                synchronized(this@Backend) {
                    offeringsCallbacks.remove(cacheKey)
                }?.forEach { (_, onError) ->
                    onError(error, isServerError)
                }
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    offeringsCallbacks.remove(cacheKey)
                }?.forEach { (onSuccess, onError) ->
                    if (result.isSuccessful()) {
                        try {
                            onSuccess(result.body)
                        } catch (e: JSONException) {
                            val isServerError = false
                            onError(e.toPurchasesError().also { errorLog(it) }, isServerError)
                        }
                    } else {
                        onError(
                            result.toPurchasesError().also { errorLog(it) },
                            RCHTTPStatusCodes.isServerError(result.responseCode),
                        )
                    }
                }
            }
        }
        synchronized(this@Backend) {
            val delay = if (appInBackground) Delay.DEFAULT else Delay.NONE
            offeringsCallbacks.addBackgroundAwareCallback(call, dispatcher, cacheKey, onSuccess to onError, delay)
        }
    }

    fun logIn(
        appUserID: String,
        newAppUserID: String,
        onSuccessHandler: (CustomerInfo, Boolean) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit,
    ) {
        val cacheKey = listOfNotNull(
            appUserID,
            newAppUserID,
        )
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                val body = mapOf(
                    APP_USER_ID to appUserID,
                    NEW_APP_USER_ID to newAppUserID,
                )
                val postFieldsToSign = listOf(
                    APP_USER_ID to appUserID,
                    NEW_APP_USER_ID to newAppUserID,
                )
                return httpClient.performRequest(
                    appConfig.baseURL,
                    Endpoint.LogIn,
                    body,
                    postFieldsToSign,
                    backendHelper.authenticationHeaders,
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
                            onErrorHandler(
                                PurchasesError(PurchasesErrorCode.UnknownError)
                                    .also { errorLog(it) },
                            )
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
        onErrorHandler: (PurchasesError, Boolean) -> Unit,
    ) {
        val cacheKey = diagnosticsList.map { it.hashCode().toString() }

        val body = mapOf("entries" to JSONArray(diagnosticsList))
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    AppConfig.diagnosticsURL,
                    Endpoint.PostDiagnostics,
                    body,
                    postFieldsToSign = null,
                    backendHelper.authenticationHeaders,
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
                eventsDispatcher,
                cacheKey,
                onSuccessHandler to onErrorHandler,
                Delay.LONG,
            )
        }
    }

    fun postPaywallEvents(
        paywallEventRequest: PaywallEventRequest,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (error: PurchasesError, shouldMarkAsSynced: Boolean) -> Unit,
    ) {
        val body = PaywallEventRequest.json.encodeToJsonElement(paywallEventRequest).asMap()
        if (body == null) {
            onErrorHandler(
                PurchasesError(
                    PurchasesErrorCode.UnknownError,
                    "Error encoding paywall event request",
                ).also { errorLog(it) },
                true,
            )
            return
        }
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    AppConfig.paywallEventsURL,
                    Endpoint.PostPaywallEvents,
                    body,
                    postFieldsToSign = null,
                    backendHelper.authenticationHeaders,
                )
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    paywallEventsCallbacks.remove(paywallEventRequest.cacheKey)
                }?.forEach { (_, onErrorHandler) ->
                    onErrorHandler(error, false)
                }
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    paywallEventsCallbacks.remove(paywallEventRequest.cacheKey)
                }?.forEach { (onSuccessHandler, onErrorHandler) ->
                    if (result.isSuccessful()) {
                        onSuccessHandler()
                    } else {
                        onErrorHandler(result.toPurchasesError(), RCHTTPStatusCodes.isSynced(result.responseCode))
                    }
                }
            }
        }
        synchronized(this@Backend) {
            paywallEventsCallbacks.addCallback(
                call,
                eventsDispatcher,
                paywallEventRequest.cacheKey,
                onSuccessHandler to onErrorHandler,
                Delay.LONG,
            )
        }
    }

    fun getProductEntitlementMapping(
        onSuccessHandler: (ProductEntitlementMapping) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit,
    ) {
        val endpoint = Endpoint.GetProductEntitlementMapping
        val path = endpoint.getPath()
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    endpoint,
                    body = null,
                    postFieldsToSign = null,
                    backendHelper.authenticationHeaders,
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
                Delay.LONG,
            )
        }
    }

    fun getCustomerCenterConfig(
        appUserID: String,
        onSuccessHandler: (CustomerCenterConfigData) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit,
    ) {
        val endpoint = Endpoint.GetCustomerCenterConfig(appUserID)
        val path = endpoint.getPath()
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    endpoint,
                    body = null,
                    postFieldsToSign = null,
                    backendHelper.authenticationHeaders,
                )
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    customerCenterCallbacks.remove(path)
                }?.forEach { (_, onErrorHandler) ->
                    onErrorHandler(error)
                }
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    customerCenterCallbacks.remove(path)
                }?.forEach { (onSuccessHandler, onErrorHandler) ->
                    if (result.isSuccessful()) {
                        try {
                            val customerCenterRoot = json.decodeFromString<CustomerCenterRoot>(
                                result.payload,
                            )
                            onSuccessHandler(customerCenterRoot.customerCenter)
                        } catch (e: SerializationException) {
                            onErrorHandler(e.toPurchasesError().also { errorLog(it) })
                        } catch (e: IllegalArgumentException) {
                            onErrorHandler(e.toPurchasesError().also { errorLog(it) })
                        }
                    } else {
                        onErrorHandler(result.toPurchasesError().also { errorLog(it) })
                    }
                }
            }
        }
        synchronized(this@Backend) {
            customerCenterCallbacks.addCallback(
                call,
                dispatcher,
                path,
                onSuccessHandler to onErrorHandler,
                Delay.NONE,
            )
        }
    }

    @Suppress("NestedBlockDepth")
    fun postRedeemWebPurchase(
        appUserID: String,
        redemptionToken: String,
        onResultHandler: (RedeemWebPurchaseListener.Result) -> Unit,
    ) {
        val endpoint = Endpoint.PostRedeemWebPurchase
        val path = endpoint.getPath()
        val body = mapOf("redemption_token" to redemptionToken, APP_USER_ID to appUserID)
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    appConfig.baseURL,
                    endpoint,
                    body,
                    postFieldsToSign = null,
                    backendHelper.authenticationHeaders,
                )
            }

            override fun onError(error: PurchasesError) {
                synchronized(this@Backend) {
                    redeemWebPurchaseCallbacks.remove(path)
                }?.forEach { callback ->
                    callback(RedeemWebPurchaseListener.Result.Error(error))
                }
            }

            override fun onCompletion(result: HTTPResult) {
                synchronized(this@Backend) {
                    redeemWebPurchaseCallbacks.remove(path)
                }?.forEach { callback ->
                    if (result.isSuccessful()) {
                        callback(
                            RedeemWebPurchaseListener.Result.Success(CustomerInfoFactory.buildCustomerInfo(result)),
                        )
                    } else {
                        when (result.backendErrorCode) {
                            BackendErrorCode.BackendInvalidWebRedemptionToken.value -> {
                                callback(RedeemWebPurchaseListener.Result.InvalidToken)
                            }
                            BackendErrorCode.BackendExpiredWebRedemptionToken.value -> {
                                val resultBody = result.body
                                val redemptionError = resultBody.optJSONObject("purchase_redemption_error_info")
                                val obfuscatedEmail = redemptionError?.optString("obfuscated_email")
                                if (obfuscatedEmail == null) {
                                    errorLog("Error parsing expired redemption token response: $resultBody")
                                    callback(RedeemWebPurchaseListener.Result.Error(result.toPurchasesError()))
                                } else {
                                    callback(RedeemWebPurchaseListener.Result.Expired(obfuscatedEmail))
                                }
                            }
                            BackendErrorCode.BackendWebPurchaseAlreadyRedeemed.value -> {
                                callback(RedeemWebPurchaseListener.Result.AlreadyRedeemed)
                            }
                            else -> {
                                callback(RedeemWebPurchaseListener.Result.Error(result.toPurchasesError()))
                            }
                        }
                    }
                }
            }
        }
        synchronized(this@Backend) {
            redeemWebPurchaseCallbacks.addCallback(
                call,
                dispatcher,
                path,
                onResultHandler,
                Delay.NONE,
            )
        }
    }

    fun clearCaches() {
        httpClient.clearCaches()
    }

    private fun determinePostReceiptErrorHandlingBehavior(
        responseCode: Int,
        purchasesError: PurchasesError,
    ) = if (RCHTTPStatusCodes.isServerError(responseCode)) {
        PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME
    } else if (purchasesError.code == PurchasesErrorCode.UnsupportedError) {
        PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME
    } else {
        PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED
    }

    @Synchronized
    private fun <S, E> MutableMap<BackgroundAwareCallbackCacheKey, MutableList<Pair<S, E>>>.addBackgroundAwareCallback(
        call: Dispatcher.AsyncCall,
        dispatcher: Dispatcher,
        cacheKey: BackgroundAwareCallbackCacheKey,
        functions: Pair<S, E>,
        delay: Delay = Delay.NONE,
    ) {
        val foregroundCacheKey = cacheKey.copy(appInBackground = false)
        val foregroundCallAlreadyInPlace = containsKey(foregroundCacheKey)
        val cacheKeyToUse = if (cacheKey.appInBackground && foregroundCallAlreadyInPlace) {
            debugLog(NetworkStrings.SAME_CALL_SCHEDULED_WITHOUT_JITTER.format(foregroundCacheKey))
            foregroundCacheKey
        } else {
            cacheKey
        }
        addCallback(call, dispatcher, cacheKeyToUse, functions, delay)
        // In case we have a request with a jittered delay queued, and we perform the same request without
        // jittered delay, we want to call the callback using the unjittered request
        val backgroundedCacheKey = cacheKey.copy(appInBackground = true)
        val backgroundCallAlreadyInPlace = containsKey(foregroundCacheKey)
        if (!cacheKey.appInBackground && backgroundCallAlreadyInPlace) {
            debugLog(NetworkStrings.SAME_CALL_SCHEDULED_WITH_JITTER.format(foregroundCacheKey))
            remove(backgroundedCacheKey)?.takeIf { it.isNotEmpty() }?.let { backgroundedCallbacks ->
                if (containsKey(cacheKey)) {
                    this[cacheKey]?.addAll(backgroundedCallbacks)
                } else {
                    this[cacheKey] = backgroundedCallbacks
                }
            }
        }
    }

    private fun <K, F> MutableMap<K, MutableList<F>>.addCallback(
        call: Dispatcher.AsyncCall,
        dispatcher: Dispatcher,
        cacheKey: K,
        functions: F,
        delay: Delay = Delay.NONE,
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

internal data class BackgroundAwareCallbackCacheKey(
    val cacheKey: List<String>,
    val appInBackground: Boolean,
)

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

/**
 * [GoogleReplacementMode] used to be `GoogleProrationMode`. The backend still expects these values, hence this enum.
 */
private enum class LegacyProrationMode {
    IMMEDIATE_WITHOUT_PRORATION,
    IMMEDIATE_WITH_TIME_PRORATION,
    IMMEDIATE_AND_CHARGE_FULL_PRICE,
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE,
    DEFERRED,
}

private val GoogleReplacementMode.asLegacyProrationMode: LegacyProrationMode
    get() = when (this) {
        GoogleReplacementMode.WITHOUT_PRORATION -> LegacyProrationMode.IMMEDIATE_WITHOUT_PRORATION
        GoogleReplacementMode.WITH_TIME_PRORATION -> LegacyProrationMode.IMMEDIATE_WITH_TIME_PRORATION
        GoogleReplacementMode.CHARGE_FULL_PRICE -> LegacyProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE
        GoogleReplacementMode.CHARGE_PRORATED_PRICE -> LegacyProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE
        GoogleReplacementMode.DEFERRED -> LegacyProrationMode.DEFERRED
    }
