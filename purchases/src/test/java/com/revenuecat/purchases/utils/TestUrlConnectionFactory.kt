package com.revenuecat.purchases.utils

import java.io.InputStream

internal class TestUrlConnection(
    override val responseCode: Int,
    override val inputStream: InputStream
) : UrlConnection {

    var disconnectCallCount = 0
    override fun disconnect() {
        disconnectCallCount += 1
    }
}

internal class TestUrlConnectionFactory(
    private val mockedConnections: Map<String, TestUrlConnection>
): UrlConnectionFactory {
    override fun createConnection(url: String, requestMethod: String): UrlConnection {
        return mockedConnections[url] ?: throw IllegalArgumentException("No mocked connection for URL: $url")
    }
}
