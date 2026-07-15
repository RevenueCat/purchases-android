package com.revenuecat.purchases.common.networking

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.NetworkStrings
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal data class ETagData(
    val eTag: String,
    val lastRefreshTime: Date?,
)

/**
 * Legacy (pre-split) cache entry format: metadata and payload combined in one JSON string, with the
 * payload double-escaped. Retained only to read-and-migrate entries written by older SDK versions —
 * new entries are written as [ETagCacheMetadata] + a verbatim payload key.
 */
@OptIn(InternalRevenueCatAPI::class)
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal data class HTTPResultWithETag(
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

/**
 * Everything the ETag cache persists about a response except its payload. Stored as a small flat JSON
 * under the URL key, while the payload is stored verbatim under a separate key ([ETagManager.payloadKey])
 * so caching a large response never re-encodes a string we already hold in memory
 * (https://github.com/RevenueCat/purchases-android/issues/3628).
 *
 * `origin` is intentionally not persisted: stored results always read back as [HTTPResult.Origin.CACHE].
 *
 * Downgrade note: older SDK versions cannot parse this format — after a downgrade, reads of split
 * entries fail (as request errors, not crashes) and do not self-heal until the cache is cleared
 * (e.g. identity change) since the failure happens before any response could overwrite the entry.
 * Accepted tradeoff for keeping the URL as the metadata key; see PR #3774.
 */
@OptIn(InternalRevenueCatAPI::class)
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal data class ETagCacheMetadata(
    val eTagData: ETagData,
    val responseCode: Int,
    val requestDate: Date?,
    val verificationResult: VerificationResult,
    val isLoadShedderResponse: Boolean,
    val isFallbackURL: Boolean,
) {
    fun serialize(): String {
        return JSONObject().apply {
            put(SERIALIZATION_NAME_ETAG, eTagData.eTag)
            eTagData.lastRefreshTime?.let { put(SERIALIZATION_NAME_LAST_REFRESH_TIME, it.time) }
            put(SERIALIZATION_NAME_RESPONSE_CODE, responseCode)
            requestDate?.let { put(SERIALIZATION_NAME_REQUEST_DATE, it.time) }
            put(SERIALIZATION_NAME_VERIFICATION_RESULT, verificationResult.name)
            put(SERIALIZATION_NAME_IS_LOAD_SHEDDER_RESPONSE, isLoadShedderResponse)
            put(SERIALIZATION_NAME_IS_FALLBACK_URL, isFallbackURL)
        }.toString()
    }

    fun toHTTPResult(payload: String): HTTPResult {
        return HTTPResult(
            responseCode,
            payload,
            HTTPResult.Origin.CACHE,
            requestDate,
            verificationResult,
            isLoadShedderResponse,
            isFallbackURL,
        )
    }

    companion object {
        private const val SERIALIZATION_NAME_ETAG = "eTag"
        private const val SERIALIZATION_NAME_LAST_REFRESH_TIME = "lastRefreshTime"
        private const val SERIALIZATION_NAME_RESPONSE_CODE = "responseCode"
        private const val SERIALIZATION_NAME_REQUEST_DATE = "requestDate"
        private const val SERIALIZATION_NAME_VERIFICATION_RESULT = "verificationResult"
        private const val SERIALIZATION_NAME_IS_LOAD_SHEDDER_RESPONSE = "isLoadShedderResponse"
        private const val SERIALIZATION_NAME_IS_FALLBACK_URL = "isFallbackURL"

        fun fromResult(result: HTTPResult, eTagData: ETagData): ETagCacheMetadata {
            return ETagCacheMetadata(
                eTagData = eTagData,
                responseCode = result.responseCode,
                requestDate = result.requestDate,
                verificationResult = result.verificationResult,
                isLoadShedderResponse = result.isLoadShedderResponse,
                isFallbackURL = result.isFallbackURL,
            )
        }

        /**
         * Returns null when [serialized] is not the split-format metadata JSON: a legacy combined entry
         * (top-level "httpResult" string instead of "responseCode") or corrupt data. Callers fall back
         * to legacy migration.
         */
        @Suppress("SwallowedException", "ReturnCount")
        fun deserialize(serialized: String): ETagCacheMetadata? {
            return try {
                val jsonObject = JSONObject(serialized)
                if (!jsonObject.has(SERIALIZATION_NAME_RESPONSE_CODE)) {
                    return null
                }
                val lastRefreshTime = jsonObject.optLong(SERIALIZATION_NAME_LAST_REFRESH_TIME, -1L)
                    .takeIf { it != -1L }
                    ?.let { Date(it) }
                val requestDate = jsonObject.optLong(SERIALIZATION_NAME_REQUEST_DATE, -1L)
                    .takeIf { it != -1L }
                    ?.let { Date(it) }
                val verificationResult = if (jsonObject.has(SERIALIZATION_NAME_VERIFICATION_RESULT)) {
                    VerificationResult.valueOf(jsonObject.getString(SERIALIZATION_NAME_VERIFICATION_RESULT))
                } else {
                    VerificationResult.NOT_REQUESTED
                }
                ETagCacheMetadata(
                    eTagData = ETagData(jsonObject.getString(SERIALIZATION_NAME_ETAG), lastRefreshTime),
                    responseCode = jsonObject.getInt(SERIALIZATION_NAME_RESPONSE_CODE),
                    requestDate = requestDate,
                    verificationResult = verificationResult,
                    isLoadShedderResponse = jsonObject.optBoolean(SERIALIZATION_NAME_IS_LOAD_SHEDDER_RESPONSE, false),
                    isFallbackURL = jsonObject.optBoolean(SERIALIZATION_NAME_IS_FALLBACK_URL, false),
                )
            } catch (e: JSONException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}

@OptIn(InternalRevenueCatAPI::class)
@Suppress("TooManyFunctions")
internal class ETagManager(
    context: Context,
    private val prefs: Lazy<SharedPreferences> = lazy { initializeSharedPreferences(context) },
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {

    internal fun getETagHeaders(
        urlString: String,
        verificationRequested: Boolean,
        refreshETag: Boolean = false,
    ): Map<String, String?> {
        val metadata = if (refreshETag) null else getStoredMetadata(urlString)
        val eTagData = metadata?.eTagData?.takeIf { shouldUseETag(metadata.verificationResult, verificationRequested) }
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
        urlString: String,
        refreshETag: Boolean,
        requestDate: Date?,
        verificationResult: VerificationResult,
        isLoadShedderResponse: Boolean,
        isFallbackURL: Boolean,
    ): HTTPResult? {
        val resultFromBackend = HTTPResult(
            responseCode,
            payload,
            HTTPResult.Origin.BACKEND,
            requestDate,
            verificationResult,
            isLoadShedderResponse,
            isFallbackURL,
        )
        eTagHeader?.let { eTagInResponse ->
            if (shouldUseCachedVersion(responseCode)) {
                val storedResult = getStoredResult(urlString)?.let { storedResult ->
                    storedResult.copy(
                        // This assumes we won't store verification failures in the cache and we will clear the cache
                        // when enabling verification.
                        verificationResult = verificationResult,
                        requestDate = requestDate ?: storedResult.requestDate,
                    )
                }
                return storedResult
                    ?: if (refreshETag) {
                        log(LogIntent.WARNING) { NetworkStrings.ETAG_CALL_ALREADY_RETRIED.format(resultFromBackend) }
                        resultFromBackend
                    } else {
                        null
                    }
            }

            storeBackendResultIfNoError(urlString, resultFromBackend, eTagInResponse)
        }
        return resultFromBackend
    }

    internal fun shouldUseCachedVersion(responseCode: Int) = responseCode == RCHTTPStatusCodes.NOT_MODIFIED

    @Suppress("ReturnCount")
    internal fun getStoredResult(urlString: String): HTTPResult? {
        val serialized = prefs.value.getString(urlString, null) ?: return null
        val metadata = ETagCacheMetadata.deserialize(serialized)
        if (metadata != null) {
            // Read without the monitor: a concurrent storeResult() landing between the two getString calls can
            // pair the old metadata with the new payload for this one read. The window is nanoseconds, requires
            // concurrent same-URL traffic, and the worst case is one response served with the previous entry's
            // metadata fields — accepted to keep reads lock-free.
            val payload = prefs.value.getString(payloadKey(urlString), null) ?: return null
            return metadata.toHTTPResult(payload)
        }
        return migrateLegacyEntry(urlString, serialized)?.httpResult
    }

    internal fun storeBackendResultIfNoError(
        urlString: String,
        resultFromBackend: HTTPResult,
        eTagInResponse: String,
    ) {
        if (shouldStoreBackendResult(resultFromBackend)) {
            storeResult(urlString, resultFromBackend, eTagInResponse)
        }
    }

    @Synchronized
    internal fun clearCaches() {
        prefs.value.edit().clear().apply()
    }

    @Synchronized
    private fun storeResult(
        urlString: String,
        result: HTTPResult,
        eTag: String,
    ) {
        val metadata = ETagCacheMetadata.fromResult(result, ETagData(eTag, dateProvider.now))
        prefs.value.edit()
            .putString(urlString, metadata.serialize())
            // The payload is stored verbatim under its own key: embedding it inside the metadata JSON
            // would re-escape a multi-MB string we already hold, which OOMed on large /offerings
            // responses (https://github.com/RevenueCat/purchases-android/issues/3628).
            .putString(payloadKey(urlString), result.payloadText)
            .apply()
    }

    private fun getStoredMetadata(urlString: String): ETagCacheMetadata? {
        val serialized = prefs.value.getString(urlString, null) ?: return null
        return ETagCacheMetadata.deserialize(serialized)
            ?: migrateLegacyEntry(urlString, serialized)?.let { legacy ->
                ETagCacheMetadata.fromResult(legacy.httpResult, legacy.eTagData)
            }
    }

    /**
     * Pre-split entries embedded the payload (double-escaped) inside one combined JSON string. Rewrite
     * them in the split format on first read so later reads never pay the unescape cost again, and
     * return the parsed legacy entry. Unparseable data reads as a cache miss.
     */
    @Suppress("SwallowedException", "ReturnCount")
    @Synchronized
    private fun migrateLegacyEntry(urlString: String, serialized: String): HTTPResultWithETag? {
        val legacy = try {
            HTTPResultWithETag.deserialize(serialized)
        } catch (e: JSONException) {
            return null
        }
        // Re-read under the monitor: a concurrent clearCaches() or storeResult() may have removed or
        // replaced this entry after the (unsynchronized) caller captured it — writing the stale capture
        // back would resurrect data the clear was meant to purge. Treat any change as a cache miss.
        if (prefs.value.getString(urlString, null) != serialized) {
            return null
        }
        prefs.value.edit()
            .putString(urlString, ETagCacheMetadata.fromResult(legacy.httpResult, legacy.eTagData).serialize())
            .putString(payloadKey(urlString), legacy.httpResult.payloadText)
            .apply()
        return legacy
    }

    private fun shouldStoreBackendResult(resultFromBackend: HTTPResult): Boolean {
        val responseCode = resultFromBackend.responseCode
        return responseCode != RCHTTPStatusCodes.NOT_MODIFIED &&
            responseCode < RCHTTPStatusCodes.ERROR &&
            resultFromBackend.verificationResult != VerificationResult.FAILED
    }

    private fun shouldUseETag(verificationResult: VerificationResult, verificationRequested: Boolean): Boolean {
        return when (verificationResult) {
            VerificationResult.VERIFIED -> true
            VerificationResult.NOT_REQUESTED -> !verificationRequested
            // Should never happen since we don't store these verification results in the cache
            VerificationResult.FAILED, VerificationResult.VERIFIED_ON_DEVICE -> false
        }
    }

    companion object {
        // Cache keys are endpoint URLs from URL(baseURL, path).toString(), which never carry a '#'
        // fragment, so this suffix cannot collide with another URL's entry.
        private const val PAYLOAD_KEY_SUFFIX = "#rc_payload"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal fun payloadKey(urlString: String): String = "$urlString$PAYLOAD_KEY_SUFFIX"

        fun initializeSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(
                "${context.packageName}_preferences_etags",
                Context.MODE_PRIVATE,
            )
    }
}
