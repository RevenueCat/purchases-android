package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import java.net.URL

internal fun interface ForceServerErrorStrategy {
    companion object {
        val doNotFail = ForceServerErrorStrategy { _, _ -> false }
        val failAll = ForceServerErrorStrategy { _, _ -> true }
        val failExceptFallbackUrls = ForceServerErrorStrategy { baseURL, _ ->
            baseURL.toString() != AppConfig.fallbackURL.toString()
        }
    }
    val serverErrorURL: String
        get() = "https://api.revenuecat.com/force-server-failure"

    fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean

    fun modifyRequestURL(url: URL, endpoint: Endpoint): URL = url

    /**
     * Whether a direct [java.net.HttpURLConnection] to [url] should fail as if the host were unreachable. Unlike
     * the rest of this interface, this covers requests that never reach `HTTPClient`:
     * - Remote config blobs are fetched straight from the config CDN, a different host from the API, so forcing
     *   API errors alone leaves them downloading normally. Default `false` keeps that (an API outage does not
     *   imply the CDN is down); return `true` to simulate having no network at all.
     * - API source health checks probe `<source>/v1/health/connectivity`, so returning `true` for that url
     *   reports the source unhealthy. Combined with [shouldForceServerError] for the same source, this simulates
     *   the source being down, and the SDK fails over to the next one.
     */
    fun shouldForceConnectionFailure(url: String): Boolean = false

    @OptIn(InternalRevenueCatAPI::class)
    fun fakeResponseWithoutPerformingRequest(baseURL: URL, endpoint: Endpoint): HTTPResult? {
        return null
    }
}
