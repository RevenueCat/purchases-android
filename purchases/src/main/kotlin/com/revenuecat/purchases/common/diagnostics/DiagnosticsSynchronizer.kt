package com.revenuecat.purchases.common.diagnostics

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.verboseLog
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This class is in charge of syncing all previously tracked diagnostics. All operations will be executed
 * in a single background thread. Which should match the ones used when tracking diagnostics.
 * Multithreading is not currently supported for these operations.
 *
 * If syncing diagnostics fails multiple times, we will delete any stored diagnostics data and start again.
 */
internal class DiagnosticsSynchronizer(
    private val diagnosticsHelper: DiagnosticsHelper,
    private val diagnosticsFileHelper: DiagnosticsFileHelper,
    private val diagnosticsTracker: DiagnosticsTracker,
    private val backend: Backend,
    private val diagnosticsDispatcher: Dispatcher,
) : DiagnosticsEventTrackerListener {
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MAX_NUMBER_POST_RETRIES = 3

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MAX_EVENTS_TO_SYNC_PER_REQUEST: Int = 200
    }

    val isSyncing = AtomicBoolean(false)

    public fun syncDiagnosticsFileIfNeeded() {
        enqueue {
            try {
                if (isSyncing.getAndSet(true)) {
                    verboseLog { "Already syncing diagnostics file." }
                    return@enqueue
                }
                val diagnosticsList = getEventsToSync()
                val diagnosticsCount = diagnosticsList.size
                if (diagnosticsCount == 0) {
                    verboseLog { "No diagnostics to sync." }
                    isSyncing.set(false)
                    return@enqueue
                }
                backend.postDiagnostics(
                    diagnosticsList = diagnosticsList,
                    onSuccessHandler = {
                        verboseLog { "Synced diagnostics file successfully." }
                        diagnosticsHelper.clearConsecutiveNumberOfErrors()
                        diagnosticsFileHelper.clear(diagnosticsCount)
                        isSyncing.set(false)
                    },
                    onErrorHandler = { error, shouldRetry ->
                        if (shouldRetry) {
                            verboseLog {
                                "Error syncing diagnostics file: $error. " +
                                    "Will retry the next time the SDK is initialized"
                            }
                            if (diagnosticsHelper.increaseConsecutiveNumberOfErrors() >= MAX_NUMBER_POST_RETRIES) {
                                verboseLog {
                                    "Error syncing diagnostics file: $error. " +
                                        "This was the final attempt ($MAX_NUMBER_POST_RETRIES). " +
                                        "Deleting diagnostics file without posting."
                                }
                                diagnosticsHelper.resetDiagnosticsStatus()
                                diagnosticsTracker.trackMaxDiagnosticsSyncRetriesReached()
                            }
                        } else {
                            verboseLog {
                                "Error syncing diagnostics file: $error. " +
                                    "Deleting diagnostics file without retrying."
                            }
                            diagnosticsHelper.resetDiagnosticsStatus()
                            diagnosticsTracker.trackClearingDiagnosticsAfterFailedSync()
                        }
                        isSyncing.set(false)
                    },
                )
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                verboseLog { "Error syncing diagnostics file: $e" }
                try {
                    diagnosticsHelper.resetDiagnosticsStatus()
                } catch (e: IOException) {
                    verboseLog { "Error deleting diagnostics file: $e" }
                }
                isSyncing.set(false)
            }
        }
    }

    override fun onEventTracked() {
        syncDiagnosticsFileIfBigEnough()
    }

    private fun syncDiagnosticsFileIfBigEnough() {
        enqueue {
            if (diagnosticsFileHelper.isDiagnosticsFileBigEnoughToSync()) {
                verboseLog { "Diagnostics file is big enough to sync. Syncing it." }
                syncDiagnosticsFileIfNeeded()
            }
        }
    }

    private fun getEventsToSync(): List<JSONObject> {
        var eventsToSync: List<JSONObject> = emptyList()
        diagnosticsFileHelper.readFileAsJson { sequence ->
            eventsToSync = sequence.take(MAX_EVENTS_TO_SYNC_PER_REQUEST).toList()
        }
        return eventsToSync
    }

    private fun enqueue(command: () -> Unit) {
        diagnosticsDispatcher.enqueue(command = command)
    }
}
