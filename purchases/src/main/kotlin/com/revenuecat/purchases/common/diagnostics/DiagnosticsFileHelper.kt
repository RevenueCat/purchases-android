package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.utils.DataListener
import org.json.JSONObject

/**
 * All methods in this file should be executed within the diagnostics thread to ensure there are no threading issues.
 */
internal class DiagnosticsFileHelper(
    private val fileHelper: FileHelper,
) {
    companion object {
        const val DIAGNOSTICS_FILE_PATH = "RevenueCat/diagnostics/diagnostic_entries.jsonl"
        const val DIAGNOSTICS_FILE_LIMIT_IN_KB = 500
    }

    @Synchronized
    fun isDiagnosticsFileTooBig(): Boolean {
        return fileHelper.fileSizeInKB(DIAGNOSTICS_FILE_PATH) > DIAGNOSTICS_FILE_LIMIT_IN_KB
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
    fun readDiagnosticsFile(listener: DataListener<JSONObject>) {
        if (fileHelper.fileIsEmpty(DIAGNOSTICS_FILE_PATH)) {
            listener.onComplete()
        } else {
            fileHelper.readFilePerLines(
                DIAGNOSTICS_FILE_PATH,
                object : DataListener<Pair<String, Int>> {
                    override fun onData(data: Pair<String, Int>) {
                        listener.onData(JSONObject(data.first))
                    }

                    override fun onComplete() {
                        listener.onComplete()
                    }
                },
            )
        }
    }
}
