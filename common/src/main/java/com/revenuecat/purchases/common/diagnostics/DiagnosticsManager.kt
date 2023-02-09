package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.verboseLog
import java.io.IOException

class DiagnosticsManager(
    private val diagnosticsFileHelper: DiagnosticsFileHelper,
    private val diagnosticsAnonymizer: DiagnosticsAnonymizer,
    private val backend: Backend,
    private val diagnosticsDispatcher: Dispatcher
) {
    fun syncDiagnosticsFileIfNeeded() {
        enqueue {
            try {
                if (diagnosticsFileHelper.diagnosticsFileIsEmpty()) return@enqueue
                val diagnosticsList = diagnosticsFileHelper.readDiagnosticsFile()
                val diagnosticsCount = diagnosticsList.size
                if (diagnosticsCount == 0) {
                    verboseLog("No diagnostics to sync.")
                    return@enqueue
                }
                backend.postDiagnostics(
                    diagnosticsList = diagnosticsList,
                    onSuccessHandler = {
                        verboseLog("Synced diagnostics file successfully.")
                        diagnosticsFileHelper.cleanSentDiagnostics(diagnosticsCount)
                    },
                    onErrorHandler = { error ->
                        verboseLog("Error syncing diagnostics file: $error")
                        diagnosticsFileHelper.deleteDiagnosticsFile()
                    }
                )
            } catch (e: IOException) {
                verboseLog("Error syncing diagnostics: $e")
                diagnosticsFileHelper.deleteDiagnosticsFile()
            }
        }
    }

    fun trackEvent(diagnosticsEvent: DiagnosticsEvent) {
        enqueue {
            val anonymizedEvent = diagnosticsAnonymizer.anonymizeEventIfNeeded(diagnosticsEvent)
            // WIP: Check that file size is not above certain limit. If it is, delete.
            verboseLog("Tracking diagnostics event: $anonymizedEvent")
            try {
                diagnosticsFileHelper.appendEventToDiagnosticsFile(anonymizedEvent)
            } catch (e: IOException) {
                verboseLog("Error tracking diagnostics event: $e")
            }
        }
    }

    private fun enqueue(command: () -> Unit) {
        diagnosticsDispatcher.enqueue(command = command)
    }
}
