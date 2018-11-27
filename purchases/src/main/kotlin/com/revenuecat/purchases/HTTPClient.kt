package com.revenuecat.purchases

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
    var baseURL: URL = URL("https://api.revenuecat.com/")
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

    private fun getInputStream(connection: HttpURLConnection): InputStream {
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
     * @throws HTTPErrorException Thrown for any unexpected errors, not thrown for returned HTTP error codes
     */
    @Throws(HTTPClient.HTTPErrorException::class)
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

    @Throws(HTTPClient.HTTPErrorException::class)
    fun performRequest(
        path: String,
        body: JSONObject?,
        headers: Map<String, String>?
    ): Result {
        val fullURL: URL
        try {
            fullURL = URL(baseURL, "/v1$path")
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }

        val connection: HttpURLConnection
        try {
            connection = (fullURL.openConnection() as HttpURLConnection).apply{
                headers?.forEach { (key, value) ->
                    addRequestProperty(key, value)
                }
                addRequestProperty("Content-Type", "application/json")
                addRequestProperty("X-Platform", "android")
                addRequestProperty(
                    "X-Platform-Version",
                    Integer.toString(android.os.Build.VERSION.SDK_INT)
                )
                addRequestProperty("X-Version", Purchases.frameworkVersion)

                if (body != null) {
                    doOutput = true
                    requestMethod = "POST"
                    val os = outputStream
                    writeFully(buffer(os), body.toString())
                }
            }
        } catch (e: IOException) {
            throw HTTPErrorException(-1, "Error establishing connection " + e.message)
        }

        val inputStream = getInputStream(connection)
        val result = HTTPClient.Result()

        val payload: String
        try {
            result.responseCode = connection.responseCode
            payload = readFully(inputStream)
        } catch (e: IOException) {
            throw HTTPErrorException(result.responseCode, "Error reading response: " + e.message)
        } finally {
            connection.disconnect()
        }

        try {
            result.body = JSONObject(payload)
        } catch (e: JSONException) {
            throw HTTPErrorException(result.responseCode, "Error parsing JSON body: $payload")
        }

        return result
    }

    internal class Result {
        var responseCode: Int = 0
        var body: JSONObject? = null
    }

    internal class HTTPErrorException(httpCode: Int, message: String) :
        Exception("[$httpCode]: $message")

}
