//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Build
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

internal class HTTPClient(
    private var baseURL: URL = URL("https://api.revenuecat.com/")
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
     * @param headers Map of headers, basic headers are added automatically
     * @return Result containing the HTTP response code and the parsed JSON body
     * @throws JSONException Thrown for any JSON errors, not thrown for returned HTTP error codes
     * @throws IOException Thrown for any unexpected errors, not thrown for returned HTTP error codes
     */
    @Throws(JSONException::class, IOException::class)
    fun performRequest(
        path: String,
        body: Map<*, *>?,
        headers: Map<String, String>
    ): Result {
        val jsonBody =
            body?.let {
                JSONObject(body)
            }

        return performRequest(path, jsonBody, headers)
    }

    @Throws(JSONException::class, IOException::class)
    fun performRequest(
        path: String,
        body: JSONObject?,
        headers: Map<String, String>?
    ): Result {
        val fullURL: URL
        val connection: HttpURLConnection
        try {
            fullURL = URL(baseURL, "/v1$path")
            connection = getConnection(fullURL, headers, body)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }

        val inputStream = getInputStream(connection)
        val result = HTTPClient.Result()

        val payload: String?
        try {
            debugLog("${connection.requestMethod} $path")
            result.responseCode = connection.responseCode
            payload = inputStream?.let { readFully(it) }
        } finally {
            connection.disconnect()
        }

        result.body = JSONObject(payload)
        debugLog("${connection.requestMethod} $path ${result.responseCode}")

        return result
    }

    private fun getConnection(
        fullURL: URL,
        headers: Map<String, String>?,
        body: JSONObject?
    ): HttpURLConnection {
        return (fullURL.openConnection() as HttpURLConnection).apply {
            headers?.forEach { (key, value) ->
                addRequestProperty(key, value)
            }
            addRequestProperty("Content-Type", "application/json")
            addRequestProperty("X-Platform", "android")
            addRequestProperty(
                "X-Platform-Version",
                Integer.toString(Build.VERSION.SDK_INT)
            )
            addRequestProperty("X-Version", Purchases.frameworkVersion)

            if (body != null) {
                doOutput = true
                requestMethod = "POST"
                val os = outputStream
                writeFully(buffer(os), body.toString())
            }
        }
    }

    internal class Result {
        var responseCode: Int = 0
        var body: JSONObject? = null
    }

}
