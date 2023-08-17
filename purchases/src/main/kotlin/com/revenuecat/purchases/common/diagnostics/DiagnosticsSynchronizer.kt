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
import java.util.stream.Collectors

/**
 * This class is in charge of syncing all previously tracked diagnostics. All operations will be executed
 * in a single background thread. Which should match the ones used when tracking diagnostics.
 * Multithreading is not currently supported for these operations.
 *
 * If syncing diagnostics fails multiple times, we will delete any stored diagnostics data and start again.
 */
@RequiresApi(Build.VERSION_CODES.N)
internal class DiagnosticsSynchronizer(
    private val diagnosticsFileHelper: DiagnosticsFileHelper,
    private val diagnosticsTracker: DiagnosticsTracker,
    private val backend: Backend,
    private val diagnosticsDispatcher: Dispatcher,
    private val sharedPreferences: SharedPreferences,
) {
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val CONSECUTIVE_FAILURES_COUNT_KEY = "consecutive_failures_count"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MAX_NUMBER_POST_RETRIES = 3

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MAX_EVENTS_TO_SYNC_PER_REQUEST: Long = 200

        fun initializeSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(
                "com_revenuecat_purchases_${context.packageName}_preferences_diagnostics",
                Context.MODE_PRIVATE,
            )
    }

    fun clearDiagnosticsFileIfTooBig() {
        if (diagnosticsFileHelper.isDiagnosticsFileTooBig()) {
            verboseLog("Diagnostics file is too big. Deleting it.")
            diagnosticsTracker.trackMaxEventsStoredLimitReached()
            resetDiagnosticsStatus()
        }
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
                        clearConsecutiveNumberOfErrors()
                        diagnosticsFileHelper.deleteOlderDiagnostics(diagnosticsCount)
                    },
                    onErrorHandler = { error, shouldRetry ->
                        if (shouldRetry) {
                            verboseLog(
                                "Error syncing diagnostics file: $error. " +
                                    "Will retry the next time the SDK is initialized",
                            )
                            if (increaseConsecutiveNumberOfErrors() >= MAX_NUMBER_POST_RETRIES) {
                                verboseLog(
                                    "Error syncing diagnostics file: $error. " +
                                        "This was the final attempt ($MAX_NUMBER_POST_RETRIES). " +
                                        "Deleting diagnostics file without posting.",
                                )
                                resetDiagnosticsStatus()
                            }
                        } else {
                            verboseLog(
                                "Error syncing diagnostics file: $error. " +
                                    "Deleting diagnostics file without retrying.",
                            )
                            resetDiagnosticsStatus()
                        }
                    },
                )
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                verboseLog("Error syncing diagnostics file: $e")
                try {
                    resetDiagnosticsStatus()
                } catch (e: IOException) {
                    verboseLog("Error deleting diagnostics file: $e")
                }
            }
        }
    }

    private fun getEventsToSync(): List<JSONObject> {
        var eventsToSync: List<JSONObject> = emptyList()
        diagnosticsFileHelper.readDiagnosticsFile { stream ->
            eventsToSync = stream.limit(MAX_EVENTS_TO_SYNC_PER_REQUEST).collect(Collectors.toList())
        }
        return eventsToSync
    }

    private fun enqueue(command: () -> Unit) {
        diagnosticsDispatcher.enqueue(command = command)
    }

    private fun clearConsecutiveNumberOfErrors() {
        sharedPreferences.edit().remove(CONSECUTIVE_FAILURES_COUNT_KEY).apply()
    }

    private fun increaseConsecutiveNumberOfErrors(): Int {
        var count = sharedPreferences.getInt(CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        sharedPreferences.edit().putInt(CONSECUTIVE_FAILURES_COUNT_KEY, ++count).apply()
        return count
    }

    private fun resetDiagnosticsStatus() {
        clearConsecutiveNumberOfErrors()
        diagnosticsFileHelper.deleteDiagnosticsFile()
    }
}
