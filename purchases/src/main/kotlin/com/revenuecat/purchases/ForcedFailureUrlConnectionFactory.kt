package com.revenuecat.purchases

import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import java.io.IOException

/**
 * Wraps [delegate] so downloads [strategy] declares unreachable throw instead of connecting, letting an
 * integration test simulate no network on paths that bypass `HTTPClient`.
 */
internal class ForcedFailureUrlConnectionFactory(
    private val delegate: UrlConnectionFactory,
    private val strategy: ForceServerErrorStrategy,
) : UrlConnectionFactory {

    override fun createConnection(
        url: String,
        connectTimeoutMillis: Int,
        readTimeoutMillis: Int,
        requestMethod: String,
    ): UrlConnection {
        if (strategy.shouldForceConnectionFailure(url)) {
            throw IOException("Forced connection failure for $url")
        }
        return delegate.createConnection(url, connectTimeoutMillis, readTimeoutMillis, requestMethod)
    }
}
