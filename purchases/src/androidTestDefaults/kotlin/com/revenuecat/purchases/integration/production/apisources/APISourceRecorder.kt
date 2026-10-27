package com.revenuecat.purchases.integration.production.apisources

import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.networking.Endpoint
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList

internal object APIHosts {
    const val PRIMARY = "api.revenuecat.com"
    const val BACKUP = "api.rc-backup.com"
    val FALLBACK: String = AppConfig.fallbackURL.host
}

/**
 * Records the host every `HTTPClient` request was sent to, through the [ForceServerErrorStrategy]s it creates.
 * Hosts are matched instead of sources, so requests sent to the default host once every source is exhausted, or
 * to the static fallback host, are covered too.
 */
internal class APISourceRecorder {

    data class Request(val endpoint: Endpoint, val host: String)

    private val recordedRequests = CopyOnWriteArrayList<Request>()

    val requests: List<Request>
        get() = recordedRequests.toList()

    inline fun <reified T : Endpoint> hosts(): List<String> = requests.filter { it.endpoint is T }.map { it.host }

    /** Records requests without failing any. */
    fun recordingStrategy(): ForceServerErrorStrategy = strategy(failingHosts = emptySet(), unhealthyHosts = emptySet())

    /** Requests to [hosts] get a 5xx, but their health checks pass, so the SDK doesn't fail over. */
    fun serverErrorStrategy(hosts: Set<String>): ForceServerErrorStrategy =
        strategy(failingHosts = hosts, unhealthyHosts = emptySet())

    /** Requests to [hosts] get a 5xx and their health checks fail, so the SDK fails over to the next source. */
    fun outageStrategy(hosts: Set<String>): ForceServerErrorStrategy =
        strategy(failingHosts = hosts, unhealthyHosts = hosts)

    private fun strategy(failingHosts: Set<String>, unhealthyHosts: Set<String>) = object : ForceServerErrorStrategy {
        override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean {
            recordedRequests.add(Request(endpoint, baseURL.host))
            return baseURL.host in failingHosts
        }

        override fun shouldForceConnectionFailure(url: String): Boolean = URL(url).host in unhealthyHosts
    }
}
