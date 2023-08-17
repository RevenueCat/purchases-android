package com.revenuecat.purchases.common.diagnostics

import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.verboseLog
import org.json.JSONObject
import java.util.stream.Stream

/**
 * All methods in this file should be executed within the diagnostics thread to ensure there are no threading issues.
 */
@RequiresApi(Build.VERSION_CODES.N)
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
    fun readDiagnosticsFile(streamBlock: ((Stream<JSONObject>) -> Unit)) {
        if (fileHelper.fileIsEmpty(DIAGNOSTICS_FILE_PATH)) {
            streamBlock(Stream.empty())
        } else {
            fileHelper.readFilePerLines(DIAGNOSTICS_FILE_PATH) { stream ->
                streamBlock(stream.map { JSONObject(it) })
            }
        }
    }
}
