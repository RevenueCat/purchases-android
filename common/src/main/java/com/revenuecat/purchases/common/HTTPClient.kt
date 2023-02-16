//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Build
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPRequest
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.strings.NetworkStrings
import com.revenuecat.purchases.utils.filterNotNullValues
import org.json.JSONException
import org.json.JSONObject
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

class HTTPClient(
    private val appConfig: AppConfig,
    private val eTagManager: ETagManager,
    private val diagnosticsTracker: DiagnosticsTracker?,
    private val dateProvider: DateProvider = DefaultDateProvider()
) {
    internal companion object {
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
        return readFully(buffer(inputStream))
    }

    @Throws(IOException::class)
    private fun readFully(reader: BufferedReader): String {
        val sb = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            sb.append(line)
            line = reader.readLine()
        }
        return sb.toString()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getInputStream(connection: HttpURLConnection): InputStream? {
        return try {
            connection.inputStream
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException,
                is IOException -> {
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
        refreshETag: Boolean = false
    ): HTTPResult {
        var callSuccessful = false
        val requestStartTime = dateProvider.now.time
        var callResult: HTTPResult? = null
        try {
            callResult = performCall(baseURL, endpoint, body, requestHeaders, refreshETag)
            callSuccessful = true
        } finally {
            trackEndpointHitIfNeeded(endpoint, requestStartTime, callSuccessful, callResult)
        }
        if (callResult == null) {
            log(LogIntent.WARNING, NetworkStrings.ETAG_RETRYING_CALL)
            return performRequest(baseURL, endpoint, body, requestHeaders, refreshETag = true)
        }
        return callResult
    }

    private fun performCall(
        baseURL: URL,
        endpoint: Endpoint,
        body: Map<String, Any?>?,
        requestHeaders: Map<String, String>,
        refreshETag: Boolean
    ): HTTPResult? {
        val jsonBody = body?.convert()
        val path = endpoint.getPath()
        val urlPathWithVersion = "/v1$path"
        val connection: HttpURLConnection
        try {
            val fullURL = URL(baseURL, urlPathWithVersion)

            val headers = getHeaders(requestHeaders, urlPathWithVersion, refreshETag)
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

        return eTagManager.getHTTPResultFromCacheOrBackend(
            responseCode,
            payload,
            connection,
            urlPathWithVersion,
            refreshETag
        )
    }

    private fun trackEndpointHitIfNeeded(
        endpoint: Endpoint,
        requestStartTime: Long,
        callSuccessful: Boolean,
        callResult: HTTPResult?
    ) {
        diagnosticsTracker?.let { tracker ->
            val responseTime = dateProvider.now.time - requestStartTime
            val responseCode = if (callSuccessful) {
                // When the result given by ETagManager is null, is because we are asking to refresh the etag
                // since we could not find the response in the cache.
                callResult?.responseCode ?: RCHTTPStatusCodes.NOT_MODIFIED
            } else {
                NO_STATUS_CODE
            }
            val origin = callResult?.origin
            val requestWasError = callSuccessful && RCHTTPStatusCodes.isSuccessful(responseCode)
            tracker.trackEndpointHit(endpoint, responseTime, requestWasError, responseCode, origin)
        }
    }

    fun clearCaches() {
        eTagManager.clearCaches()
    }

    private fun getHeaders(
        authenticationHeaders: Map<String, String>,
        urlPath: String,
        refreshETag: Boolean
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
            "X-Observer-Mode-Enabled" to if (appConfig.finishTransactions) "false" else "true"
        )
            .plus(authenticationHeaders)
            .plus(eTagManager.getETagHeader(urlPath, refreshETag))
            .filterNotNullValues()
    }

    private fun Map<String, Any?>.convert(): JSONObject {
        val mapWithoutInnerMaps = mapValues { (_, value) ->
            value.tryCast<Map<String, Any?>>(ifSuccess = { convert() })
        }
        return JSONObject(mapWithoutInnerMaps)
    }

    // To avoid Java type erasure, we use a Kotlin inline function with a reified parameter
    // so that we can check the type on runtime.
    //
    // Doing something like:
    // if (value is Map<*, *>) (value as Map<String, Any?>).convert()
    //
    // Would give an unchecked cast warning due to Java type erasure
    private inline fun <reified T> Any?.tryCast(
        ifSuccess: T.() -> Any?
    ): Any? {
        return if (this is T) {
            this.ifSuccess()
        } else {
            this
        }
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
}
