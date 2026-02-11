package com.revenuecat.purchases.utils

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal interface UrlConnectionFactory {
    public fun createConnection(url: String, requestMethod: String = "GET"): UrlConnection
}

internal interface UrlConnection {
    public val responseCode: Int
    public val inputStream: InputStream
    public fun disconnect()
}

private const val TIMEOUT = 5000

internal class DefaultUrlConnectionFactory : UrlConnectionFactory {
    override fun createConnection(url: String, requestMethod: String): UrlConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT
        connection.readTimeout = TIMEOUT
        connection.requestMethod = requestMethod
        connection.doInput = true
        return DefaultUrlConnection(connection)
    }
}

private class DefaultUrlConnection(
    private val connection: HttpURLConnection,
) : UrlConnection {
    override val responseCode: Int
        get() = connection.responseCode
    override val inputStream: InputStream
        get() = connection.inputStream
    override fun disconnect() {
        connection.disconnect()
    }
}
