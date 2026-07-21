package com.revenuecat.purchases.common.networking

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.NetworkStrings
import com.revenuecat.purchases.utils.isAndroidNOrNewer
import com.revenuecat.purchases.utils.optNullableLong
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal data class ETagData(
    val eTag: String,
    val lastRefreshTime: Date?,
)

/**
 * Everything the ETag cache persists about a response except its payload, stored as a small flat JSON
 * under the URL key; the payload lives in an [ETagPayloadStore] file. The prefs file name is versioned,
 * which keeps downgrades safe: older SDKs read their own file and simply refetch, and their file is
 * deleted, unparsed, at the next upgraded launch ([ETagManager.deleteLegacyPreferencesFile]).
 *
 * `origin` is intentionally not persisted: stored results always read back as [HTTPResult.Origin.CACHE].
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
    /** Verified on read to detect truncated payload files; `null` skips the check. */
    val payloadSizeBytes: Long? = null,
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
            payloadSizeBytes?.let { put(SERIALIZATION_NAME_PAYLOAD_SIZE_BYTES, it) }
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
        private const val SERIALIZATION_NAME_PAYLOAD_SIZE_BYTES = "payloadSizeBytes"

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

        /** Null when [serialized] is unparseable, including entries from older SDK formats. */
        @Suppress("SwallowedException")
        fun deserialize(serialized: String): ETagCacheMetadata? {
            return try {
                val jsonObject = JSONObject(serialized)
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
                    payloadSizeBytes = jsonObject.optNullableLong(SERIALIZATION_NAME_PAYLOAD_SIZE_BYTES),
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
    private val prefs: Lazy<SharedPreferences> = lazy {
        // The unlink is negligible next to the blocking prefs load that follows, and the first prefs
        // touch is normally an ETag header read on a dispatcher thread anyway.
        deleteLegacyPreferencesFile(context)
        initializeSharedPreferences(context)
    },
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val payloadStore: ETagPayloadStore = ETagPayloadStore(context),
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
        val metadata = ETagCacheMetadata.deserialize(serialized) ?: return null
        // Lock-free read: a store racing between the metadata and payload reads fails the size check,
        // costing one spurious miss plus its refresh retry. The size check itself is what turns a
        // truncated file into a miss here: downstream, HTTPResult.body swallows the parse failure and
        // the server keeps answering 304 for its eTag, so a bad entry would never heal.
        val payload = payloadStore.read(urlString, metadata.payloadSizeBytes) ?: return null
        return metadata.toHTTPResult(payload)
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
        // Metadata first: its in-memory clear is immediate, so readers miss before ever touching files.
        // A crash before the async disk flush leaves metadata without payloads, a self-healing miss.
        prefs.value.edit().clear().apply()
        payloadStore.clear()
    }

    @Synchronized
    private fun storeResult(
        urlString: String,
        result: HTTPResult,
        eTag: String,
    ) {
        persistEntry(
            urlString,
            ETagCacheMetadata.fromResult(result, ETagData(eTag, dateProvider.now)),
            result.payloadText,
        )
    }

    /**
     * Payload file first, metadata only once that write succeeded: metadata present always implies its
     * payload was written.
     */
    private fun persistEntry(urlString: String, metadata: ETagCacheMetadata, payload: String) {
        val payloadSizeBytes = payloadStore.write(urlString, payload) ?: return
        prefs.value.edit()
            .putString(urlString, metadata.copy(payloadSizeBytes = payloadSizeBytes).serialize())
            .apply()
    }

    private fun getStoredMetadata(urlString: String): ETagCacheMetadata? {
        return prefs.value.getString(urlString, null)?.let { ETagCacheMetadata.deserialize(it) }
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
        private const val LEGACY_PREFERENCES_FILE_SUFFIX = "_preferences_etags"
        private const val PREFERENCES_FILE_SUFFIX = "_preferences_etags_v2"

        /**
         * Deletes the pre-split-format prefs file without ever loading it, so its multi-MB payload
         * values are dropped rather than parsed into memory. Not supported below API 24; the stale
         * file is left in place there.
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal fun deleteLegacyPreferencesFile(context: Context) {
            if (isAndroidNOrNewer()) {
                try {
                    context.deleteSharedPreferences("${context.packageName}$LEGACY_PREFERENCES_FILE_SUFFIX")
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    // Best-effort cleanup: a failure here must never break the cache itself.
                    errorLog(e) { "Failed to delete the legacy ETag preferences file." }
                }
            }
        }

        fun initializeSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(
                "${context.packageName}$PREFERENCES_FILE_SUFFIX",
                Context.MODE_PRIVATE,
            )
    }
}
