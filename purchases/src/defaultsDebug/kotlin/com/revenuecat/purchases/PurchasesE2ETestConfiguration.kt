package com.revenuecat.purchases

import android.annotation.SuppressLint
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.utils.DefaultIsDebugBuildProvider
import java.net.URL

/** Debug-only configuration entry point used by the E2E test app. */
@InternalRevenueCatAPI
@JvmSynthetic
public fun Purchases.Companion.configureForE2ETests(
    configuration: PurchasesConfiguration,
    currentForceServerErrorStrategy: () -> E2EForceServerErrorStrategy,
): Purchases {
    val forceServerErrorStrategy = object : ForceServerErrorStrategy {
        override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean = false

        override fun modifyRequestURL(url: URL, endpoint: Endpoint): URL {
            return when (currentForceServerErrorStrategy()) {
                E2EForceServerErrorStrategy.Never -> url
                E2EForceServerErrorStrategy.RemoteConfigNetworkError -> {
                    if (endpoint.isRemoteConfig()) remoteConfigOfflineURL else url
                }
            }
        }
    }

    return PurchasesFactory(
        isDebugBuild = DefaultIsDebugBuildProvider(configuration.context),
    ).createPurchases(
        configuration = configuration,
        platformInfo = platformInfo,
        proxyURL = proxyURL,
        forceServerErrorStrategy = forceServerErrorStrategy,
    ).also {
        @SuppressLint("RestrictedApi")
        sharedInstance = it
        serviceDispatcher.initialize(it)
    }
}

private fun Endpoint.isRemoteConfig(): Boolean =
    this is Endpoint.GetRemoteConfig || this is Endpoint.GetRemoteConfigFallback

private val remoteConfigOfflineURL = URL("https://config-offline.invalid/")
