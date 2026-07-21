package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceHandle
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceProvider
import com.revenuecat.purchases.common.warnLog
import java.net.MalformedURLException
import java.net.URL

/**
 * Decides which API source a request should target and whether a failed request should fail over to
 * the next source. Only connection-level failures and 5xx responses can trigger a failover, and only
 * after a health check against the current source fails: a 2xx health response means the source is
 * healthy (the request failure was something else, e.g. endpoint-specific), so the original error
 * surfaces without switching hosts. A source whose health check returns non-2xx or cannot complete
 * is reported unhealthy and the next one takes over. 4xx responses never fail over: they are request
 * errors, not source problems.
 */
internal class APISourceFailover(
    private val appConfig: AppConfig,
    private val sourceProvider: RemoteConfigSourceProvider,
    private val healthChecker: SourceHealthChecker,
) {

    /** A source handle plus its parsed base URL; only parseable sources are ever handed out. */
    data class ResolvedSource(val handle: RemoteConfigSourceHandle, val url: URL)

    sealed interface FailureDecision {
        /** The current source failed its health check; retry the request on [next]. */
        data class RetryNextSource(val next: ResolvedSource) : FailureDecision

        /** The current source is healthy, so failing over is pointless: surface the original error. */
        object SourceHealthy : FailureDecision

        /** Every source has been reported unhealthy; surface the original error. */
        object SourcesExhausted : FailureDecision
    }

    /**
     * The source a request to [endpoint] should target, or null to keep using [baseURL]. Sources apply
     * only when the `usesRemoteConfigAPISources` dangerous setting is enabled, this is not an endpoint
     * fallback-host attempt, the endpoint opts in via [Endpoint.usesAPISources], and [baseURL] is still
     * the default host (a proxy or an overridden base URL pins the host and bypasses API sources).
     */
    fun currentSource(endpoint: Endpoint, baseURL: URL, isFallbackAttempt: Boolean): ResolvedSource? {
        val eligible = appConfig.usesRemoteConfigAPISources &&
            !isFallbackAttempt &&
            endpoint.usesAPISources &&
            baseURL.toString() == AppConfig.baseUrlString
        return if (eligible) currentResolvedSource() else null
    }

    /**
     * Decides what to do after a request to [source] failed at the connection level or returned a
     * 5xx, health-checking the source to tell a source outage apart from anything else. The health
     * check performs blocking network I/O on the calling thread, so this must always be called from
     * a background thread (as [com.revenuecat.purchases.common.HTTPClient] requests already are).
     */
    fun onRequestFailure(source: ResolvedSource): FailureDecision {
        if (healthChecker.isHealthy(source.handle.url)) {
            log(LogIntent.DEBUG) {
                "API source ${source.handle.url} is healthy despite the request failing; not failing over."
            }
            return FailureDecision.SourceHealthy
        }
        sourceProvider.reportUnhealthy(source.handle)
        return currentResolvedSource()?.let { FailureDecision.RetryNextSource(it) }
            ?: FailureDecision.SourcesExhausted
    }

    /**
     * The provider's current source with its URL parsed. Sources with malformed URLs are reported
     * unhealthy and skipped, so they participate in failover instead of silently pinning requests to
     * the default host. The walk ends when the provider runs out of sources.
     */
    private fun currentResolvedSource(): ResolvedSource? {
        while (true) {
            val handle = sourceProvider.currentAPISource() ?: return null
            try {
                return ResolvedSource(handle, URL(handle.url))
            } catch (e: MalformedURLException) {
                warnLog { "Skipping API source with malformed url ${handle.url}: $e" }
                sourceProvider.reportUnhealthy(handle)
            }
        }
    }
}
