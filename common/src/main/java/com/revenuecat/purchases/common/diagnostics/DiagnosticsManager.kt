package com.revenuecat.purchases.common.diagnostics

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.verboseLog
import java.io.IOException

/**
 * This class is the entry point to perform all diagnostics related operations. All operations will be executed
 * in a background thread.
 *
 * If syncing diagnostics fails multiple times, we will delete any stored diagnostics data and start again.
 */
class DiagnosticsManager(
    private val diagnosticsFileHelper: DiagnosticsFileHelper,
    private val diagnosticsAnonymizer: DiagnosticsAnonymizer,
    private val backend: Backend,
    private val diagnosticsDispatcher: Dispatcher,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val CONSECUTIVE_FAILURES_COUNT_KEY = "consecutive_failures_count"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MAX_NUMBER_POST_RETRIES = 3

        fun initializeSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(
                "com_revenuecat_purchases_${context.packageName}_preferences_diagnostics",
                Context.MODE_PRIVATE
            )
    }

    fun syncDiagnosticsFileIfNeeded() {
        enqueue {
            try {
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
                        clearConsecutiveNumberOfErrors()
                        diagnosticsFileHelper.cleanSentDiagnostics(diagnosticsCount)
                    },
                    onErrorHandler = { error, shouldRetry ->
                        if (shouldRetry) {
                            verboseLog("Error syncing diagnostics file: $error. " +
                                "Will retry the next time the SDK is initialized")
                            if (increaseConsecutiveNumberOfErrors() >= MAX_NUMBER_POST_RETRIES) {
                                verboseLog("Error syncing diagnostics file: $error. " +
                                    "This was the final attempt ($MAX_NUMBER_POST_RETRIES). " +
                                    "Deleting diagnostics file without posting.")
                                resetDiagnosticsStatus()
                            }
                        } else {
                            verboseLog("Error syncing diagnostics file: $error. " +
                                "Deleting diagnostics file without retrying.")
                            resetDiagnosticsStatus()
                        }
                    }
                )
            } catch (e: IOException) {
                verboseLog("Error syncing diagnostics file: $e")
                try {
                    resetDiagnosticsStatus()
                } catch (e: IOException) {
                    verboseLog("Error deleting diagnostics file: $e")
                }
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
