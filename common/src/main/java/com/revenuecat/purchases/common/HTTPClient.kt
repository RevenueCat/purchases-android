//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Build
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.HTTPRequest
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
import kotlin.jvm.Throws

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

    private fun getInputStream(connection: HttpURLConnection): InputStream? {
        return try {
            connection.inputStream
        } catch (e: IOException) {
            connection.errorStream
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
     * @param authenticationHeaders Map of headers, basic headers are added automatically
     * @return Result containing the HTTP response code and the parsed JSON body
     * @throws JSONException Thrown for any JSON errors, not thrown for returned HTTP error codes
     * @throws IOException Thrown for any unexpected errors, not thrown for returned HTTP error codes
     */
    @Throws(JSONException::class, IOException::class)
    fun performRequest(
        path: String,
        body: Map<String, Any?>?,
        authenticationHeaders: Map<String, String>
    ): HTTPResult {
        val jsonBody = body?.convert()

        val fullURL: URL
        val connection: HttpURLConnection
        val httpRequest: HTTPRequest
        try {
            fullURL = URL(appConfig.baseURL, "/v1$path")
            val headersWithoutETag = getHeaders(authenticationHeaders)
            httpRequest = HTTPRequest(fullURL, headersWithoutETag, jsonBody).let {
                eTagManager.addETagHeaderToRequest(it)
            }

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
            throw IOException("Network call payload is null.")
        }
        val result = HTTPResult(responseCode, payload)
        val eTagInResponse = connection.getHeaderField("X-RevenueCat-ETag")
        return eTagManager.processResponse(httpRequest, eTagInResponse, result)
    }

    fun clearCaches() {
        eTagManager.clearCaches()
    }

    private fun getHeaders(authenticationHeaders: Map<String, String>): Map<String, String> {
        return mapOf(
            "Content-Type" to "application/json",
            "X-Platform" to getXPlatformHeader(),
            "X-Platform-Flavor" to appConfig.platformInfo.flavor,
            "X-Platform-Flavor-Version" to appConfig.platformInfo.version,
            "X-Platform-Version" to Build.VERSION.SDK_INT.toString(),
            "X-Version" to Config.frameworkVersion,
            "X-Client-Locale" to appConfig.languageTag,
            "X-Client-Version" to appConfig.versionName,
            "X-Observer-Mode-Enabled" to if (appConfig.finishTransactions) "false" else "true"
        ).plus(authenticationHeaders).filterNotNullValues()
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
