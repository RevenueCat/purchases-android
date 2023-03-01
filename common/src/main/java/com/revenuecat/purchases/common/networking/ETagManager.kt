package com.revenuecat.purchases.common.networking

import android.content.Context
import android.content.SharedPreferences
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.NetworkStrings
import org.json.JSONObject

private const val SERIALIZATION_NAME_ETAG = "eTag"
private const val SERIALIZATION_NAME_HTTPRESULT = "httpResult"

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

class ETagManager(
    private val prefs: SharedPreferences
) {

    internal fun getETagHeader(
        path: String,
        refreshETag: Boolean = false
    ): Map<String, String> {
        val eTagHeader = HTTPRequest.ETAG_HEADER_NAME to if (refreshETag) "" else getETag(path)
        return mapOf(eTagHeader)
    }

    @Suppress("LongParameterList")
    internal fun getHTTPResultFromCacheOrBackend(
        responseCode: Int,
        payload: String,
        eTagHeader: String?,
        urlPathWithVersion: String,
        refreshETag: Boolean,
        verificationStatus: HTTPResult.VerificationStatus
    ): HTTPResult? {
        val resultFromBackend = HTTPResult(responseCode, payload, HTTPResult.Origin.BACKEND, verificationStatus)
        eTagHeader?.let { eTagInResponse ->
            if (shouldUseCachedVersion(responseCode)) {
                val storedResult = getStoredResult(urlPathWithVersion)
                return storedResult
                    ?: if (refreshETag) {
                        log(LogIntent.WARNING, NetworkStrings.ETAG_CALL_ALREADY_RETRIED.format(resultFromBackend))
                        resultFromBackend
                    } else {
                        null
                    }
            }

            storeBackendResultIfNoError(urlPathWithVersion, resultFromBackend, eTagInResponse)
        }
        return resultFromBackend
    }

    internal fun shouldUseCachedVersion(responseCode: Int) = responseCode == RCHTTPStatusCodes.NOT_MODIFIED

    internal fun getStoredResult(path: String): HTTPResult? {
        val storedResult = getStoredResultSavedInSharedPreferences(path)
        return storedResult?.httpResult
    }

    internal fun storeBackendResultIfNoError(
        path: String,
        resultFromBackend: HTTPResult,
        eTagInResponse: String
    ) {
        if (shouldStoreBackendResult(resultFromBackend)) {
            storeResult(path, resultFromBackend, eTagInResponse)
        }
    }

    @Synchronized
    internal fun clearCaches() {
        prefs.edit().clear().apply()
    }

    @Synchronized
    private fun storeResult(
        path: String,
        result: HTTPResult,
        eTag: String
    ) {
        val cacheResult = result.copy(origin = HTTPResult.Origin.CACHE)
        val httpResultWithETag = HTTPResultWithETag(eTag, cacheResult)
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

    private fun shouldStoreBackendResult(resultFromBackend: HTTPResult): Boolean {
        val responseCode = resultFromBackend.responseCode
        return responseCode != RCHTTPStatusCodes.NOT_MODIFIED &&
            responseCode < RCHTTPStatusCodes.ERROR &&
            resultFromBackend.verificationStatus != HTTPResult.VerificationStatus.ERROR
    }

    companion object {
        fun initializeSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(
                "${context.packageName}_preferences_etags",
                Context.MODE_PRIVATE
            )
    }
}
