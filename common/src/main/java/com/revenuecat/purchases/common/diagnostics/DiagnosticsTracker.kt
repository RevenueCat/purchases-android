package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.verboseLog
import java.io.IOException

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
        responseTime: Long,
        wasSuccessful: Boolean,
        responseCode: Int,
        resultOrigin: HTTPResult.Origin?
    ) {
        trackEvent(
            DiagnosticsEvent.Log(
                name = DiagnosticsLogEventName.HTTP_REQUEST_PERFORMED,
                properties = mapOf(
                    "endpoint_name" to endpoint.name,
                    "response_time_millis" to responseTime,
                    "successful" to wasSuccessful,
                    "response_code" to responseCode,
                    "etag_hit" to (resultOrigin == HTTPResult.Origin.CACHE)
                )
            )
        )
    }

    fun trackMaxEventsStoredLimitReached(totalEventsStored: Int, eventsRemoved: Int, useCurrentThread: Boolean = true) {
        val event = DiagnosticsEvent.Log(
            name = DiagnosticsLogEventName.MAX_EVENTS_STORED_LIMIT_REACHED,
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

    fun trackEvent(diagnosticsEvent: DiagnosticsEvent) {
        diagnosticsDispatcher.enqueue(command = {
            trackEventInCurrentThread(diagnosticsEvent)
        })
    }

    internal fun trackEventInCurrentThread(diagnosticsEvent: DiagnosticsEvent) {
        val anonymizedEvent = diagnosticsAnonymizer.anonymizeEventIfNeeded(diagnosticsEvent)
        verboseLog("Tracking diagnostics event: $anonymizedEvent")
        try {
            diagnosticsFileHelper.appendEventToDiagnosticsFile(anonymizedEvent)
        } catch (e: IOException) {
            verboseLog("Error tracking diagnostics event: $e")
        }
    }
}
