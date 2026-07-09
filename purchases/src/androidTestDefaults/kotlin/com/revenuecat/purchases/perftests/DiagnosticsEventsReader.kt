package com.revenuecat.purchases.perftests

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * Incrementally reads the SDK's diagnostics file (`files/RevenueCat/diagnostics/diagnostic_entries.jsonl`,
 * written by `DiagnosticsTracker` when the SDK is configured with `diagnosticsEnabled(true)`) and extracts
 * `http_request_performed` events. This reuses the SDK's own telemetry for per-endpoint request timings
 * (`get_offerings`, `remote_config`, ...) instead of duplicating instrumentation.
 *
 * The reader tracks how many lines it has already consumed so each harvest returns only the events since the
 * previous one. If the file shrinks between harvests (the `DiagnosticsSynchronizer` synced and truncated it),
 * the offset resets and the remaining lines are treated as new.
 *
 * Not captured here: blob downloads (they bypass `HTTPClient`, so they emit no diagnostics events). Count
 * those at the network shaping layer.
 */
internal class DiagnosticsEventsReader(context: Context) {

    private val diagnosticsFile = File(context.filesDir, DIAGNOSTICS_FILE_PATH)
    private var consumedLines = 0

    /** Forgets progress so the next harvest reads from the top (used when the file is cleared between runs). */
    fun reset() {
        consumedLines = 0
    }

    fun harvestNewHttpEvents(): List<HttpRequestEvent> {
        if (!diagnosticsFile.exists()) return emptyList()
        val lines = diagnosticsFile.readLines()
        if (lines.size < consumedLines) consumedLines = 0
        val newLines = lines.drop(consumedLines)
        consumedLines = lines.size
        return newLines.mapNotNull(::parseHttpEvent)
    }

    private fun parseHttpEvent(line: String): HttpRequestEvent? {
        val entry = try {
            JSONObject(line)
        } catch (_: JSONException) {
            null
        }
        if (entry == null || entry.optString("name") != HTTP_REQUEST_PERFORMED_EVENT_NAME) return null
        val properties = entry.optJSONObject("properties") ?: JSONObject()
        return HttpRequestEvent(
            endpointName = properties.optStringOrNull("endpoint_name"),
            responseTimeMillis = if (properties.has("response_time_millis")) {
                properties.optLong("response_time_millis")
            } else {
                null
            },
            responseCode = if (properties.has("response_code")) properties.optInt("response_code") else null,
            successful = if (properties.has("successful")) properties.optBoolean("successful") else null,
            etagHit = if (properties.has("etag_hit")) properties.optBoolean("etag_hit") else null,
            isRetry = if (properties.has("is_retry")) properties.optBoolean("is_retry") else null,
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private companion object {
        // Mirrors DiagnosticsFileHelper.DIAGNOSTICS_FILE_PATH.
        private const val DIAGNOSTICS_FILE_PATH = "RevenueCat/diagnostics/diagnostic_entries.jsonl"
        private const val HTTP_REQUEST_PERFORMED_EVENT_NAME = "http_request_performed"
    }
}
