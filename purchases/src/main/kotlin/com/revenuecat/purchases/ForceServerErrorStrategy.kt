package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.networking.Endpoint
import java.net.URL

internal interface ForceServerErrorStrategy {
    companion object {
        const val serverErrorURL = "https://api.revenuecat.com/force-server-failure"
        val failAll = object : ForceServerErrorStrategy {
            override fun shouldForceServerError(
                baseURL: URL,
                endpoint: Endpoint,
            ): Boolean {
                return true
            }
        }
        val failExceptFallbackUrls = object : ForceServerErrorStrategy {
            override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean {
                return baseURL.toString() != AppConfig.fallbackURL.toString()
            }
        }
    }
    fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean
}
