package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
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

    @OptIn(InternalRevenueCatAPI::class)
    fun fakeResponseWithoutPerformingRequest(baseURL: URL, endpoint: Endpoint): HTTPResult? {
        return null
    }
}

private fun Endpoint.isRemoteConfig(): Boolean =
    this is Endpoint.GetRemoteConfig || this is Endpoint.GetRemoteConfigFallback

/**
 * Maps the public [ForceServerErrorMode] to a concrete internal [ForceServerErrorStrategy] scoped to
 * the remote-config endpoint. Kept internal so [Endpoint]/[HTTPResult] stay out of the public API.
 */
@OptIn(InternalRevenueCatAPI::class)
internal fun ForceServerErrorMode.toForceServerErrorStrategy(): ForceServerErrorStrategy = when (this) {
    ForceServerErrorMode.REMOTE_CONFIG_NOT_FOUND -> object : ForceServerErrorStrategy {
        override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean = false
        override fun fakeResponseWithoutPerformingRequest(baseURL: URL, endpoint: Endpoint): HTTPResult? =
            if (endpoint.isRemoteConfig()) {
                HTTPResult(
                    responseCode = RCHTTPStatusCodes.NOT_FOUND,
                    payload = "{}",
                    origin = HTTPResult.Origin.BACKEND,
                    requestDate = null,
                    verificationResult = VerificationResult.NOT_REQUESTED,
                    isLoadShedderResponse = false,
                    isFallbackURL = false,
                )
            } else {
                null
            }
    }
    ForceServerErrorMode.REMOTE_CONFIG_NETWORK_ERROR -> object : ForceServerErrorStrategy {
        // A `.invalid` TLD never resolves (RFC 2606), so the config request raises an UnknownHostException.
        override val serverErrorURL: String = "https://config-offline.invalid/"
        override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean = endpoint.isRemoteConfig()
    }
}
