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
    private val diagnosticsDispatcher: Dispatcher
) {

    fun trackHttpRequestPerformed(
        endpoint: Endpoint,
        responseTime: Duration,
        wasSuccessful: Boolean,
        responseCode: Int,
        resultOrigin: HTTPResult.Origin?
    ) {
        trackEvent(
            DiagnosticsEntry.Event(
                name = DiagnosticsEventName.HTTP_REQUEST_PERFORMED,
                properties = mapOf(
                    "endpoint_name" to endpoint.name,
                    "response_time_millis" to responseTime.inWholeMilliseconds,
                    "successful" to wasSuccessful,
                    "response_code" to responseCode,
                    "etag_hit" to (resultOrigin == HTTPResult.Origin.CACHE)
                )
            )
        )
    }

    fun trackGoogleQuerySkuDetailsRequest(
        skuType: String,
        billingResponseCode: Int,
        billingDebugMessage: String,
        responseTimeMillis: Long
    ) {
        trackEvent(
            DiagnosticsEntry.Event(
                name = DiagnosticsEventName.GOOGLE_QUERY_SKU_DETAILS_REQUEST,
                properties = mapOf(
                    "sku_type_queried" to skuType,
                    "billing_response_code" to billingResponseCode,
                    "billing_debug_message" to billingDebugMessage,
                    "response_time_millis" to responseTimeMillis
                )
            )
        )
    }

    fun trackGoogleQueryPurchasesRequest(
        skuType: String,
        billingResponseCode: Int,
        billingDebugMessage: String,
        responseTimeMillis: Long
    ) {
        trackEvent(
            DiagnosticsEntry.Event(
                name = DiagnosticsEventName.GOOGLE_QUERY_PURCHASES_REQUEST,
                properties = mapOf(
                    "sku_type_queried" to skuType,
                    "billing_response_code" to billingResponseCode,
                    "billing_debug_message" to billingDebugMessage,
                    "response_time_millis" to responseTimeMillis
                )
            )
        )
    }

    fun trackGoogleQueryPurchaseHistoryRequest(
        billingResponseCode: Int,
        billingDebugMessage: String,
        responseTimeMillis: Long
    ) {
        trackEvent(
            DiagnosticsEntry.Event(
                name = DiagnosticsEventName.GOOGLE_QUERY_PURCHASE_HISTORY_REQUEST,
                properties = mapOf(
                    "billing_response_code" to billingResponseCode,
                    "billing_debug_message" to billingDebugMessage,
                    "response_time_millis" to responseTimeMillis
                )
            )
        )
    }

    fun trackMaxEventsStoredLimitReached(totalEventsStored: Int, eventsRemoved: Int, useCurrentThread: Boolean = true) {
        val event = DiagnosticsEntry.Event(
            name = DiagnosticsEventName.MAX_EVENTS_STORED_LIMIT_REACHED,
            properties = mapOf(
                "total_number_events_stored" to totalEventsStored,
                "events_removed" to eventsRemoved
            )
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
