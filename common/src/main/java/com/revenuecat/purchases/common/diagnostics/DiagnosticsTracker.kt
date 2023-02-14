package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.verboseLog
import java.io.IOException

class DiagnosticsTracker(
    private val diagnosticsFileHelper: DiagnosticsFileHelper,
    private val diagnosticsAnonymizer: DiagnosticsAnonymizer,
    private val diagnosticsDispatcher: Dispatcher
) {

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
