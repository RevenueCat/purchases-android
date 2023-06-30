//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Build
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPRequest
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.MapConverter
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import com.revenuecat.purchases.strings.NetworkStrings
import com.revenuecat.purchases.utils.filterNotNullValues
import org.json.JSONException
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection
import java.util.Date
import kotlin.time.Duration

internal class HTTPClient(
    private val appConfig: AppConfig,
    private val eTagManager: ETagManager,
    private val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    val signingManager: SigningManager,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val mapConverter: MapConverter = MapConverter(),
) {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal companion object {
        // This will be used when we could not reach the server due to connectivity or any other issues.
        const val NO_STATUS_CODE = -1
    }

    private fun buffer(inputStream: InputStream): BufferedReader {
        return BufferedReader(InputStreamReader(inputStream))
    }

    private fun buffer(outputStream: OutputStream): BufferedWriter {
        return BufferedWriter(OutputStreamWriter(outputStream))
    }

    @Throws(IOException::class)
    private fun readFully(inputStream: InputStream): String {
        return buffer(inputStream).readText()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getInputStream(connection: HttpURLConnection): InputStream? {
        return try {
            connection.inputStream
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException,
                is IOException,
                -> {
                    log(LogIntent.WARNING, NetworkStrings.PROBLEM_CONNECTING.format(e.message))
                    connection.errorStream
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
    @Throws(JSONException::class, IOException::class)
    fun performRequest(
        baseURL: URL,
        endpoint: Endpoint,
        body: Map<String, Any?>?,
        requestHeaders: Map<String, String>,
        refreshETag: Boolean = false,
    ): HTTPResult {
        if (appConfig.forceServerErrors) {
            warnLog("Forcing server error for request to ${endpoint.getPath()}")
            return HTTPResult(
                RCHTTPStatusCodes.ERROR,
                payload = "",
                HTTPResult.Origin.BACKEND,
                requestDate = null,
                VerificationResult.NOT_REQUESTED,
            )
        }
        var callSuccessful = false
        val requestStartTime = dateProvider.now
        var callResult: HTTPResult? = null
        try {
            callResult = performCall(baseURL, endpoint, body, requestHeaders, refreshETag)
            callSuccessful = true
        } finally {
            trackHttpRequestPerformedIfNeeded(endpoint, requestStartTime, callSuccessful, callResult)
        }
        if (callResult == null) {
            log(LogIntent.WARNING, NetworkStrings.ETAG_RETRYING_CALL)
            callResult = performRequest(baseURL, endpoint, body, requestHeaders, refreshETag = true)
        }
        return callResult
    }

    @Suppress("ThrowsCount")
    private fun performCall(
        baseURL: URL,
        endpoint: Endpoint,
        body: Map<String, Any?>?,
        requestHeaders: Map<String, String>,
        refreshETag: Boolean,
    ): HTTPResult? {
        val jsonBody = body?.let { mapConverter.convertToJSON(it) }
        val path = endpoint.getPath()
        val urlPathWithVersion = "/v1$path"
        val connection: HttpURLConnection
        val shouldSignResponse = signingManager.shouldVerifyEndpoint(endpoint)
        val nonce: String?
        try {
            val fullURL = URL(baseURL, urlPathWithVersion)

            nonce = if (shouldSignResponse) signingManager.createRandomNonce() else null
            val headers = getHeaders(requestHeaders, urlPathWithVersion, refreshETag, nonce)

            val httpRequest = HTTPRequest(fullURL, headers, jsonBody)

            connection = getConnection(httpRequest)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }

        val inputStream = getInputStream(connection)

        val payload: String?
        val responseCode: Int
        try {
            debugLog(NetworkStrings.API_REQUEST_STARTED.format(connection.requestMethod, path))
            responseCode = connection.responseCode
            payload = inputStream?.let { readFully(it) }
        } finally {
            inputStream?.close()
            connection.disconnect()
        }

        debugLog(NetworkStrings.API_REQUEST_COMPLETED.format(connection.requestMethod, path, responseCode))
        if (payload == null) {
            throw IOException(NetworkStrings.HTTP_RESPONSE_PAYLOAD_NULL)
        }

        val verificationResult = if (shouldSignResponse &&
            nonce != null &&
            RCHTTPStatusCodes.isSuccessful(responseCode)
        ) {
            verifyResponse(path, responseCode, connection, payload, nonce)
        } else {
            VerificationResult.NOT_REQUESTED
        }

        if (verificationResult == VerificationResult.FAILED &&
            signingManager.signatureVerificationMode is SignatureVerificationMode.Enforced
        ) {
            throw SignatureVerificationException(path)
        }

        return eTagManager.getHTTPResultFromCacheOrBackend(
            responseCode,
            payload,
            getETagHeader(connection),
            urlPathWithVersion,
            refreshETag,
            getRequestDateHeader(connection),
            verificationResult,
        )
    }

    private fun trackHttpRequestPerformedIfNeeded(
        endpoint: Endpoint,
        requestStartTime: Date,
        callSuccessful: Boolean,
        callResult: HTTPResult?,
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
            tracker.trackHttpRequestPerformed(
                endpoint,
                responseTime,
                requestWasError,
                responseCode,
                origin,
                verificationResult,
            )
        }
    }

    fun clearCaches() {
        eTagManager.clearCaches()
    }

    private fun getHeaders(
        authenticationHeaders: Map<String, String>,
        urlPath: String,
        refreshETag: Boolean,
        nonce: String?,
    ): Map<String, String> {
        return mapOf(
            "Content-Type" to "application/json",
            "X-Platform" to getXPlatformHeader(),
            "X-Platform-Flavor" to appConfig.platformInfo.flavor,
            "X-Platform-Flavor-Version" to appConfig.platformInfo.version,
            "X-Platform-Version" to Build.VERSION.SDK_INT.toString(),
            "X-Version" to Config.frameworkVersion,
            "X-Client-Locale" to appConfig.languageTag,
            "X-Client-Version" to appConfig.versionName,
            "X-Client-Bundle-ID" to appConfig.packageName,
            "X-Observer-Mode-Enabled" to if (appConfig.finishTransactions) "false" else "true",
            "X-Nonce" to nonce,
        )
            .plus(authenticationHeaders)
            .plus(eTagManager.getETagHeaders(urlPath, refreshETag))
            .filterNotNullValues()
    }

    private fun getConnection(request: HTTPRequest): HttpURLConnection {
        return (request.fullURL.openConnection() as HttpURLConnection).apply {
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
        responseCode: Int,
        connection: URLConnection,
        payload: String?,
        nonce: String,
    ): VerificationResult {
        return signingManager.verifyResponse(
            urlPath = urlPath,
            responseCode = responseCode,
            signature = connection.getHeaderField(HTTPResult.SIGNATURE_HEADER_NAME),
            nonce = nonce,
            body = payload,
            requestTime = getRequestTimeHeader(connection),
            eTag = getETagHeader(connection),
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
}
