package com.revenuecat.purchases.common.networking

import android.content.Context
import android.content.SharedPreferences
import com.revenuecat.purchases.common.sha1
import org.json.JSONObject

private const val NOT_MODIFIED_RESPONSE_CODE = 304
private const val INTERNAL_SERVER_ERROR_RESPONSE_CODE = 500
private typealias RequestHash = String

data class HTTPResultWithETag(
    val eTag: String,
    val httpResult: HTTPResult
) {
    fun serialize(): String {
        val jsonObject = JSONObject()
        jsonObject.put("eTag", eTag)
        jsonObject.put("httpResult", httpResult.serialize())
        return jsonObject.toString()
    }

    companion object {
        fun deserialize(serialized: String): HTTPResultWithETag {
            val jsonObject = JSONObject(serialized)
            val eTag = jsonObject.getString("eTag")
            val serializedHTTPResult = jsonObject.getString("httpResult")
            return HTTPResultWithETag(eTag, HTTPResult.deserialize(serializedHTTPResult))
        }
    }
}

class ETagManager(
    private val prefs: SharedPreferences
) {
    private var hashes: Map<HTTPRequest, RequestHash> = emptyMap()

    internal fun addETagHeaderToRequest(
        httpRequestWithoutETagHeader: HTTPRequest
    ): HTTPRequest {
        val eTagHeader = "X-RevenueCat-ETag" to getETag(httpRequestWithoutETagHeader)
        val updatedHeaders = httpRequestWithoutETagHeader.headers + mapOf(eTagHeader)
        return HTTPRequest(httpRequestWithoutETagHeader.fullURL, updatedHeaders, httpRequestWithoutETagHeader.body)
    }

    internal fun processResponse(
        httpRequest: HTTPRequest,
        eTagInResponse: String?,
        result: HTTPResult
    ): HTTPResult {
        if (eTagInResponse != null) {
            if (result.responseCode == NOT_MODIFIED_RESPONSE_CODE) {
                getStoredResult(httpRequest)?.let { (_, cachedResult) ->
                    return cachedResult
                }
            } else if (result.responseCode != INTERNAL_SERVER_ERROR_RESPONSE_CODE) {
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
        val requestHash = getHTTPRequestHash(request)
        val httpResultWithETag = HTTPResultWithETag(eTag, result)
        prefs.edit().putString(requestHash, httpResultWithETag.serialize()).apply()
    }

    private fun getStoredResult(httpRequest: HTTPRequest): HTTPResultWithETag? {
        val requestHash = getHTTPRequestHash(httpRequest)
        val serializedHTTPResultWithETag = prefs.getString(requestHash, null)
        return serializedHTTPResultWithETag?.let {
            HTTPResultWithETag.deserialize(it)
        }
    }

    private fun getETag(httpRequest: HTTPRequest): String {
        return getStoredResult(httpRequest)?.eTag ?: ""
    }

    @Synchronized
    internal fun getHTTPRequestHash(httpRequest: HTTPRequest): RequestHash {
        hashes[httpRequest]?.let { return it }

        val sha1 = (httpRequest.fullURL.toString()).sha1()
        val updatedHashesMap = hashes.toMutableMap().also {
            it[httpRequest] = sha1
        }
        hashes - updatedHashesMap
        return sha1
    }

    @Synchronized
    internal fun clearCaches() {
        hashes = emptyMap()
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
