package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.common.RequestResponseListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Data class representing a recorded HTTP request in the golden file format.
 */
data class RecordedRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val body: String?
) {
    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("url", url)
            put("method", method)
            put("headers", sortJsonObject(JSONObject(headers)))

            // Try to parse body as JSON, otherwise store null
            if (body != null) {
                val jsonObject = sortJsonObject(JSONObject(body))
                put("body", jsonObject)
            } else {
                put("body", JSONObject.NULL)
            }
        }
    }
}

/**
 * Data class representing a recorded HTTP response in the golden file format.
 */
data class RecordedResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String?
) {
    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("statusCode", statusCode)
            put("headers", sortJsonObject(JSONObject(headers)))

            // Try to parse body as JSON, otherwise store null
            if (body != null) {
                try {
                    // Try to parse as JSONObject first
                    val jsonObject = sortJsonObject(JSONObject(body))
                    put("body", jsonObject)
                } catch (_: Exception) {
                    // Not valid JSON, store null
                    put("body", JSONObject.NULL)
                }
            } else {
                put("body", JSONObject.NULL)
            }
        }
    }
}

/**
 * Sorts the keys of a JSONObject alphabetically.
 * Returns a new JSONObject with keys in alphabetical order.
 */
private fun sortJsonObject(json: JSONObject): JSONObject {
    val sortedKeys = json.keys().asSequence().toList().sorted()
    return JSONObject().apply {
        sortedKeys.forEach { key ->
            put(key, json.get(key))
        }
    }
}

/**
 * Golden file recorder that implements RequestResponseListener to record
 * HTTP requests and responses for backend integration tests.
 * Verification is done through git version control.
 */
class GoldenFileRecorder(
    private val className: String,
    private val testName: String,
    private val baseDirectory: File,
    headersToIgnore: Set<String> = DEFAULT_HEADERS_TO_IGNORE,
    private val bodyFieldsToIgnore: Set<String> = DEFAULT_BODY_FIELDS_TO_IGNORE
) : RequestResponseListener {

    companion object {
        /**
         * Default headers to ignore during recording as they contain dynamic values.
         * Supports regex patterns for flexible matching.
         * Examples:
         * - "X-Nonce" - exact match (case-insensitive)
         * - "X-Amz-.*" - regex pattern to match all AWS headers
         */
        val DEFAULT_HEADERS_TO_IGNORE = setOf(
            "X-Nonce",
            "X-Request-Time",
            "Date",
            "X-RevenueCat-Request-Time",
            "request-time",
            "date",
            "X-Amz-.*",  // Filter all AWS headers (e.g., X-Amz-Date, X-Amz-Request-Id)
            "X-Amzn-.*",
            "X-Request-Id",
            "Via",
            "X-Signature",
            "X-Revenuecat-Etag",
            "Last-Modified",
            "CF-Ray",
            "x-envoy-upstream-service-time",
            "Age",
            "x-revenuecat-data-source",
            "CF-Cache-Status",
            "Cache-Control",
            "Vary",
            "Authorization",
            "X-Version",
            "Server-Timing",
            "Accept-Ranges",
            "X-Cache",
            "Alt-Svc",
        )

        /**
         * Default body fields to ignore during recording as they contain dynamic values.
         * Supports path syntax for nested fields and array elements:
         * - "field_name" - top-level field
         * - "parent > child" - nested field
         * - "array > \[fieldName\]" - field within each array element
         */
        val DEFAULT_BODY_FIELDS_TO_IGNORE = setOf(
            "request_date",
            "request_date_ms",
            "events > [id]",
            "events > [app_user_id]",
            "events > [session_id]",
            "events > [app_session_id]",
            "events > [offering_id]",
            "events > [timestamp]",
        )
    }

    private var requestCounter = 0
    private val testDirectory: File
        get() = File(File(baseDirectory, className), testName).also { it.mkdirs() }

    // Compile regex patterns once for performance
    private val headerIgnorePatterns = headersToIgnore.map { pattern ->
        Regex(pattern, RegexOption.IGNORE_CASE)
    }

    override fun onRequestResponse(
        url: String,
        method: String,
        requestHeaders: Map<String, String>,
        requestBody: String?,
        responseCode: Int,
        responseHeaders: Map<String, String>,
        responseBody: String
    ) {
        requestCounter++
        val sequenceNumber = String.format("%03d", requestCounter)

        val request = RecordedRequest(
            url = url,
            method = method,
            headers = filterHeaders(requestHeaders),
            body = filterBody(requestBody)
        )
        val response = RecordedResponse(
            statusCode = responseCode,
            headers = filterHeaders(responseHeaders),
            body = filterBody(responseBody)
        )

        recordGoldenFile(sequenceNumber, request, response)
    }

    private fun filterHeaders(headers: Map<String, String>): Map<String, String> {
        return headers.filterKeys { key ->
            // Keep the header only if it doesn't match any of the ignore patterns
            !headerIgnorePatterns.any { pattern -> pattern.matches(key) }
        }
    }

    private fun filterBody(body: String?): String? {
        if (body == null || bodyFieldsToIgnore.isEmpty()) {
            return body
        }

        return try {
            val jsonObject = JSONObject(body)
            bodyFieldsToIgnore.forEach { pathSpec ->
                applyFieldFilter(jsonObject, pathSpec)
            }
            jsonObject.toString()
        } catch (@Suppress("SwallowedException") _: Exception) {
            // Not a JSON object or parsing failed, return as-is
            body
        }
    }

    /**
     * Applies filtering to a field specified by a path.
     * Path syntax:
     * - "field" - top-level field
     * - "parent > child" - nested field
     * - "parent > \[field\]" - field in each element of parent array
     */
    private fun applyFieldFilter(jsonObject: JSONObject, pathSpec: String) {
        val parts = pathSpec.split(">").map { it.trim() }
        navigateAndFilter(jsonObject, parts, 0)
    }

    private fun navigateAndFilter(current: Any, parts: List<String>, index: Int) {
        if (index >= parts.size) return

        val part = parts[index]
        val isLast = index == parts.size - 1

        // Check if this part is an array field accessor like "[fieldName]"
        if (part.startsWith("[") && part.endsWith("]")) {
            val fieldName = part.substring(1, part.length - 1)
            if (current is JSONArray) {
                // Apply to each element in the array
                for (i in 0 until current.length()) {
                    val element = current.get(i)
                    if (element is JSONObject && element.has(fieldName)) {
                        if (isLast) {
                            element.put(fieldName, "TEST_STATIC_VALUE")
                        } else {
                            navigateAndFilter(element.get(fieldName), parts, index + 1)
                        }
                    }
                }
            }
        } else {
            // Regular field access
            if (current is JSONObject && current.has(part)) {
                if (isLast) {
                    current.put(part, "TEST_STATIC_VALUE")
                } else {
                    navigateAndFilter(current.get(part), parts, index + 1)
                }
            }
        }
    }

    private fun recordGoldenFile(sequenceNumber: String, request: RecordedRequest, response: RecordedResponse) {
        val requestFile = File(testDirectory, "request_$sequenceNumber.json")
        val responseFile = File(testDirectory, "response_$sequenceNumber.json")

        requestFile.writeText(request.toJSON().toString(2))
        responseFile.writeText(response.toJSON().toString(2))
    }
}
