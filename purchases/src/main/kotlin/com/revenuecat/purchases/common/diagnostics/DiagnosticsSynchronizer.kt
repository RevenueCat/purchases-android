package com.revenuecat.purchases.common.diagnostics

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.verboseLog
import org.json.JSONObject
import java.io.IOException

/**
 * This class is in charge of syncing all previously tracked diagnostics. All operations will be executed
 * in a single background thread. Which should match the ones used when tracking diagnostics.
 * Multithreading is not currently supported for these operations.
 *
 * If syncing diagnostics fails multiple times, we will delete any stored diagnostics data and start again.
 */
@RequiresApi(Build.VERSION_CODES.N)
internal class DiagnosticsSynchronizer(
    private val diagnosticsHelper: DiagnosticsHelper,
    private val diagnosticsFileHelper: DiagnosticsFileHelper,
    private val diagnosticsTracker: DiagnosticsTracker,
    private val backend: Backend,
    private val diagnosticsDispatcher: Dispatcher,
) {
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MAX_NUMBER_POST_RETRIES = 3

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MAX_EVENTS_TO_SYNC_PER_REQUEST: Int = 200

        fun initializeSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(
                "com_revenuecat_purchases_${context.packageName}_preferences_diagnostics",
                Context.MODE_PRIVATE,
            )
    }

    fun syncDiagnosticsFileIfNeeded() {
        enqueue {
            try {
                val diagnosticsList = getEventsToSync()
                val diagnosticsCount = diagnosticsList.size
                if (diagnosticsCount == 0) {
                    verboseLog("No diagnostics to sync.")
                    return@enqueue
                }
                backend.postDiagnostics(
                    diagnosticsList = diagnosticsList,
                    onSuccessHandler = {
                        verboseLog("Synced diagnostics file successfully.")
                        diagnosticsHelper.clearConsecutiveNumberOfErrors()
                        diagnosticsFileHelper.clear(diagnosticsCount)
                    },
                    onErrorHandler = { error, shouldRetry ->
                        if (shouldRetry) {
                            verboseLog(
                                "Error syncing diagnostics file: $error. " +
                                    "Will retry the next time the SDK is initialized",
                            )
                            if (diagnosticsHelper.increaseConsecutiveNumberOfErrors() >= MAX_NUMBER_POST_RETRIES) {
                                verboseLog(
                                    "Error syncing diagnostics file: $error. " +
                                        "This was the final attempt ($MAX_NUMBER_POST_RETRIES). " +
                                        "Deleting diagnostics file without posting.",
                                )
                                diagnosticsHelper.resetDiagnosticsStatus()
                                diagnosticsTracker.trackMaxDiagnosticsSyncRetriesReached()
                            }
                        } else {
                            verboseLog(
                                "Error syncing diagnostics file: $error. " +
                                    "Deleting diagnostics file without retrying.",
                            )
                            diagnosticsHelper.resetDiagnosticsStatus()
                            diagnosticsTracker.trackClearingDiagnosticsAfterFailedSync()
                        }
                    },
                )
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                verboseLog("Error syncing diagnostics file: $e")
                try {
                    diagnosticsHelper.resetDiagnosticsStatus()
                } catch (e: IOException) {
                    verboseLog("Error deleting diagnostics file: $e")
                }
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
