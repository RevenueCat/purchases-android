package com.revenuecat.e2etests

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

@Suppress("UNUSED_PARAMETER")
internal fun configurePurchases(
    configuration: PurchasesConfiguration,
    initialForceServerErrorStrategy: String? = null,
) {
    Purchases.configure(configuration)
}
