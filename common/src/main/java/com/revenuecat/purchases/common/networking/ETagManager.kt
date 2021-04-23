package com.revenuecat.purchases.common.networking

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection

private const val SERIALIZATION_NAME_ETAG = "eTag"
private const val SERIALIZATION_NAME_HTTPRESULT = "httpResult"

internal const val ETAG_HEADER_NAME = "X-RevenueCat-ETag"

data class HTTPResultWithETag(
    val eTag: String,
    val httpResult: HTTPResult
) {
    fun serialize(): String {
        return JSONObject().apply {
            put(SERIALIZATION_NAME_ETAG, eTag)
            put(SERIALIZATION_NAME_HTTPRESULT, httpResult.serialize())
        }.toString()
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

typealias ShouldRefreshETags = Boolean

class ETagManager(
    private val prefs: SharedPreferences
) {

    internal fun getETagHeader(
        path: String,
        refreshETag: Boolean = false
    ): Map<String, String> {
        val eTagHeader = ETAG_HEADER_NAME to if (refreshETag) "" else getETag(path)
        return mapOf(eTagHeader)
    }

    internal fun shouldUseCachedVersion(responseCode: Int) = responseCode == RC_HTTP_STATUS_NOT_MODIFIED

    internal fun getStoredResult(path: String): HTTPResult? {
        val storedResult = getStoredResultSavedInSharedPreferences(path)
        return storedResult?.httpResult
    }

    internal fun storeBackendResultIfNoError(
        path: String,
        resultFromBackend: HTTPResult,
        eTagInResponse: String
    ) {
        val responseCode = resultFromBackend.responseCode
        if (responseCode != RC_HTTP_STATUS_NOT_MODIFIED && responseCode < RC_HTTP_STATUS_ERROR) {
            storeResult(path, resultFromBackend, eTagInResponse)
        }
    }

    @Synchronized
    private fun storeResult(
        path: String,
        result: HTTPResult,
        eTag: String
    ) {
        val httpResultWithETag = HTTPResultWithETag(eTag, result)
        prefs.edit().putString(path, httpResultWithETag.serialize()).apply()
    }

    private fun getStoredResultSavedInSharedPreferences(path: String): HTTPResultWithETag? {
        val serializedHTTPResultWithETag = prefs.getString(path, null)
        return serializedHTTPResultWithETag?.let {
            HTTPResultWithETag.deserialize(it)
        }
    }

    private fun getETag(path: String): String {
        return getStoredResultSavedInSharedPreferences(path)?.eTag.orEmpty()
    }

    @Synchronized
    internal fun clearCaches() {
        prefs.edit().clear().apply()
    }

    companion object {
        fun initializeSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(
                "${context.packageName}_preferences_etags",
                Context.MODE_PRIVATE
            )
    }
}

internal fun HttpURLConnection.getETagHeader(): String? = this.getHeaderField(ETAG_HEADER_NAME)
