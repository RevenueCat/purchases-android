package com.revenuecat.purchases.utils

import java.io.InputStream

internal class TestUrlConnection(
    override val responseCode: Int,
    override val inputStream: InputStream
) : UrlConnection {

    var disconnectCallCount = 0
        private set

    val isDisconnected: Boolean
        get() = disconnectCallCount > 0

    override fun disconnect() {
        disconnectCallCount += 1
    }
}

internal class TestUrlConnectionFactory(
    private val mockedConnections: Map<String, TestUrlConnection>
): UrlConnectionFactory {
    private val _createdConnections = mutableListOf<String>()
    val createdConnections: List<String>
        get() = _createdConnections.toList()

    override fun createConnection(url: String, requestMethod: String): UrlConnection {
        _createdConnections.add(url)
        return mockedConnections[url] ?: throw IllegalArgumentException("No mocked connection for URL: $url")
    }

    fun clear() {
        _createdConnections.clear()
    }
}
