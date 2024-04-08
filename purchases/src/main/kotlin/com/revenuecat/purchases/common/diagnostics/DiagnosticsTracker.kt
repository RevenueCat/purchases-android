package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.utils.EventsFileHelper
import com.revenuecat.purchases.utils.isAndroidNOrNewer
import java.io.IOException
import kotlin.time.Duration

/**
 * This class is the entry point for all diagnostics tracking. It contains all information for all events
 * sent and their properties. Use this class if you want to send a a diagnostics entry.
 */
@Suppress("TooManyFunctions")
internal class DiagnosticsTracker(
    private val appConfig: AppConfig,
    private val diagnosticsFileHelper: EventsFileHelper<DiagnosticsEntry>,
    private val diagnosticsAnonymizer: DiagnosticsAnonymizer,
    private val diagnosticsDispatcher: Dispatcher,
) {
    private companion object {
        const val ENDPOINT_NAME_KEY = "endpoint_name"
        const val SUCCESSFUL_KEY = "successful"
        const val RESPONSE_CODE_KEY = "response_code"
        const val ETAG_HIT_KEY = "etag_hit"
        const val VERIFICATION_RESULT_KEY = "verification_result"
        const val RESPONSE_TIME_MILLIS_KEY = "response_time_millis"
        const val PRODUCT_TYPE_QUERIED_KEY = "product_type_queried"
        const val BILLING_RESPONSE_CODE = "billing_response_code"
        const val BILLING_DEBUG_MESSAGE = "billing_debug_message"
    }

    @Suppress("LongParameterList")
    fun trackHttpRequestPerformed(
        endpoint: Endpoint,
        responseTime: Duration,
        wasSuccessful: Boolean,
        responseCode: Int,
        resultOrigin: HTTPResult.Origin?,
        verificationResult: VerificationResult,
    ) {
        val eTagHit = resultOrigin == HTTPResult.Origin.CACHE
        trackEvent(
            DiagnosticsEntry(
                name = DiagnosticsEntryName.HTTP_REQUEST_PERFORMED,
                properties = mapOf(
                    ENDPOINT_NAME_KEY to endpoint.name,
                    RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                    SUCCESSFUL_KEY to wasSuccessful,
                    RESPONSE_CODE_KEY to responseCode,
                    ETAG_HIT_KEY to eTagHit,
                    VERIFICATION_RESULT_KEY to verificationResult.name,
                ),
            ),
        )
    }

    // region Google

    fun trackGoogleQueryProductDetailsRequest(
        productType: String,
        billingResponseCode: Int,
        billingDebugMessage: String,
        responseTime: Duration,
    ) {
        trackEvent(
            DiagnosticsEntry(
                name = DiagnosticsEntryName.GOOGLE_QUERY_PRODUCT_DETAILS_REQUEST,
                properties = mapOf(
                    PRODUCT_TYPE_QUERIED_KEY to productType,
                    BILLING_RESPONSE_CODE to billingResponseCode,
                    BILLING_DEBUG_MESSAGE to billingDebugMessage,
                    RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                ),
            ),
        )
    }

    fun trackGoogleQueryPurchasesRequest(
        productType: String,
        billingResponseCode: Int,
        billingDebugMessage: String,
        responseTime: Duration,
    ) {
        trackEvent(
            DiagnosticsEntry(
                name = DiagnosticsEntryName.GOOGLE_QUERY_PURCHASES_REQUEST,
                properties = mapOf(
                    PRODUCT_TYPE_QUERIED_KEY to productType,
                    BILLING_RESPONSE_CODE to billingResponseCode,
                    BILLING_DEBUG_MESSAGE to billingDebugMessage,
                    RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                ),
            ),
        )
    }

    fun trackGoogleQueryPurchaseHistoryRequest(
        productType: String,
        billingResponseCode: Int,
        billingDebugMessage: String,
        responseTime: Duration,
    ) {
        trackEvent(
            DiagnosticsEntry(
                name = DiagnosticsEntryName.GOOGLE_QUERY_PURCHASE_HISTORY_REQUEST,
                properties = mapOf(
                    PRODUCT_TYPE_QUERIED_KEY to productType,
                    BILLING_RESPONSE_CODE to billingResponseCode,
                    BILLING_DEBUG_MESSAGE to billingDebugMessage,
                    RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                ),
            ),
        )
    }

    // endregion

    // region Amazon

    fun trackAmazonQueryProductDetailsRequest(
        responseTime: Duration,
        wasSuccessful: Boolean,
    ) {
        trackEvent(
            DiagnosticsEntry(
                name = DiagnosticsEntryName.AMAZON_QUERY_PRODUCT_DETAILS_REQUEST,
                properties = mapOf(
                    SUCCESSFUL_KEY to wasSuccessful,
                    RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                ),
            ),
        )
    }

    fun trackAmazonQueryPurchasesRequest(
        responseTime: Duration,
        wasSuccessful: Boolean,
    ) {
        trackEvent(
            DiagnosticsEntry(
                name = DiagnosticsEntryName.AMAZON_QUERY_PURCHASES_REQUEST,
                properties = mapOf(
                    SUCCESSFUL_KEY to wasSuccessful,
                    RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                ),
            ),
        )
    }

    // endregion

    fun trackMaxEventsStoredLimitReached(useCurrentThread: Boolean = true) {
        val event = DiagnosticsEntry(
            name = DiagnosticsEntryName.MAX_EVENTS_STORED_LIMIT_REACHED,
            properties = mapOf(),
        )
        if (useCurrentThread) {
            trackEventInCurrentThread(event)
        } else {
            trackEvent(event)
        }
    }

    fun trackProductDetailsNotSupported(
        billingResponseCode: Int,
        billingDebugMessage: String,
    ) {
        val event = DiagnosticsEntry(
            name = DiagnosticsEntryName.PRODUCT_DETAILS_NOT_SUPPORTED,
            properties = mapOf(
                "play_store_version" to (appConfig.playStoreVersionName ?: ""),
                "play_services_version" to (appConfig.playServicesVersionName ?: ""),
                BILLING_RESPONSE_CODE to billingResponseCode,
                BILLING_DEBUG_MESSAGE to billingDebugMessage,
            ),
        )
        trackEvent(event)
    }

    fun trackCustomerInfoVerificationResultIfNeeded(
        customerInfo: CustomerInfo,
    ) {
        val verificationResult = customerInfo.entitlements.verification
        if (verificationResult == VerificationResult.NOT_REQUESTED) {
            return
        }
        val event = DiagnosticsEntry(
            name = DiagnosticsEntryName.CUSTOMER_INFO_VERIFICATION_RESULT,
            properties = mapOf(
                VERIFICATION_RESULT_KEY to verificationResult.name,
            ),
        )
        trackEvent(event)
    }

    fun trackEvent(diagnosticsEntry: DiagnosticsEntry) {
        diagnosticsDispatcher.enqueue(command = {
            trackEventInCurrentThread(diagnosticsEntry)
        })
    }

    internal fun trackEventInCurrentThread(diagnosticsEntry: DiagnosticsEntry) {
        if (isAndroidNOrNewer()) {
            val anonymizedEvent = diagnosticsAnonymizer.anonymizeEntryIfNeeded(diagnosticsEntry)
            verboseLog("Tracking diagnostics entry: $anonymizedEvent")
            try {
                diagnosticsFileHelper.appendEvent(anonymizedEvent)
            } catch (e: IOException) {
                verboseLog("Error tracking diagnostics entry: $e")
            }
        }
    }
}
