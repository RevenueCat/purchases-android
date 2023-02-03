//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Build
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.HTTPRequest
import com.revenuecat.purchases.common.networking.HTTPResult
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
import java.util.zip.GZIPOutputStream

class HTTPClient(
    private val appConfig: AppConfig,
    private val eTagManager: ETagManager
) {

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
     * @param path The resource being requested
     * @param body The body of the request, for GET must be null
     * @param requestHeaders Map of headers, basic headers are added automatically
     * @return Result containing the HTTP response code and the parsed JSON body
     * @throws JSONException Thrown for any JSON errors, not thrown for returned HTTP error codes
     * @throws IOException Thrown for any unexpected errors, not thrown for returned HTTP error codes
     */
    @Throws(JSONException::class, IOException::class)
    fun performRequest(
        path: String,
        body: Map<String, Any?>?,
        requestHeaders: Map<String, String>,
        refreshETag: Boolean = false,
        gzipRequest: Boolean = false
    ): HTTPResult {
        val jsonBody = body?.convert()

        val fullURL: URL
        val connection: HttpURLConnection
        val httpRequest: HTTPRequest
        val urlPathWithVersion = "/v1$path"
        try {
            fullURL = URL(appConfig.baseURL, urlPathWithVersion)

            val headers = getHeaders(requestHeaders, urlPathWithVersion, refreshETag, gzipRequest)
            httpRequest = HTTPRequest(fullURL, headers, jsonBody, gzipRequest)

            connection = getConnection(httpRequest)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }

        val inputStream = getInputStream(connection)

        val payload: String?
        val responseCode: Int
        try {
            log(LogIntent.DEBUG, NetworkStrings.API_REQUEST_STARTED.format(connection.requestMethod, path))
            responseCode = connection.responseCode
            payload = inputStream?.let { readFully(it) }
        } finally {
            inputStream?.close()
            connection.disconnect()
        }

        log(LogIntent.DEBUG, NetworkStrings.API_REQUEST_COMPLETED.format(connection.requestMethod, path, responseCode))
        if (payload == null) {
            throw IOException(NetworkStrings.HTTP_RESPONSE_PAYLOAD_NULL)
        }

        val callResult: HTTPResult? = eTagManager.getHTTPResultFromCacheOrBackend(
            responseCode,
            payload,
            connection,
            urlPathWithVersion,
            refreshETag
        )
        if (callResult == null) {
            log(LogIntent.WARNING, NetworkStrings.ETAG_RETRYING_CALL)
            return performRequest(path, body, requestHeaders, refreshETag = true)
        }
        return callResult
    }

    fun clearCaches() {
        eTagManager.clearCaches()
    }

    private fun getHeaders(
        authenticationHeaders: Map<String, String>,
        urlPath: String,
        refreshETag: Boolean,
        gzipRequest: Boolean
    ): Map<String, String> {
        val contentEncoding = if (gzipRequest) "gzip" else null
        return mapOf(
            "Content-Type" to "application/json",
            "Content-Encoding" to contentEncoding,
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
                val outputStream = if (request.gzipRequest) GZIPOutputStream(os) else os
                writeFully(buffer(outputStream), body.toString())
            }
        }
    }

    private fun getXPlatformHeader() = when (appConfig.store) {
        Store.AMAZON -> "amazon"
        else -> "android"
    }
}
