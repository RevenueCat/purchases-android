package com.revenuecat.purchases.common.diagnostics

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
        const val RESPONSE_TIME_MILLIS_KEY = "response_time_millis"
        const val PRODUCT_TYPE_QUERIED_KEY = "product_type_queried"
    }

    fun trackHttpRequestPerformed(
        endpoint: Endpoint,
        responseTime: Duration,
        wasSuccessful: Boolean,
        responseCode: Int,
        resultOrigin: HTTPResult.Origin?,
    ) {
        trackEvent(
            DiagnosticsEntry.Event(
                name = DiagnosticsEventName.HTTP_REQUEST_PERFORMED,
                properties = mapOf(
                    "endpoint_name" to endpoint.name,
                    RESPONSE_TIME_MILLIS_KEY to responseTime.inWholeMilliseconds,
                    "successful" to wasSuccessful,
                    "response_code" to responseCode,
                    "etag_hit" to (resultOrigin == HTTPResult.Origin.CACHE),
                ),
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
