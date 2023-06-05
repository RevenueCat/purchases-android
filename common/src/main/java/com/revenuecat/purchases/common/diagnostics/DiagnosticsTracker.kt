package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.verboseLog
import java.io.IOException
import kotlin.time.Duration

/**
 * This class is the entry point for all diagnostics tracking. It contains all information for all events
 * sent and their properties. Use this class if you want to send a a diagnostics entry.
 */
class DiagnosticsTracker(
    private val diagnosticsFileHelper: DiagnosticsFileHelper,
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
            DiagnosticsEntry.Event(
                name = DiagnosticsEventName.HTTP_REQUEST_PERFORMED,
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
        // We also send http requests as a counter to have more real-time data
        trackEvent(
            DiagnosticsEntry.Counter(
                name = DiagnosticsCounterName.HTTP_REQUEST_PERFORMED,
                tags = mapOf(
                    ENDPOINT_NAME_KEY to endpoint.name,
                    SUCCESSFUL_KEY to wasSuccessful.toString(),
                    RESPONSE_CODE_KEY to responseCode.toString(),
                    ETAG_HIT_KEY to eTagHit.toString(),
                    VERIFICATION_RESULT_KEY to verificationResult.name,
                ),
                value = 1,
            ),
        )
    }

    fun trackGoogleQueryProductDetailsRequest(
        productType: String,
        billingResponseCode: Int,
        billingDebugMessage: String,
        responseTime: Duration,
    ) {
        trackEvent(
            DiagnosticsEntry.Event(
                name = DiagnosticsEventName.GOOGLE_QUERY_PRODUCT_DETAILS_REQUEST,
                properties = mapOf(
                    PRODUCT_TYPE_QUERIED_KEY to productType,
                    "billing_response_code" to billingResponseCode,
                    "billing_debug_message" to billingDebugMessage,
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
            DiagnosticsEntry.Event(
                name = DiagnosticsEventName.GOOGLE_QUERY_PURCHASES_REQUEST,
                properties = mapOf(
                    PRODUCT_TYPE_QUERIED_KEY to productType,
                    "billing_response_code" to billingResponseCode,
                    "billing_debug_message" to billingDebugMessage,
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
            DiagnosticsEntry.Event(
                name = DiagnosticsEventName.GOOGLE_QUERY_PURCHASE_HISTORY_REQUEST,
                properties = mapOf(
                    PRODUCT_TYPE_QUERIED_KEY to productType,
                    "billing_response_code" to billingResponseCode,
                    "billing_debug_message" to billingDebugMessage,
                    RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                ),
            ),
        )
    }

    fun trackMaxEventsStoredLimitReached(totalEventsStored: Int, eventsRemoved: Int, useCurrentThread: Boolean = true) {
        val event = DiagnosticsEntry.Event(
            name = DiagnosticsEventName.MAX_EVENTS_STORED_LIMIT_REACHED,
            properties = mapOf(
                "total_number_events_stored" to totalEventsStored,
                "events_removed" to eventsRemoved,
            ),
        )
        if (useCurrentThread) {
            trackEventInCurrentThread(event)
        } else {
            trackEvent(event)
        }
    }

    fun trackEvent(diagnosticsEntry: DiagnosticsEntry) {
        diagnosticsDispatcher.enqueue(command = {
            trackEventInCurrentThread(diagnosticsEntry)
        })
    }

    internal fun trackEventInCurrentThread(diagnosticsEntry: DiagnosticsEntry) {
        val anonymizedEvent = diagnosticsAnonymizer.anonymizeEntryIfNeeded(diagnosticsEntry)
        verboseLog("Tracking diagnostics event: $anonymizedEvent")
        try {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(anonymizedEvent)
        } catch (e: IOException) {
            verboseLog("Error tracking diagnostics event: $e")
        }
    }
}
