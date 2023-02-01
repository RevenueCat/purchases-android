package com.revenuecat.purchases.common.telemetry

import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.verboseLog
import org.json.JSONObject

/**
 * All methods in this file should be executed within the telemetry thread to ensure there are no threading issues.
 */
class TelemetryFileHelper(
    private val fileHelper: FileHelper
) {
    companion object {
        const val TELEMETRY_FILE_PATH = "RevenueCat/telemetry/telemetry_events.jsonl"
    }

    @Synchronized
    fun appendEventToTelemetryFile(telemetryEvent: TelemetryEvent) {
        fileHelper.appendToFile(TELEMETRY_FILE_PATH, telemetryEvent.toString() + "\n")
    }

    @Synchronized
    fun cleanSentTelemetry(telemetrySentCount: Int) {
        fileHelper.removeFirstLinesFromFile(TELEMETRY_FILE_PATH, telemetrySentCount)
    }

    @Synchronized
    fun deleteTelemetryFile() {
        if (!fileHelper.deleteFile(TELEMETRY_FILE_PATH)) {
            verboseLog("Failed to delete telemetry file.")
        }
    }

    @Synchronized
    fun telemetryFileIsEmpty(): Boolean {
        return fileHelper.fileIsEmpty(TELEMETRY_FILE_PATH)
    }

    @Synchronized
    fun readTelemetryFile(): List<JSONObject> {
        return fileHelper.readFilePerLines(TELEMETRY_FILE_PATH).map { JSONObject(it) }
    }
}
