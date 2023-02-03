package com.revenuecat.purchases.common.telemetry

import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.verboseLog
import java.io.IOException

class TelemetryManager(
    private val telemetryFileHelper: TelemetryFileHelper,
    private val telemetryAnonymizer: TelemetryAnonymizer,
    private val backend: Backend,
    private val telemetryDispatcher: Dispatcher
) {
    fun syncTelemetryFileIfNeeded() {
        enqueue {
            try {
                if (telemetryFileHelper.telemetryFileIsEmpty()) return@enqueue
                val telemetryList = telemetryFileHelper.readTelemetryFile()
                val telemetryCount = telemetryList.size
                if (telemetryCount == 0) {
                    verboseLog("No telemetry to sync.")
                    return@enqueue
                }
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
            } catch (e: IOException) {
                verboseLog("Error syncing metrics: $e")
                telemetryFileHelper.deleteTelemetryFile()
            }
        }
    }

    fun trackEvent(telemetryEvent: TelemetryEvent) {
        enqueue {
            val anonymizedEvent = telemetryAnonymizer.anonymizeEventIfNeeded(telemetryEvent)
            // WIP: Check that file size is not above certain limit. If it is, delete.
            verboseLog("Tracking telemetry event: $anonymizedEvent")
            try {
                telemetryFileHelper.appendEventToTelemetryFile(anonymizedEvent)
            } catch (e: IOException) {
                verboseLog("Error tracking telemetry event: $e")
            }
        }
    }

    private fun enqueue(command: () -> Unit) {
        telemetryDispatcher.enqueue(command = command)
    }
}
