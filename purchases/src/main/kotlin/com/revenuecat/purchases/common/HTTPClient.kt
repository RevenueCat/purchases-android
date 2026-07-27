//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Build
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.api.BuildConfig
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.networking.APISourceFailover
import com.revenuecat.purchases.common.networking.ConnectionErrorReason
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPRequest
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.HTTPTimeoutManager
import com.revenuecat.purchases.common.networking.MapConverter
import com.revenuecat.purchases.common.networking.NullPointerReadingErrorStreamException
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCContainerFormatException
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import com.revenuecat.purchases.interfaces.StorefrontProvider
import com.revenuecat.purchases.strings.NetworkStrings
import com.revenuecat.purchases.utils.filterNotNullValues
import org.json.JSONException
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLConnection
import java.util.Date
import kotlin.time.Duration

/**
 * Listener interface for observing HTTP requests and responses.
 * Useful for testing and recording network interactions.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal interface RequestResponseListener {
    @Suppress("LongParameterList")
    fun onRequestResponse(
        url: String,
        method: String,
        requestHeaders: Map<String, String>,
        requestBody: String?,
        responseCode: Int,
        responseHeaders: Map<String, String>,
        responseBody: String,
    )
}

@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongParameterList")
internal class HTTPClient(
    private val appConfig: AppConfig,
    private val eTagManager: ETagManager,
    private val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    val signingManager: SigningManager,
    private val storefrontProvider: StorefrontProvider,
    private val apiSourceFailover: APISourceFailover?,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val mapConverter: MapConverter = MapConverter(),
    private val localeProvider: LocaleProvider,
    private val forceServerErrorStrategy: ForceServerErrorStrategy? = null,
    private val requestResponseListener: RequestResponseListener? = null,
    private val timeoutManager: HTTPTimeoutManager = HTTPTimeoutManager(appConfig, dateProvider),
) {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal companion object {
        // This will be used when we could not reach the server due to connectivity or any other issues.
        const val NO_STATUS_CODE = -1

        // Accept header value requesting the RC Container Format response.
        const val RC_FORMAT_ACCEPT = "application/x-rc-format"

        // Request header advertising which per-element codecs the SDK can decode. Dedicated to RC Container
        // Format body compression so it never collides with the transport-level Accept-Encoding.
        const val RC_FORMAT_ACCEPT_ENCODING_HEADER = "Accept-RC-Element-Encoding"

        // Per-element codecs the SDK can decode, advertised so the server never picks one we can't
        // (e.g. brotli/zstd). "gzip" implies "identity" is also acceptable.
        const val RC_FORMAT_ACCEPT_ENCODING = "gzip"

        // Defensive cap on API source attempts within one request, in case the source list is re-armed
        // (topic rebuild or interval restart) while a request is walking it.
        const val MAX_API_SOURCE_ATTEMPTS = 5
    }

    private val enableExtraRequestLogging = BuildConfig.ENABLE_EXTRA_REQUEST_LOGGING && appConfig.isDebugBuild

    private fun buffer(outputStream: OutputStream): BufferedWriter {
        return BufferedWriter(OutputStreamWriter(outputStream))
    }

    @Throws(IOException::class)
    private fun readBytesFully(inputStream: InputStream): ByteArray {
        return inputStream.readBytes()
    }

    /** A human-readable rendering of a response body for logging: byte size for RC Format, text otherwise. */
    private fun ByteArray.describeForLogging(endpoint: Endpoint, responseCode: Int): String =
        if (endpoint.expectsRCFormatResponse && RCHTTPStatusCodes.isSuccessful(responseCode)) {
            "<rc-format: $size bytes>"
        } else {
            String(this, Charsets.UTF_8)
        }

    @Suppress("TooGenericExceptionCaught")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getInputStream(connection: HttpURLConnection): InputStream? {
        return try {
            connection.inputStream
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException,
                is IOException,
                -> {
                    log(LogIntent.WARNING) { NetworkStrings.PROBLEM_CONNECTING.format(e.message) }
                    try {
                        connection.errorStream
                    } catch (e: NullPointerException) {
                        // We've received some reports on issues reading from the errorStream in what seems to be a
                        // Android/device specific issue: https://github.com/RevenueCat/purchases-android/issues/2606.
                        // This attempts to handle this gracefully.
                        throw NullPointerReadingErrorStreamException(e.message, e)
                    }
                }

                else -> throw e
            }
        }
    }

    @Throws(IOException::class)
    private fun writeFully(writer: BufferedWriter, body: String) {
        writer.write(body)
        writer.flush()
    }

    /** Performs a synchronous web request to the RevenueCat API
     * @param baseURL The server URL used to perform the request
     * @param endpoint Endpoint being used for the request
     * @param body The body of the request, for GET must be null
     * @param requestHeaders Map of headers, basic headers are added automatically
     * @return Result containing the HTTP response code and the parsed JSON body
     * @throws JSONException Thrown for any JSON errors, not thrown for returned HTTP error codes
     * @throws IOException Thrown for any unexpected errors, not thrown for returned HTTP error codes
     */
    @Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
    @Throws(JSONException::class, IOException::class)
    fun performRequest(
        baseURL: URL,
        endpoint: Endpoint,
        body: Map<String, Any?>?,
        postFieldsToSign: List<Pair<String, String>>?,
        requestHeaders: Map<String, String>,
        refreshETag: Boolean = false,
        fallbackBaseURLs: List<URL> = emptyList(),
        fallbackURLIndex: Int = 0,
    ): HTTPResult {
        fun canUseFallback(): Boolean =
            endpoint.supportsFallbackBaseURLs && fallbackURLIndex in fallbackBaseURLs.indices

        fun performRequestToFallbackURL(): HTTPResult {
            val fallbackBaseURL = fallbackBaseURLs[fallbackURLIndex]
            log(LogIntent.DEBUG) {
                NetworkStrings.RETRYING_CALL_WITH_FALLBACK_URL.format(
                    endpoint.getPath(useFallback = true),
                    fallbackBaseURL,
                )
            }
            return performRequest(
                fallbackBaseURL,
                endpoint,
                body,
                postFieldsToSign,
                requestHeaders,
                refreshETag,
                fallbackBaseURLs,
                fallbackURLIndex + 1,
            )
        }

        val isMainBackend = fallbackURLIndex == 0

        var source = apiSourceFailover?.currentSource(endpoint, baseURL, isFallbackAttempt = !isMainBackend)
        var sourceAttempts = 0

        while (true) {
            sourceAttempts++
            val outcome = performAttempt(
                requestBaseURL = source?.url ?: baseURL,
                isFallbackURL = fallbackURLIndex > 0,
                isMainBackend = isMainBackend,
                fallbackAvailable = canUseFallback(),
                endpoint = endpoint,
                body = body,
                postFieldsToSign = postFieldsToSign,
                requestHeaders = requestHeaders,
                refreshETag = refreshETag,
            )
            if (outcome.canFailOverToNextSource) {
                val nextSource = sourceToRetryOn(source, sourceAttempts, endpoint)
                if (nextSource != null) {
                    source = nextSource
                    continue
                }
            }
            return when (outcome) {
                is AttemptOutcome.Failed -> {
                    if (!canUseFallback()) {
                        throw outcome.exception
                    }
                    // Unlike iOS, we keep failing over on every connection-level IOException here, including
                    // ones that may be caused by the device being offline. iOS suppresses the host switch on
                    // device connectivity errors, but Android has no equivalent signal at this layer: a device
                    // with no connectivity and a host whose DNS fails both surface as UnknownHostException, and
                    // telling them apart requires a ConnectivityManager check (ACCESS_NETWORK_STATE), which the
                    // SDK does not currently have.
                    var fallbackResult = performRequestToFallbackURL()
                    if (RCHTTPStatusCodes.isServerError(fallbackResult.responseCode) && canUseFallback()) {
                        fallbackResult = performRequestToFallbackURL()
                    }
                    fallbackResult
                }

                is AttemptOutcome.Completed -> {
                    val result = outcome.result
                    when {
                        result == null -> {
                            log(LogIntent.WARNING) { NetworkStrings.ETAG_RETRYING_CALL }
                            performRequest(
                                baseURL,
                                endpoint,
                                body,
                                postFieldsToSign,
                                requestHeaders,
                                refreshETag = true,
                                fallbackBaseURLs,
                                fallbackURLIndex,
                            )
                        }

                        RCHTTPStatusCodes.isServerError(result.responseCode) && canUseFallback() ->
                            // Handle server errors with fallback URLs
                            performRequestToFallbackURL()

                        else -> result
                    }
                }
            }
        }
    }

    /** The result of one request attempt against a single host. */
    private sealed interface AttemptOutcome {
        /** [result] is null when an ETag cache miss requires retrying with a refreshed ETag. */
        data class Completed(val result: HTTPResult?) : AttemptOutcome

        data class Failed(val exception: IOException) : AttemptOutcome

        /** Whether this outcome is a failure that API sources may recover from by switching hosts. */
        val canFailOverToNextSource: Boolean
            get() = when (this) {
                is Failed -> true
                is Completed -> result?.let { RCHTTPStatusCodes.isServerError(it.responseCode) } == true
            }
    }

    /**
     * Performs one request attempt against [requestBaseURL], recording its timeout bookkeeping and
     * diagnostics. Connection-level failures come back as [AttemptOutcome.Failed] instead of throwing.
     */
    @Suppress("LongParameterList", "InstanceOfCheckForException")
    private fun performAttempt(
        requestBaseURL: URL,
        isFallbackURL: Boolean,
        isMainBackend: Boolean,
        fallbackAvailable: Boolean,
        endpoint: Endpoint,
        body: Map<String, Any?>?,
        postFieldsToSign: List<Pair<String, String>>?,
        requestHeaders: Map<String, String>,
        refreshETag: Boolean,
    ): AttemptOutcome {
        var callSuccessful = false
        val requestStartTime = dateProvider.now
        var callResult: HTTPResult? = null
        var requestResult: HTTPTimeoutManager.RequestResult = HTTPTimeoutManager.RequestResult.OTHER_RESULT
        var exceptionHit: IOException? = null

        try {
            callResult = performCall(
                requestBaseURL,
                isFallbackURL,
                endpoint,
                body,
                postFieldsToSign,
                requestHeaders,
                refreshETag,
            )
            callSuccessful = true

            if (isMainBackend && callResult?.let { RCHTTPStatusCodes.isSuccessful(it.responseCode) } == true) {
                requestResult = HTTPTimeoutManager.RequestResult.SUCCESS_ON_MAIN_BACKEND
            }
        } catch (e: IOException) {
            exceptionHit = e
            if (e is SocketTimeoutException && isMainBackend && fallbackAvailable) {
                requestResult =
                    HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
            }
        } finally {
            timeoutManager.recordRequestResult(requestResult)

            trackHttpRequestPerformedIfNeeded(
                requestBaseURL,
                endpoint,
                requestStartTime,
                callSuccessful,
                callResult,
                isRetry = refreshETag,
                connectionException = exceptionHit,
            )
        }
        return exceptionHit?.let { AttemptOutcome.Failed(it) } ?: AttemptOutcome.Completed(callResult)
    }

    /**
     * The next source to retry [endpoint]'s failed request on, or null when the request didn't target an
     * API [source], the attempt cap is reached, or [APISourceFailover] decides against failing over (the
     * source's health check passed, or every source is already unhealthy).
     */
    private fun sourceToRetryOn(
        source: APISourceFailover.ResolvedSource?,
        sourceAttempts: Int,
        endpoint: Endpoint,
    ): APISourceFailover.ResolvedSource? {
        val decision = source
            ?.takeIf { sourceAttempts < MAX_API_SOURCE_ATTEMPTS }
            ?.let { apiSourceFailover?.onRequestFailure(it) }
        return (decision as? APISourceFailover.FailureDecision.RetryNextSource)?.next?.also {
            log(LogIntent.DEBUG) {
                NetworkStrings.RETRYING_CALL_WITH_NEXT_API_SOURCE.format(endpoint.name, it.url)
            }
        }
    }

    @Suppress("ThrowsCount", "LongParameterList", "LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
    private fun performCall(
        baseURL: URL,
        isFallbackURL: Boolean,
        endpoint: Endpoint,
        body: Map<String, Any?>?,
        postFieldsToSign: List<Pair<String, String>>?,
        requestHeaders: Map<String, String>,
        refreshETag: Boolean,
    ): HTTPResult? {
        val jsonBody = body?.let { mapConverter.convertToJSON(it) }
        val path = endpoint.getPath(useFallback = isFallbackURL)
        val connection: HttpURLConnection
        val shouldSignResponse = signingManager.shouldVerifyEndpoint(endpoint)
        val shouldAddNonce = shouldSignResponse && endpoint.needsNonceToPerformSigning
        val nonce: String?
        val postFieldsToSignHeader: String?
        val fullURL: URL

        if (appConfig.runningTests) {
            forceServerErrorStrategy?.fakeResponseWithoutPerformingRequest(baseURL, endpoint)?.let {
                warnLog { "Faking response for request to ${endpoint.getPath()}" }
                return it
            }
        }

        try {
            fullURL = if (appConfig.runningTests &&
                forceServerErrorStrategy?.shouldForceServerError(baseURL, endpoint) == true
            ) {
                warnLog { "Forcing server error for request to ${URL(baseURL, path)}" }
                URL(forceServerErrorStrategy.serverErrorURL)
            } else {
                URL(baseURL, path)
            }

            nonce = if (shouldAddNonce) signingManager.createRandomNonce() else null
            postFieldsToSignHeader = postFieldsToSign?.takeIf { shouldSignResponse }?.let {
                signingManager.getPostParamsForSigningHeaderIfNeeded(endpoint, postFieldsToSign)
            }
            val headers = getHeaders(
                requestHeaders,
                fullURL,
                refreshETag,
                nonce,
                shouldSignResponse,
                postFieldsToSignHeader,
                endpoint,
            )

            val httpRequest = HTTPRequest(fullURL, headers, jsonBody)

            if (enableExtraRequestLogging) {
                debugLog { "HTTP request:\\n ${toCurlRequest(httpRequest)}" }
            }

            val timeout = timeoutManager.getTimeoutForRequest(
                isFallback = isFallbackURL,
                fallbackAvailable = endpoint.supportsFallbackBaseURLs && appConfig.fallbackBaseURLs.isNotEmpty(),
            )

            connection = getConnection(httpRequest, timeout)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }

        val inputStream = getInputStream(connection)

        val payloadBytes: ByteArray?
        val responseCode: Int
        try {
            debugLog { NetworkStrings.API_REQUEST_STARTED.format(connection.requestMethod, path) }
            responseCode = connection.responseCode
            payloadBytes = inputStream?.let { readBytesFully(it) }
            if (enableExtraRequestLogging) {
                debugLog {
                    "HTTP response:\\n  status code: $responseCode \\n  " +
                        "body: ${payloadBytes?.describeForLogging(endpoint, responseCode)}"
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            if (enableExtraRequestLogging) {
                errorLog(e) { "HTTP request failed" }
            }
            throw e
        } finally {
            inputStream?.close()
            connection.disconnect()
        }

        debugLog { NetworkStrings.API_REQUEST_COMPLETED.format(connection.requestMethod, path, responseCode) }
        if (payloadBytes == null &&
            (responseCode != RCHTTPStatusCodes.NO_CONTENT || !endpoint.expectsRCFormatResponse)
        ) {
            throw IOException(NetworkStrings.HTTP_RESPONSE_PAYLOAD_NULL)
        }
        // A 204 No Content response legitimately has no body, so a missing payload is treated as empty bytes.
        val bodyBytes = payloadBytes ?: ByteArray(0)

        // RC Format endpoints expose successful responses as raw bytes; everything else (including error
        // responses, which are still JSON) is decoded as UTF-8 text.
        val payload: HTTPResult.Payload = if (
            endpoint.expectsRCFormatResponse && RCHTTPStatusCodes.isSuccessful(responseCode)
        ) {
            HTTPResult.Payload.RCFormat(bodyBytes)
        } else {
            HTTPResult.Payload.Text(String(bodyBytes, Charsets.UTF_8))
        }
        val payloadText = payload.text

        // Notify listener if present
        if (appConfig.runningTests) {
            requestResponseListener?.let {
                val responseHeaders = mutableMapOf<String, String>()
                connection.headerFields.forEach { (key, values) ->
                    // Skip null keys (status line) and collect all headers
                    if (key != null && values.isNotEmpty()) {
                        responseHeaders[key] = values.joinToString(", ")
                    }
                }

                try {
                    val fullURL = URL(baseURL, path)
                    it.onRequestResponse(
                        url = fullURL.toString(),
                        method = connection.requestMethod,
                        requestHeaders = getHeaders(
                            requestHeaders,
                            fullURL,
                            refreshETag,
                            nonce,
                            shouldSignResponse,
                            postFieldsToSignHeader,
                            endpoint,
                        ),
                        requestBody = jsonBody?.toString(),
                        responseCode = responseCode,
                        responseHeaders = responseHeaders,
                        responseBody = payloadText.orEmpty(),
                    )
                } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                    // Don't let listener errors break the request
                    warnLog { "RequestResponseListener error: ${e.message}" }
                }
            }
        }

        val verificationResult = if (shouldSignResponse &&
            RCHTTPStatusCodes.isSuccessful(responseCode)
        ) {
            if (endpoint.expectsRCFormatResponse) {
                if (responseCode == RCHTTPStatusCodes.NO_CONTENT && bodyBytes.isEmpty()) {
                    verifyRCFormatNoContentResponse(path, connection, nonce)
                } else {
                    verifyRCFormatResponse(path, connection, bodyBytes, nonce)
                }
            } else {
                verifyResponse(path, connection, payloadText, nonce, postFieldsToSignHeader)
            }
        } else {
            VerificationResult.NOT_REQUESTED
        }

        if (verificationResult == VerificationResult.FAILED &&
            signingManager.signatureVerificationMode is SignatureVerificationMode.Enforced
        ) {
            throw SignatureVerificationException(path)
        }

        val isLoadShedderResponse = getLoadShedderHeader(connection)
        // RC Container Format endpoints are not ETag-cached: build the result directly and skip the cache.
        return if (endpoint.expectsRCFormatResponse) {
            HTTPResult(
                responseCode,
                payload,
                HTTPResult.Origin.BACKEND,
                getRequestDateHeader(connection),
                verificationResult,
                isLoadShedderResponse,
                isFallbackURL,
            )
        } else {
            eTagManager.getHTTPResultFromCacheOrBackend(
                responseCode,
                payloadText.orEmpty(),
                getETagHeader(connection),
                fullURL.toString(),
                refreshETag,
                getRequestDateHeader(connection),
                verificationResult,
                isLoadShedderResponse,
                isFallbackURL,
            )
        }
    }

    private fun toCurlRequest(httpRequest: HTTPRequest): String {
        val builder = StringBuilder("curl -v ")

        val method = if (httpRequest.body == null) {
            "GET"
        } else {
            "POST"
        }

        builder.append("-X ").append(method).append(" \\\n  ")

        for (entry in httpRequest.headers) {
            builder.append("-H \"").append(entry.key).append(":")
            builder.append(entry.value)
            builder.append("\" \\\n  ")
        }

        if (httpRequest.body != null) builder.append("-d '").append(httpRequest.body.toString()).append("' \\\n  ")

        builder.append("\"").append(httpRequest.fullURL).append("\"")

        return builder.toString()
    }

    private fun trackHttpRequestPerformedIfNeeded(
        baseURL: URL,
        endpoint: Endpoint,
        requestStartTime: Date,
        callSuccessful: Boolean,
        callResult: HTTPResult?,
        isRetry: Boolean,
        connectionException: IOException?,
    ) {
        diagnosticsTrackerIfEnabled?.let { tracker ->
            val responseTime = Duration.between(requestStartTime, dateProvider.now)
            val responseCode = if (callSuccessful) {
                // When the result given by ETagManager is null, is because we are asking to refresh the etag
                // since we could not find the response in the cache.
                callResult?.responseCode ?: RCHTTPStatusCodes.NOT_MODIFIED
            } else {
                NO_STATUS_CODE
            }
            val origin = callResult?.origin
            val verificationResult = callResult?.verificationResult ?: VerificationResult.NOT_REQUESTED
            val requestWasError = callSuccessful && RCHTTPStatusCodes.isSuccessful(responseCode)
            val connectionErrorReason = connectionException?.let { ConnectionErrorReason.fromIOException(it) }
            tracker.trackHttpRequestPerformed(
                baseURL.host,
                endpoint,
                responseTime,
                requestWasError,
                responseCode,
                callResult?.backendErrorCode,
                origin,
                verificationResult,
                isRetry,
                connectionErrorReason,
            )
        }
    }

    fun clearCaches() {
        eTagManager.clearCaches()
    }

    @Suppress("LongParameterList")
    private fun getHeaders(
        authenticationHeaders: Map<String, String>,
        fullURL: URL,
        refreshETag: Boolean,
        nonce: String?,
        shouldSignResponse: Boolean,
        postFieldsToSignHeader: String?,
        endpoint: Endpoint,
    ): Map<String, String> {
        return mapOf(
            "Content-Type" to "application/json",
            "Accept" to if (endpoint.expectsRCFormatResponse) RC_FORMAT_ACCEPT else null,
            // Advertise supported per-element codecs for RC Container Format responses (gzip-only on Android).
            // Uses a dedicated header (not standard Accept-Encoding) so it only governs per-element body
            // compression and never the HTTP transport encoding.
            RC_FORMAT_ACCEPT_ENCODING_HEADER to
                if (endpoint.expectsRCFormatResponse) RC_FORMAT_ACCEPT_ENCODING else null,
            "X-Platform" to getXPlatformHeader(),
            "X-Platform-Flavor" to appConfig.platformInfo.flavor,
            "X-Platform-Flavor-Version" to appConfig.platformInfo.version,
            "X-Platform-Version" to Build.VERSION.SDK_INT.toString(),
            "X-Platform-Device" to Build.MODEL,
            "X-Platform-Brand" to Build.BRAND,
            "X-Version" to Config.frameworkVersion,
            "X-Preferred-Locales" to localeProvider.currentLocalesLanguageTags.replace(oldChar = '-', newChar = '_'),
            "X-Client-Locale" to appConfig.languageTag,
            "X-Client-Version" to appConfig.versionName,
            "X-Client-Bundle-ID" to appConfig.packageName,
            "X-Observer-Mode-Enabled" to if (appConfig.finishTransactions) "false" else "true",
            "X-Nonce" to nonce,
            HTTPRequest.POST_PARAMS_HASH to postFieldsToSignHeader,
            "X-Custom-Entitlements-Computation" to if (appConfig.customEntitlementComputation) "true" else null,
            "X-UI-Preview-Mode" to if (appConfig.uiPreviewMode) "true" else null,
            "X-Storefront" to storefrontProvider.getStorefront(),
            "X-Is-Debug-Build" to appConfig.isDebugBuild.toString(),
            "X-Kotlin-Version" to KotlinVersion.CURRENT.toString(),
            "X-Is-Backgrounded" to appConfig.isAppBackgrounded.toString(),
            "X-Billing-Client-Sdk-Version" to BuildConfig.BILLING_CLIENT_VERSION,
        )
            .plus(authenticationHeaders)
            // RC Container Format endpoints are not ETag-cached, so they send no If-None-Match header.
            .plus(
                if (endpoint.expectsRCFormatResponse) {
                    emptyMap()
                } else {
                    eTagManager.getETagHeaders(fullURL.toString(), shouldSignResponse, refreshETag)
                },
            )
            .filterNotNullValues()
    }

    private fun getConnection(request: HTTPRequest, timeoutMs: Long): HttpURLConnection {
        return (request.fullURL.openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs.toInt()
            // We leave the read timeout to the default (readTimeout = 0), which means infinite.
            request.headers.forEach { (key, value) ->
                addRequestProperty(key, value)
            }
            request.body?.let { body ->
                doOutput = true
                requestMethod = "POST"
                val os = outputStream
                writeFully(buffer(os), body.toString())
            }
        }
    }

    private fun getXPlatformHeader() = when (appConfig.store) {
        Store.AMAZON -> "amazon"
        else -> "android"
    }

    private fun verifyResponse(
        urlPath: String,
        connection: URLConnection,
        payload: String?,
        nonce: String?,
        postFieldsToSignHeader: String?,
    ): VerificationResult {
        return signingManager.verifyResponse(
            urlPath = urlPath,
            signatureString = connection.getHeaderField(HTTPResult.SIGNATURE_HEADER_NAME),
            nonce = nonce,
            bodyBytes = payload?.toByteArray(),
            requestTime = getRequestTimeHeader(connection),
            eTag = getETagHeader(connection),
            postFieldsToSignHeader = postFieldsToSignHeader,
        )
    }

    /**
     * Verifies an RC Container Format response. The backend signs the leading config element's (element 0)
     * **uncompressed** bytes — the config part / `main_body` — so we verify the signature over
     * [RCContainer.config], which is the element already decoded by the container. Per-element compression is
     * transparent to the signature (as it is to the element checksum), so a codec change never invalidates a
     * signed config. The per-element container checksums are untrusted lookup hints, not a trust anchor: inline
     * blob elements are not signed and are instead authenticated transitively by hashing against the `blob_ref`
     * in the signed config. This endpoint is not ETag-cached and sends no post params, but the signature does
     * cover the request [nonce].
     */
    private fun verifyRCFormatResponse(
        urlPath: String,
        connection: URLConnection,
        payloadBytes: ByteArray,
        nonce: String?,
    ): VerificationResult {
        val bodyBytes = try {
            RCContainer.parse(payloadBytes).config
        } catch (e: RCContainerFormatException) {
            errorLog(e) { NetworkStrings.VERIFICATION_ERROR.format(urlPath) }
            return VerificationResult.FAILED
        }
        return signingManager.verifyResponse(
            urlPath = urlPath,
            signatureString = connection.getHeaderField(HTTPResult.SIGNATURE_HEADER_NAME),
            nonce = nonce,
            bodyBytes = bodyBytes,
            requestTime = getRequestTimeHeader(connection),
            eTag = getETagHeader(connection),
            postFieldsToSignHeader = null,
        )
    }

    /**
     * Verifies a `204 No Content` RC Container Format response. There is no body to sign, but the signature still
     * covers the request context (api key, [nonce], path, request time), so the empty response remains replay-
     * and tamper-evident. This endpoint emits no ETag, so the empty body is the only signed payload component.
     */
    private fun verifyRCFormatNoContentResponse(
        urlPath: String,
        connection: URLConnection,
        nonce: String?,
    ): VerificationResult {
        return signingManager.verifyResponse(
            urlPath = urlPath,
            signatureString = connection.getHeaderField(HTTPResult.SIGNATURE_HEADER_NAME),
            nonce = nonce,
            bodyBytes = ByteArray(0),
            requestTime = getRequestTimeHeader(connection),
            eTag = getETagHeader(connection),
            postFieldsToSignHeader = null,
        )
    }

    private fun getETagHeader(connection: URLConnection) = connection.getHeaderField(HTTPResult.ETAG_HEADER_NAME)
    private fun getRequestTimeHeader(connection: URLConnection): String? {
        return connection.getHeaderField(HTTPResult.REQUEST_TIME_HEADER_NAME)?.takeIf { it.isNotBlank() }
    }

    private fun getRequestDateHeader(connection: URLConnection): Date? {
        return getRequestTimeHeader(connection)?.toLong()?.let {
            Date(it)
        }
    }

    private fun getLoadShedderHeader(connection: URLConnection): Boolean {
        val loadShedderHeader = connection.getHeaderField(HTTPResult.LOAD_SHEDDER_HEADER_NAME)
        return loadShedderHeader?.lowercase() == "true"
    }
}
