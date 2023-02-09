package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.verboseLog
import org.json.JSONObject

/**
 * All methods in this file should be executed within the diagnostics thread to ensure there are no threading issues.
 */
class DiagnosticsFileHelper(
    private val fileHelper: FileHelper
) {
    companion object {
        const val DIAGNOSTICS_FILE_PATH = "RevenueCat/diagnostics/diagnostic_events.jsonl"
    }

    @Synchronized
    fun appendEventToDiagnosticsFile(diagnosticsEvent: DiagnosticsEvent) {
        fileHelper.appendToFile(DIAGNOSTICS_FILE_PATH, diagnosticsEvent.toString() + "\n")
    }

    @Synchronized
    fun cleanSentDiagnostics(diagnosticsSentCount: Int) {
        fileHelper.removeFirstLinesFromFile(DIAGNOSTICS_FILE_PATH, diagnosticsSentCount)
    }

    @Synchronized
    fun deleteDiagnosticsFile() {
        if (!fileHelper.deleteFile(DIAGNOSTICS_FILE_PATH)) {
            verboseLog("Failed to delete diagnostics file.")
        }
    }

    @Synchronized
    fun diagnosticsFileIsEmpty(): Boolean {
        return fileHelper.fileIsEmpty(DIAGNOSTICS_FILE_PATH)
    }

    @Synchronized
    fun readDiagnosticsFile(): List<JSONObject> {
        return fileHelper.readFilePerLines(DIAGNOSTICS_FILE_PATH).map { JSONObject(it) }
    }
}
