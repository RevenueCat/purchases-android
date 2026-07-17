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
 */
class GoldenFileRecorder(
    private val className: String,
    private val testName: String,
    private val baseDirectory: File,
) : RequestResponseListener {

    private var requestCounter = 0
    private val testDirectory: File
        get() = File(File(baseDirectory, className), testName).also { it.mkdirs() }

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
            headers = requestHeaders,
            body = requestBody,
        )
        val response = RecordedResponse(
            statusCode = responseCode,
            headers = responseHeaders,
            body = responseBody,
        )

        recordGoldenFile(sequenceNumber, request, response)
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
