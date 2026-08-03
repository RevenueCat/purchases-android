package com.revenuecat.e2etests

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

internal fun configurePurchases(
    configuration: PurchasesConfiguration,
) {
    Purchases.configure(configuration)
}

internal fun armRemoteConfigKillSwitchForE2ETests() = Unit
