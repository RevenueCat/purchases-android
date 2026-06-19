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
    @Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod", "InstanceOfCheckForException")
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

        var callSuccessful = false
        val requestStartTime = dateProvider.now
        var callResult: HTTPResult? = null
        var requestResult: HTTPTimeoutManager.RequestResult = HTTPTimeoutManager.RequestResult.OTHER_RESULT
        var exceptionHit: IOException? = null

        try {
            callResult = performCall(
                baseURL,
                fallbackURLIndex > 0,
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
            if (e is SocketTimeoutException && isMainBackend && canUseFallback()) {
                requestResult = HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
                callResult = performRequestToFallbackURL()
            } else if (canUseFallback()) {
                callResult = performRequestToFallbackURL()
            } else {
                throw e
            }
        } finally {
            timeoutManager.recordRequestResult(requestResult)

            trackHttpRequestPerformedIfNeeded(
                baseURL,
                endpoint,
                requestStartTime,
                callSuccessful,
                callResult,
                isRetry = refreshETag,
                connectionException = exceptionHit,
            )
        }
        if (callResult == null) {
            log(LogIntent.WARNING) { NetworkStrings.ETAG_RETRYING_CALL }
            callResult = performRequest(
                baseURL,
                endpoint,
                body,
                postFieldsToSign,
                requestHeaders,
                refreshETag = true,
                fallbackBaseURLs,
                fallbackURLIndex,
            )
        } else if (RCHTTPStatusCodes.isServerError(callResult.responseCode) && canUseFallback()) {
            // Handle server errors with fallback URLs
            callResult = performRequestToFallbackURL()
        }
        return callResult
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
                    VerificationResult.VERIFIED
                } else {
                    verifyRCFormatResponse(path, connection, bodyBytes)
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
     * Verifies an RC Container Format response. The backend signs the config element's (element 0)
     * 24-byte truncated SHA-256 checksum, so we verify the signature over that checksum and
     * independently confirm the config bytes hash to it. Both checks are required: the signature ties
     * the checksum to the backend, and [RCElement.isChecksumValid] ties the checksum to the data.
     * This endpoint is not ETag-cached and sends no nonce / post params.
     */
    private fun verifyRCFormatResponse(
        urlPath: String,
        connection: URLConnection,
        payloadBytes: ByteArray,
    ): VerificationResult {
        val config = try {
            RCContainer.parse(payloadBytes).config
        } catch (e: RCContainerFormatException) {
            errorLog(e) { NetworkStrings.VERIFICATION_ERROR.format(urlPath) }
            return VerificationResult.FAILED
        }
        return if (!config.isChecksumValid()) {
            errorLog { NetworkStrings.VERIFICATION_ERROR.format(urlPath) }
            VerificationResult.FAILED
        } else {
            signingManager.verifyResponse(
                urlPath = urlPath,
                signatureString = connection.getHeaderField(HTTPResult.SIGNATURE_HEADER_NAME),
                nonce = null,
                bodyBytes = config.checksumBytes(),
                requestTime = getRequestTimeHeader(connection),
                eTag = getETagHeader(connection),
                postFieldsToSignHeader = null,
            )
        }
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
