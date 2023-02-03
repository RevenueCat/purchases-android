package com.revenuecat.purchases.common.telemetry

import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.verboseLog

class TelemetryManager(
    private val telemetryFileHelper: TelemetryFileHelper,
    private val telemetryAnonymizer: TelemetryAnonymizer,
    private val backend: Backend,
    private val telemetryDispatcher: Dispatcher
) {
    fun syncTelemetryFileIfNeeded() {
        telemetryDispatcher.enqueue(
            command = {
                if (telemetryFileHelper.telemetryFileIsEmpty()) return@enqueue
                val telemetryList = telemetryFileHelper.readTelemetryFile()
                val telemetryCount = telemetryList.size
                backend.postTelemetry(
                    telemetryList = telemetryList,
                    onSuccessHandler = {
                        verboseLog("Synced telemetry file successfully.")
                        telemetryFileHelper.cleanSentTelemetry(telemetryCount)
                    },
                    onErrorHandler = { error ->
                        verboseLog("Error syncing telemetry file: $error")
                        telemetryFileHelper.deleteTelemetryFile()
                    }
                )
            }
        )
    }

    fun trackEvent(telemetryEvent: TelemetryEvent) {
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
