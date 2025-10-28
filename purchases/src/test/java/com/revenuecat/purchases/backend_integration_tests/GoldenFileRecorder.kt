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
            put("headers", JSONObject(headers))
            put("body", body)
        }
    }

    companion object {
        fun fromJSON(json: JSONObject): RecordedRequest {
            val headers = json.getJSONObject("headers")
            val headerMap = mutableMapOf<String, String>()
            headers.keys().forEach { key ->
                headerMap[key] = headers.getString(key)
            }
            return RecordedRequest(
                url = json.getString("url"),
                method = json.getString("method"),
                headers = headerMap,
                body = if (json.has("body") && !json.isNull("body")) json.getString("body") else null
            )
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
            put("headers", JSONObject(headers))

            // Try to parse body as JSON, otherwise store null
            if (body != null) {
                try {
                    // Try to parse as JSONObject first
                    val jsonObject = JSONObject(body)
                    put("body", jsonObject)
                } catch (e: Exception) {
                    try {
                        // Try to parse as JSONArray
                        val jsonArray = JSONArray(body)
                        put("body", jsonArray)
                    } catch (e2: Exception) {
                        // Not valid JSON, store null
                        put("body", JSONObject.NULL)
                    }
                }
            } else {
                put("body", JSONObject.NULL)
            }
        }
    }

    companion object {
        fun fromJSON(json: JSONObject): RecordedResponse {
            val headers = json.getJSONObject("headers")
            val headerMap = mutableMapOf<String, String>()
            headers.keys().forEach { key ->
                headerMap[key] = headers.getString(key)
            }

            val body = if (json.has("body") && !json.isNull("body")) {
                val bodyValue = json.get("body")
                when (bodyValue) {
                    is JSONObject -> bodyValue.toString(2)
                    is JSONArray -> bodyValue.toString(2)
                    else -> bodyValue.toString()
                }
            } else {
                null
            }

            return RecordedResponse(
                statusCode = json.getInt("statusCode"),
                headers = headerMap,
                body = body
            )
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
    private val headersToIgnore: Set<String> = DEFAULT_HEADERS_TO_IGNORE
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
            body = requestBody
        )
        val response = RecordedResponse(
            statusCode = responseCode,
            headers = filterHeaders(responseHeaders),
            body = responseBody
        )

        recordGoldenFile(sequenceNumber, request, response)
    }

    private fun filterHeaders(headers: Map<String, String>): Map<String, String> {
        return headers.filterKeys { key ->
            // Keep the header only if it doesn't match any of the ignore patterns
            !headerIgnorePatterns.any { pattern -> pattern.matches(key) }
        }
    }

    private fun recordGoldenFile(sequenceNumber: String, request: RecordedRequest, response: RecordedResponse) {
        val requestFile = File(testDirectory, "request_$sequenceNumber.json")
        val responseFile = File(testDirectory, "response_$sequenceNumber.json")

        requestFile.writeText(request.toJSON().toString(2))
        responseFile.writeText(response.toJSON().toString(2))
    }

    /**
     * Resets the request counter. Should be called at the start of each test.
     */
    fun reset() {
        requestCounter = 0
    }
}
