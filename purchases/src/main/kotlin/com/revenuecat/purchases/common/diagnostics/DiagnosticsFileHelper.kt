package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.utils.EventsFileHelper

/**
 * All methods in this file should be executed within the diagnostics thread to ensure there are no threading issues.
 */
internal class DiagnosticsFileHelper(
    private val fileHelper: FileHelper,
) : EventsFileHelper<DiagnosticsEntry>(fileHelper, DIAGNOSTICS_FILE_PATH, null) {
    companion object {
        const val DIAGNOSTICS_FILE_PATH = "RevenueCat/diagnostics/diagnostic_entries.jsonl"
        const val DIAGNOSTICS_FILE_LIMIT_IN_KB = 500
        const val DIAGNOSTICS_FILE_SYNC_LIMIT_IN_KB = 200
    }

    @Synchronized
    fun isDiagnosticsFileTooBig(): Boolean {
        return diagnosticsFileSize() > DIAGNOSTICS_FILE_LIMIT_IN_KB
    }

    @Synchronized
    fun isDiagnosticsFileBigEnoughToSync(): Boolean {
        return diagnosticsFileSize() > DIAGNOSTICS_FILE_SYNC_LIMIT_IN_KB
    }

    private fun diagnosticsFileSize(): Double {
        return fileHelper.fileSizeInKB(DIAGNOSTICS_FILE_PATH)
    }
}
