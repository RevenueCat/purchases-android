package com.revenuecat.purchases.common.diagnostics

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.events.EventsManager
import com.revenuecat.purchases.common.networking.ConnectionErrorReason
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings
import com.revenuecat.purchases.utils.filterNotNullValues
import java.io.IOException
import java.util.UUID
import kotlin.time.Duration

internal fun interface DiagnosticsEventTrackerListener {
    public fun onEventTracked()
}

/**
 * This class is the entry point for all diagnostics tracking. It contains all information for all events
 * sent and their properties. Use this class if you want to send a a diagnostics entry.
 */
@Suppress("TooManyFunctions")
internal class DiagnosticsTracker(
    private val appConfig: AppConfig,
    private val diagnosticsFileHelper: DiagnosticsFileHelper,
    private val diagnosticsHelper: DiagnosticsHelper,
    private val diagnosticsDispatcher: Dispatcher,
    private val appSessionID: UUID = EventsManager.appSessionID,
) {
    private companion object {
        const val HOST_KEY = "host"
        const val ENDPOINT_NAME_KEY = "endpoint_name"
        const val SUCCESSFUL_KEY = "successful"
        const val RESPONSE_CODE_KEY = "response_code"
        const val BACKEND_ERROR_CODE_KEY = "backend_error_code"
        const val ETAG_HIT_KEY = "etag_hit"
        const val VERIFICATION_RESULT_KEY = "verification_result"
        const val RESPONSE_TIME_MILLIS_KEY = "response_time_millis"
        const val PRODUCT_TYPE_QUERIED_KEY = "product_type_queried"
        const val PRODUCT_ID_KEY = "product_id"
        const val PRODUCT_TYPE_KEY = "product_type"
        const val OLD_PRODUCT_ID_KEY = "old_product_id"
        const val HAS_INTRO_TRIAL_KEY = "has_intro_trial"
        const val HAS_INTRO_PRICE_KEY = "has_intro_price"
        const val PRODUCT_IDS_KEY = "product_ids"
        const val PURCHASE_STATUSES_KEY = "purchase_statuses"
        const val BILLING_RESPONSE_CODE = "billing_response_code"
        const val BILLING_DEBUG_MESSAGE = "billing_debug_message"
        const val PENDING_REQUEST_COUNT = "pending_request_count"
        const val REQUESTED_PRODUCT_IDS_KEY = "requested_product_ids"
        const val NOT_FOUND_PRODUCT_IDS_KEY = "not_found_product_ids"
        const val FOUND_PRODUCT_IDS_KEY = "found_product_ids"
        const val ERROR_MESSAGE_KEY = "error_message"
        const val ERROR_CODE_KEY = "error_code"
        const val CACHE_STATUS_KEY = "cache_status"
        const val FETCH_POLICY_KEY = "fetch_policy"
        const val HAD_UNSYNCED_PURCHASES_BEFORE_KEY = "had_unsynced_purchases_before"
        const val IS_RETRY = "is_retry"
        const val REQUEST_STATUS_KEY = "request_status"
        const val CONNECTION_ERROR_REASON_KEY = "connection_error_reason"
    }

    private val commonProperties = if (appConfig.store == Store.PLAY_STORE) {
        mapOf(
            "play_store_version" to appConfig.playStoreVersionName,
            "play_services_version" to appConfig.playServicesVersionName,
        ).filterNotNullValues()
    } else {
        emptyMap()
    }

    public var listener: DiagnosticsEventTrackerListener? = null

    @Suppress("LongParameterList")
    public fun trackHttpRequestPerformed(
        host: String,
        endpoint: Endpoint,
        responseTime: Duration,
        wasSuccessful: Boolean,
        responseCode: Int,
        backendErrorCode: Int?,
        resultOrigin: HTTPResult.Origin?,
        verificationResult: VerificationResult,
        isRetry: Boolean,
        connectionErrorReason: ConnectionErrorReason?,
    ) {
        val eTagHit = resultOrigin == HTTPResult.Origin.CACHE
        trackEvent(
            eventName = DiagnosticsEntryName.HTTP_REQUEST_PERFORMED,
            properties = mapOf(
                HOST_KEY to host,
                ENDPOINT_NAME_KEY to endpoint.name,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                SUCCESSFUL_KEY to wasSuccessful,
                RESPONSE_CODE_KEY to responseCode,
                BACKEND_ERROR_CODE_KEY to backendErrorCode,
                ETAG_HIT_KEY to eTagHit,
                VERIFICATION_RESULT_KEY to verificationResult.name,
                IS_RETRY to isRetry,
                CONNECTION_ERROR_REASON_KEY to connectionErrorReason?.name,
            ).filterNotNullValues(),
        )
    }

    // region Google

    public fun trackGoogleQueryProductDetailsRequest(
        requestedProductIds: Set<String>,
        productType: String,
        billingResponseCode: Int,
        billingDebugMessage: String,
        responseTime: Duration,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.GOOGLE_QUERY_PRODUCT_DETAILS_REQUEST,
            properties = mapOf(
                REQUESTED_PRODUCT_IDS_KEY to requestedProductIds,
                PRODUCT_TYPE_QUERIED_KEY to productType,
                BILLING_RESPONSE_CODE to billingResponseCode,
                BILLING_DEBUG_MESSAGE to billingDebugMessage,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
            ),
        )
    }

    public fun trackGoogleQueryPurchasesRequest(
        productType: String,
        billingResponseCode: Int,
        billingDebugMessage: String,
        responseTime: Duration,
        foundProductIds: List<String>,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.GOOGLE_QUERY_PURCHASES_REQUEST,
            properties = mapOf(
                PRODUCT_TYPE_QUERIED_KEY to productType,
                BILLING_RESPONSE_CODE to billingResponseCode,
                BILLING_DEBUG_MESSAGE to billingDebugMessage,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                FOUND_PRODUCT_IDS_KEY to foundProductIds,
            ),
        )
    }

    public fun trackGoogleQueryPurchaseHistoryRequest(
        productType: String,
        billingResponseCode: Int,
        billingDebugMessage: String,
        responseTime: Duration,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.GOOGLE_QUERY_PURCHASE_HISTORY_REQUEST,
            properties = mapOf(
                PRODUCT_TYPE_QUERIED_KEY to productType,
                BILLING_RESPONSE_CODE to billingResponseCode,
                BILLING_DEBUG_MESSAGE to billingDebugMessage,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
            ),
        )
    }

    public fun trackGoogleBillingStartConnection() {
        trackEvent(
            eventName = DiagnosticsEntryName.GOOGLE_BILLING_START_CONNECTION,
            properties = emptyMap(),
        )
    }

    public fun trackGoogleBillingSetupFinished(responseCode: Int, debugMessage: String, pendingRequestCount: Int) {
        trackEvent(
            eventName = DiagnosticsEntryName.GOOGLE_BILLING_SETUP_FINISHED,
            properties = mapOf(
                BILLING_RESPONSE_CODE to responseCode,
                BILLING_DEBUG_MESSAGE to debugMessage,
                PENDING_REQUEST_COUNT to pendingRequestCount,
            ),
        )
    }

    public fun trackGoogleBillingServiceDisconnected() {
        trackEvent(
            eventName = DiagnosticsEntryName.GOOGLE_BILLING_SERVICE_DISCONNECTED,
            properties = emptyMap(),
        )
    }

    public fun trackGooglePurchaseStarted(
        productId: String,
        oldProductId: String?,
        hasIntroTrial: Boolean?,
        hasIntroPrice: Boolean?,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.GOOGLE_PURCHASE_STARTED,
            properties = mapOf(
                PRODUCT_ID_KEY to productId,
                OLD_PRODUCT_ID_KEY to oldProductId,
                HAS_INTRO_TRIAL_KEY to hasIntroTrial,
                HAS_INTRO_PRICE_KEY to hasIntroPrice,
            ).filterNotNullValues(),
        )
    }

    public fun trackGooglePurchaseUpdateReceived(
        productIds: List<String>?,
        purchaseStatuses: List<String>?,
        billingResponseCode: Int,
        billingDebugMessage: String,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.GOOGLE_PURCHASES_UPDATE_RECEIVED,
            properties = mapOf(
                PRODUCT_IDS_KEY to productIds,
                PURCHASE_STATUSES_KEY to purchaseStatuses,
                BILLING_RESPONSE_CODE to billingResponseCode,
                BILLING_DEBUG_MESSAGE to billingDebugMessage,
            ).filterNotNullValues(),
        )
    }

    // endregion

    // region Amazon

    public fun trackAmazonQueryProductDetailsRequest(
        responseTime: Duration,
        wasSuccessful: Boolean,
        requestedProductIds: Set<String>,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.AMAZON_QUERY_PRODUCT_DETAILS_REQUEST,
            properties = mapOf(
                SUCCESSFUL_KEY to wasSuccessful,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                REQUESTED_PRODUCT_IDS_KEY to requestedProductIds,
            ),
        )
    }

    public fun trackAmazonQueryPurchasesRequest(
        responseTime: Duration,
        wasSuccessful: Boolean,
        foundProductIds: List<String>?,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.AMAZON_QUERY_PURCHASES_REQUEST,
            properties = mapOf(
                SUCCESSFUL_KEY to wasSuccessful,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                FOUND_PRODUCT_IDS_KEY to foundProductIds,
            ).filterNotNullValues(),
        )
    }

    public fun trackAmazonPurchaseAttempt(
        productId: String,
        requestStatus: String?,
        errorCode: Int?,
        errorMessage: String?,
        responseTime: Duration,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.AMAZON_PURCHASE_ATTEMPT,
            properties = mapOf(
                PRODUCT_ID_KEY to productId,
                REQUEST_STATUS_KEY to requestStatus,
                ERROR_CODE_KEY to errorCode,
                ERROR_MESSAGE_KEY to errorMessage,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
            ).filterNotNullValues(),
        )
    }

    // endregion

    public fun trackMaxEventsStoredLimitReached(useCurrentThread: Boolean = true) {
        val event = DiagnosticsEntry(
            name = DiagnosticsEntryName.MAX_EVENTS_STORED_LIMIT_REACHED,
            properties = commonProperties,
            appSessionID = appSessionID,
        )
        if (useCurrentThread) {
            trackEventInCurrentThread(event)
        } else {
            trackEvent(event)
        }
    }

    public fun trackMaxDiagnosticsSyncRetriesReached() {
        trackEvent(
            eventName = DiagnosticsEntryName.MAX_DIAGNOSTICS_SYNC_RETRIES_REACHED,
            properties = emptyMap(),
        )
    }

    public fun trackClearingDiagnosticsAfterFailedSync() {
        trackEvent(
            eventName = DiagnosticsEntryName.CLEARING_DIAGNOSTICS_AFTER_FAILED_SYNC,
            properties = emptyMap(),
        )
    }

    public fun trackProductDetailsNotSupported(
        billingResponseCode: Int,
        billingDebugMessage: String,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.PRODUCT_DETAILS_NOT_SUPPORTED,
            properties = mapOf(
                "play_store_version" to (appConfig.playStoreVersionName ?: ""),
                "play_services_version" to (appConfig.playServicesVersionName ?: ""),
                BILLING_RESPONSE_CODE to billingResponseCode,
                BILLING_DEBUG_MESSAGE to billingDebugMessage,
            ),
        )
    }

    public fun trackCustomerInfoVerificationResultIfNeeded(
        customerInfo: CustomerInfo,
    ) {
        val verificationResult = customerInfo.entitlements.verification
        if (verificationResult == VerificationResult.NOT_REQUESTED) {
            return
        }
        trackEvent(
            eventName = DiagnosticsEntryName.CUSTOMER_INFO_VERIFICATION_RESULT,
            properties = mapOf(
                VERIFICATION_RESULT_KEY to verificationResult.name,
            ),
        )
    }

    // region Offline Entitlements

    public fun trackEnteredOfflineEntitlementsMode() {
        trackEvent(
            eventName = DiagnosticsEntryName.ENTERED_OFFLINE_ENTITLEMENTS_MODE,
            properties = mapOf(),
        )
    }

    public fun trackErrorEnteringOfflineEntitlementsMode(error: PurchasesError) {
        val reason = if (
            error.code == PurchasesErrorCode.UnsupportedError &&
            error.underlyingErrorMessage == OfflineEntitlementsStrings.OFFLINE_ENTITLEMENTS_UNSUPPORTED_INAPP_PURCHASES
        ) {
            "one_time_purchase_found"
        } else if (
            error.code == PurchasesErrorCode.CustomerInfoError &&
            error.underlyingErrorMessage == OfflineEntitlementsStrings.PRODUCT_ENTITLEMENT_MAPPING_REQUIRED
        ) {
            "no_entitlement_mapping_available"
        } else {
            "unknown"
        }
        trackEvent(
            eventName = DiagnosticsEntryName.ERROR_ENTERING_OFFLINE_ENTITLEMENTS_MODE,
            properties = mapOf(
                "offline_entitlement_error_reason" to reason,
                "error_message" to "${error.message} Underlying error: ${error.underlyingErrorMessage}",
            ),
        )
    }

    // endregion

    // region Get Offerings

    public fun trackGetOfferingsStarted() {
        trackEvent(
            eventName = DiagnosticsEntryName.GET_OFFERINGS_STARTED,
            properties = emptyMap(),
        )
    }

    enum class CacheStatus {
        NOT_CHECKED,
        NOT_FOUND,
        STALE,
        VALID,
    }

    @Suppress("LongParameterList")
    public fun trackGetOfferingsResult(
        requestedProductIds: Set<String>?,
        notFoundProductIds: Set<String>?,
        errorMessage: String?,
        errorCode: Int?,
        verificationResult: String?,
        cacheStatus: CacheStatus,
        responseTime: Duration,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.GET_OFFERINGS_RESULT,
            properties = mapOf(
                REQUESTED_PRODUCT_IDS_KEY to requestedProductIds,
                NOT_FOUND_PRODUCT_IDS_KEY to notFoundProductIds,
                ERROR_MESSAGE_KEY to errorMessage,
                ERROR_CODE_KEY to errorCode,
                VERIFICATION_RESULT_KEY to verificationResult,
                CACHE_STATUS_KEY to cacheStatus.name,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
            ).filterNotNullValues(),
        )
    }

    // endregion

    // region Get Products

    public fun trackGetProductsStarted(requestedProductIds: Set<String>) {
        trackEvent(
            eventName = DiagnosticsEntryName.GET_PRODUCTS_STARTED,
            properties = mapOf(
                REQUESTED_PRODUCT_IDS_KEY to requestedProductIds,
            ),
        )
    }

    public fun trackGetProductsResult(
        requestedProductIds: Set<String>,
        notFoundProductIds: Set<String>,
        errorMessage: String?,
        errorCode: Int?,
        responseTime: Duration,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.GET_PRODUCTS_RESULT,
            properties = mapOf(
                REQUESTED_PRODUCT_IDS_KEY to requestedProductIds,
                NOT_FOUND_PRODUCT_IDS_KEY to notFoundProductIds,
                ERROR_MESSAGE_KEY to errorMessage,
                ERROR_CODE_KEY to errorCode,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
            ).filterNotNullValues(),
        )
    }

    // endregion

    // region Sync purchases

    public fun trackSyncPurchasesStarted() {
        trackEvent(
            eventName = DiagnosticsEntryName.SYNC_PURCHASES_STARTED,
            properties = emptyMap(),
        )
    }

    public fun trackSyncPurchasesResult(
        errorCode: Int?,
        errorMessage: String?,
        responseTime: Duration,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.SYNC_PURCHASES_RESULT,
            properties = mapOf(
                ERROR_CODE_KEY to errorCode,
                ERROR_MESSAGE_KEY to errorMessage,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
            ).filterNotNullValues(),
        )
    }

    // endregion Sync purchases

    // region Restore purchases

    public fun trackRestorePurchasesStarted() {
        trackEvent(
            eventName = DiagnosticsEntryName.RESTORE_PURCHASES_STARTED,
            properties = emptyMap(),
        )
    }

    public fun trackRestorePurchasesResult(
        errorCode: Int?,
        errorMessage: String?,
        responseTime: Duration,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.RESTORE_PURCHASES_RESULT,
            properties = mapOf(
                ERROR_CODE_KEY to errorCode,
                ERROR_MESSAGE_KEY to errorMessage,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
            ).filterNotNullValues(),
        )
    }

    // endregion Restore purchases

    // region Get Customer Info

    public fun trackGetCustomerInfoStarted() {
        trackEvent(
            eventName = DiagnosticsEntryName.GET_CUSTOMER_INFO_STARTED,
            properties = emptyMap(),
        )
    }

    @Suppress("LongParameterList")
    public fun trackGetCustomerInfoResult(
        cacheFetchPolicy: CacheFetchPolicy,
        verificationResult: VerificationResult?,
        hadUnsyncedPurchasesBefore: Boolean?,
        errorMessage: String?,
        errorCode: Int?,
        responseTime: Duration,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.GET_CUSTOMER_INFO_RESULT,
            properties = mapOf(
                FETCH_POLICY_KEY to cacheFetchPolicy.name,
                VERIFICATION_RESULT_KEY to verificationResult?.name,
                HAD_UNSYNCED_PURCHASES_BEFORE_KEY to hadUnsyncedPurchasesBefore,
                ERROR_MESSAGE_KEY to errorMessage,
                ERROR_CODE_KEY to errorCode,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
            ).filterNotNullValues(),
        )
    }

    // endregion Get Customer Info

    // region Purchase

    public fun trackPurchaseStarted(productId: String, productType: ProductType) {
        trackEvent(
            eventName = DiagnosticsEntryName.PURCHASE_STARTED,
            properties = mapOf(
                PRODUCT_ID_KEY to productId,
                PRODUCT_TYPE_KEY to productType.diagnosticsName,
            ),
        )
    }

    @Suppress("LongParameterList")
    public fun trackPurchaseResult(
        productId: String,
        productType: ProductType,
        errorCode: Int?,
        errorMessage: String?,
        responseTime: Duration,
        verificationResult: VerificationResult?,
    ) {
        trackEvent(
            eventName = DiagnosticsEntryName.PURCHASE_RESULT,
            properties = mapOf(
                PRODUCT_ID_KEY to productId,
                PRODUCT_TYPE_KEY to productType.diagnosticsName,
                ERROR_CODE_KEY to errorCode,
                ERROR_MESSAGE_KEY to errorMessage,
                RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                VERIFICATION_RESULT_KEY to verificationResult?.name,
            ).filterNotNullValues(),
        )
    }

    // endregion Purchase

    private fun trackEvent(eventName: DiagnosticsEntryName, properties: Map<String, Any>) {
        trackEvent(
            DiagnosticsEntry(
                name = eventName,
                properties = commonProperties + properties,
                appSessionID = appSessionID,
            ),
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public fun trackEvent(diagnosticsEntry: DiagnosticsEntry) {
        checkAndClearDiagnosticsFileIfTooBig {
            trackEventInCurrentThread(diagnosticsEntry)
        }
    }

    internal fun trackEventInCurrentThread(diagnosticsEntry: DiagnosticsEntry) {
        verboseLog { "Tracking diagnostics entry: $diagnosticsEntry" }
        try {
            diagnosticsFileHelper.appendEvent(diagnosticsEntry)
            listener?.onEventTracked()
        } catch (e: IOException) {
            verboseLog { "Error tracking diagnostics entry: $e" }
        }
    }

    private fun checkAndClearDiagnosticsFileIfTooBig(completion: () -> Unit) {
        enqueue {
            if (diagnosticsFileHelper.isDiagnosticsFileTooBig()) {
                verboseLog { "Diagnostics file is too big. Deleting it." }
                diagnosticsHelper.resetDiagnosticsStatus()
                trackMaxEventsStoredLimitReached()
            }
            completion()
        }
    }

    private fun enqueue(command: () -> Unit) {
        diagnosticsDispatcher.enqueue(command = command)
    }
}

private val ProductType.diagnosticsName: String
    get() = when (this) {
        ProductType.SUBS -> "AUTO_RENEWABLE_SUBSCRIPTION"
        ProductType.INAPP -> "NON_SUBSCRIPTION"
        ProductType.UNKNOWN -> "UNKNOWN"
    }
