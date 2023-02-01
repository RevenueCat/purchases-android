package com.revenuecat.purchases.common.telemetry

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.verboseLog

class TelemetrySyncingManager(
    private val telemetryFileHelper: TelemetryFileHelper,
    private val backend: Backend,
    private val telemetryDispatcher: Dispatcher,
    private val telemetryEnabled: Boolean
) {
    fun syncTelemetryFileIfNeeded() {
        if (!telemetryEnabled) {
            verboseLog("Telemetry disabled. Skipping syncing events.")
            return
        }
        telemetryDispatcher.enqueue(
            command =  {
                if (telemetryFileHelper.telemetryFileIsEmpty()) return@enqueue
                performBackendRequest(
                    completion = { telemetrySentCount ->
                        verboseLog("Synced telemetry file successfully.")
                        telemetryFileHelper.cleanSentTelemetry(telemetrySentCount)
                    },
                    onError = { error ->
                        verboseLog("Error syncing telemetry file: $error")
                        // WIP: Check retry count, delete files if retried too many times.
                    }
                )
            }
        )
    }

    private fun performBackendRequest(completion: (Int) -> Unit, onError: (PurchasesError) -> Unit) {
        val telemetryList = telemetryFileHelper.readTelemetryFile()
        val telemetryCount = telemetryList.size
        backend.postTelemetry(
            telemetryList = telemetryList,
            onSuccessHandler = {
                completion(telemetryCount)
            },
            onErrorHandler = {
                onError(it)
            }
        )
    }
}
