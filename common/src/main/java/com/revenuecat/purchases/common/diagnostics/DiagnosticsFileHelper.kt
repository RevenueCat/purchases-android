package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.verboseLog
import org.json.JSONObject

/**
 * All methods in this file should be executed within the diagnostics thread to ensure there are no threading issues.
 */
class DiagnosticsFileHelper(
    private val fileHelper: FileHelper,
) {
    companion object {
        const val DIAGNOSTICS_FILE_PATH = "RevenueCat/diagnostics/diagnostic_entries.jsonl"
    }

    @Synchronized
    fun appendEntryToDiagnosticsFile(diagnosticsEntry: DiagnosticsEntry) {
        fileHelper.appendToFile(DIAGNOSTICS_FILE_PATH, diagnosticsEntry.toString() + "\n")
    }

    @Synchronized
    fun deleteOlderDiagnostics(eventsToDeleteCount: Int) {
        fileHelper.removeFirstLinesFromFile(DIAGNOSTICS_FILE_PATH, eventsToDeleteCount)
    }

    @Synchronized
    fun deleteDiagnosticsFile() {
        if (!fileHelper.deleteFile(DIAGNOSTICS_FILE_PATH)) {
            verboseLog("Failed to delete diagnostics file.")
        }
    }

    @Synchronized
    fun readDiagnosticsFile(): List<JSONObject> {
        return if (fileHelper.fileIsEmpty(DIAGNOSTICS_FILE_PATH)) {
            emptyList()
        } else {
            fileHelper.readFilePerLines(DIAGNOSTICS_FILE_PATH).map { JSONObject(it) }
        }
    }
}
