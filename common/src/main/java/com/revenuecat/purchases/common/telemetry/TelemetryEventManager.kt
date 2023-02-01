package com.revenuecat.purchases.common.telemetry

import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.verboseLog

class TelemetryEventManager(
    private val telemetryFileHelper: TelemetryFileHelper,
    private val telemetryAnonymizer: TelemetryAnonymizer,
    private val telemetryDispatcher: Dispatcher,
    private val telemetryEnabled: Boolean
) {
    @Synchronized
    fun trackEvent(telemetryEvent: TelemetryEvent) {
        if (!telemetryEnabled) {
            verboseLog("Telemetry disabled. Skipping tracking events.")
            return
        }
        telemetryDispatcher.enqueue(
            command = {
                val anonymizedEvent = telemetryAnonymizer.anonymizeEventIfNeeded(telemetryEvent)
                // WIP: Check that file size is not above certain limit. If it is, delete.
                verboseLog("Tracking telemetry event: $anonymizedEvent")
                telemetryFileHelper.appendEventToTelemetryFile(anonymizedEvent)
            }
        )
    }
}
