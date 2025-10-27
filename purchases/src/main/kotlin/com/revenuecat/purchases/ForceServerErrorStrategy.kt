package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.networking.Endpoint
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
}
