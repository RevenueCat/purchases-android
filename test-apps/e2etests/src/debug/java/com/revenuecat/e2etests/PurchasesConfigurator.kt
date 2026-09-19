package com.revenuecat.e2etests

import com.revenuecat.purchases.E2EForceServerErrorStrategy
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.configureForE2ETests
import java.util.concurrent.atomic.AtomicReference

@OptIn(InternalRevenueCatAPI::class)
private val forceServerErrorStrategy = AtomicReference(E2EForceServerErrorStrategy.Never)

@OptIn(InternalRevenueCatAPI::class)
internal fun configurePurchases(
    configuration: PurchasesConfiguration,
    initialForceServerErrorStrategy: String? = null,
) {
    forceServerErrorStrategy.set(initialForceServerErrorStrategy.toForceServerErrorStrategy())
    Purchases.configureForE2ETests(configuration, forceServerErrorStrategy::get)
}

@OptIn(InternalRevenueCatAPI::class)
private fun String?.toForceServerErrorStrategy(): E2EForceServerErrorStrategy = when (this) {
    "remote_config_network_error" -> E2EForceServerErrorStrategy.RemoteConfigNetworkError
    else -> E2EForceServerErrorStrategy.Never
}
