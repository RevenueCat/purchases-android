package com.revenuecat.purchases.common.networking

import android.content.Context
import android.content.SharedPreferences
import com.revenuecat.purchases.common.sha1
import org.json.JSONObject
import java.net.HttpURLConnection

private const val NOT_MODIFIED_RESPONSE_CODE = 304
private const val INTERNAL_SERVER_ERROR_RESPONSE_CODE = 500
private typealias RequestHash = String

private const val SERIALIZATION_NAME_ETAG = "eTag"
private const val SERIALIZATION_NAME_HTTPRESULT = "httpResult"

internal const val ETAG_HEADER_NAME = "X-RevenueCat-ETag"

data class HTTPResultWithETag(
    val eTag: String,
    val httpResult: HTTPResult
) {
    fun serialize(): String {
        val jsonObject = JSONObject()
        jsonObject.put(SERIALIZATION_NAME_ETAG, eTag)
        jsonObject.put(SERIALIZATION_NAME_HTTPRESULT, httpResult.serialize())
        return jsonObject.toString()
    }

    companion object {
        fun deserialize(serialized: String): HTTPResultWithETag {
            val jsonObject = JSONObject(serialized)
            val eTag = jsonObject.getString(SERIALIZATION_NAME_ETAG)
            val serializedHTTPResult = jsonObject.getString(SERIALIZATION_NAME_HTTPRESULT)
            return HTTPResultWithETag(eTag, HTTPResult.deserialize(serializedHTTPResult))
        }
    }
}

class ETagManager(
    private val prefs: SharedPreferences
) {
    private var hashesByHttpRequest: Map<HTTPRequest, RequestHash> = emptyMap()

    internal fun addETagHeaderToRequest(
        httpRequest: HTTPRequest
    ): HTTPRequest {
        val eTagHeader = ETAG_HEADER_NAME to getETag(httpRequest)
        val updatedHeaders = httpRequest.headers + mapOf(eTagHeader)
        return HTTPRequest(httpRequest.fullURL, updatedHeaders, httpRequest.body)
    }

    internal fun processResponse(
        httpRequest: HTTPRequest,
        connection: HttpURLConnection,
        result: HTTPResult
    ): HTTPResult {
        val eTagInResponse = connection.getHeaderField(ETAG_HEADER_NAME)
        if (eTagInResponse != null) {
            if (result.responseCode == NOT_MODIFIED_RESPONSE_CODE) {
                getStoredResult(httpRequest)?.let { (_, cachedResult) ->
                    return cachedResult
                }
            } else if (result.responseCode < INTERNAL_SERVER_ERROR_RESPONSE_CODE) {
                storeResult(httpRequest, result, eTagInResponse)
            }
        }

        return result
    }

    @Synchronized
    private fun storeResult(
        request: HTTPRequest,
        result: HTTPResult,
        eTag: String
    ) {
        val requestHash = getOrCalculateAndSaveHTTPRequestHash(request)
        val httpResultWithETag = HTTPResultWithETag(eTag, result)
        prefs.edit().putString(requestHash, httpResultWithETag.serialize()).apply()
    }

    private fun getStoredResult(httpRequest: HTTPRequest): HTTPResultWithETag? {
        val requestHash = getOrCalculateAndSaveHTTPRequestHash(httpRequest)
        val serializedHTTPResultWithETag = prefs.getString(requestHash, null)
        return serializedHTTPResultWithETag?.let {
            HTTPResultWithETag.deserialize(it)
        }
    }

    private fun getETag(httpRequest: HTTPRequest): String {
        return getStoredResult(httpRequest)?.eTag ?: ""
    }

    @Synchronized
    internal fun getOrCalculateAndSaveHTTPRequestHash(httpRequest: HTTPRequest): RequestHash {
        return hashesByHttpRequest[httpRequest] ?: calculateAndSaveHTTPRequestHash(httpRequest)
    }

    private fun calculateAndSaveHTTPRequestHash(httpRequest: HTTPRequest): String {
        val sha1 = (httpRequest.fullURL.toString()).sha1()
        val updatedHashesMap = hashesByHttpRequest.toMutableMap().also {
            it[httpRequest] = sha1
        }
        hashesByHttpRequest = updatedHashesMap
        return sha1
    }

    @Synchronized
    internal fun clearCaches() {
        hashesByHttpRequest = emptyMap()
        prefs.edit().clear().apply()
    }

    companion object {
        fun initializeSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(
                context.packageName + "_preferences_etags",
                Context.MODE_PRIVATE
            )
    }
}
