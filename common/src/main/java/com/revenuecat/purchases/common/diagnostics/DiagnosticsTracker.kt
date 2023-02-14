package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.networking.ResultOrigin
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

    fun trackEndpointHit(
        path: String,
        responseTime: Long?,
        wasSuccessful: Boolean,
        responseCode: Int,
        resultOrigin: ResultOrigin?
    ) {
        trackEvent(
            DiagnosticsEvent.Log(
                name = DiagnosticsLogEventName.ENDPOINT_HIT,
                properties = mapOf(
                    "endpoint_path" to path, // WIP: Need to anonymize this better
                    "response_time_millis" to responseTime,
                    "successful" to wasSuccessful,
                    "response_code" to responseCode,
                    "etag_hit" to (resultOrigin == ResultOrigin.CACHE)
                )
            )
        )
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
