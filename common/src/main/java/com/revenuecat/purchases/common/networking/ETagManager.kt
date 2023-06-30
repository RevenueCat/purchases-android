package com.revenuecat.purchases.common.networking

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.NetworkStrings
import org.json.JSONObject
import java.util.Date

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
data class ETagData(
    val eTag: String,
    val lastRefreshTime: Date?,
)

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
data class HTTPResultWithETag(
    val eTagData: ETagData,
    val httpResult: HTTPResult,
) {
    fun serialize(): String {
        return JSONObject().apply {
            put(SERIALIZATION_NAME_ETAG, eTagData.eTag)
            eTagData.lastRefreshTime?.let { put(SERIALIZATION_NAME_LAST_REFRESH_TIME, it.time) }
            put(SERIALIZATION_NAME_HTTPRESULT, httpResult.serialize())
        }.toString()
    }

    companion object {
        private const val SERIALIZATION_NAME_ETAG = "eTag"
        private const val SERIALIZATION_NAME_LAST_REFRESH_TIME = "lastRefreshTime"
        private const val SERIALIZATION_NAME_HTTPRESULT = "httpResult"

        fun deserialize(serialized: String): HTTPResultWithETag {
            val jsonObject = JSONObject(serialized)
            val eTag = jsonObject.getString(SERIALIZATION_NAME_ETAG)
            val lastRefreshTime = jsonObject.optLong(SERIALIZATION_NAME_LAST_REFRESH_TIME, -1L)
                .takeIf { it != -1L }
                ?.let { Date(it) }
            val serializedHTTPResult = jsonObject.getString(SERIALIZATION_NAME_HTTPRESULT)
            return HTTPResultWithETag(ETagData(eTag, lastRefreshTime), HTTPResult.deserialize(serializedHTTPResult))
        }
    }
}

class ETagManager(
    private val prefs: SharedPreferences,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {

    internal fun getETagHeaders(
        path: String,
        verificationRequested: Boolean,
        refreshETag: Boolean = false,
    ): Map<String, String?> {
        val storedResult = if (refreshETag) null else getStoredResultSavedInSharedPreferences(path)
        val eTagData = storedResult?.eTagData?.takeIf { shouldUseETag(storedResult, verificationRequested) }
        return mapOf(
            HTTPRequest.ETAG_HEADER_NAME to eTagData?.eTag.orEmpty(),
            HTTPRequest.ETAG_LAST_REFRESH_NAME to eTagData?.lastRefreshTime?.time?.toString(),
        )
    }

    @Suppress("LongParameterList")
    internal fun getHTTPResultFromCacheOrBackend(
        responseCode: Int,
        payload: String,
        eTagHeader: String?,
        urlPathWithVersion: String,
        refreshETag: Boolean,
        requestDate: Date?,
        verificationResult: VerificationResult,
    ): HTTPResult? {
        val resultFromBackend = HTTPResult(
            responseCode,
            payload,
            HTTPResult.Origin.BACKEND,
            requestDate,
            verificationResult,
        )
        eTagHeader?.let { eTagInResponse ->
            if (shouldUseCachedVersion(responseCode)) {
                val storedResult = getStoredResult(urlPathWithVersion)?.let { storedResult ->
                    storedResult.copy(
                        // This assumes we won't store verification failures in the cache and we will clear the cache
                        // when enabling verification.
                        verificationResult = verificationResult,
                        requestDate = requestDate ?: storedResult.requestDate,
                    )
                }
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
        eTagInResponse: String,
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
        eTag: String,
    ) {
        val cacheResult = result.copy(origin = HTTPResult.Origin.CACHE)
        val eTagData = ETagData(eTag, dateProvider.now)
        val httpResultWithETag = HTTPResultWithETag(eTagData, cacheResult)
        prefs.edit().putString(path, httpResultWithETag.serialize()).apply()
    }

    private fun getStoredResultSavedInSharedPreferences(path: String): HTTPResultWithETag? {
        val serializedHTTPResultWithETag = prefs.getString(path, null)
        return serializedHTTPResultWithETag?.let {
            HTTPResultWithETag.deserialize(it)
        }
    }

    private fun shouldStoreBackendResult(resultFromBackend: HTTPResult): Boolean {
        val responseCode = resultFromBackend.responseCode
        return responseCode != RCHTTPStatusCodes.NOT_MODIFIED &&
            responseCode < RCHTTPStatusCodes.ERROR &&
            resultFromBackend.verificationResult != VerificationResult.FAILED
    }

    private fun shouldUseETag(storedResult: HTTPResultWithETag, verificationRequested: Boolean): Boolean {
        return when (storedResult.httpResult.verificationResult) {
            VerificationResult.VERIFIED -> true
            VerificationResult.NOT_REQUESTED -> !verificationRequested
            // Should never happen since we don't store these verification results in the cache
            VerificationResult.FAILED, VerificationResult.VERIFIED_ON_DEVICE -> false
        }
    }

    companion object {
        fun initializeSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(
                "${context.packageName}_preferences_etags",
                Context.MODE_PRIVATE,
            )
    }
}
