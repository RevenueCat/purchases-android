package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases

@OptIn(InternalRevenueCatAPI::class)
internal object RewardVerificationServices {
    val locator: RewardVerificationServiceLocator = RewardVerificationServiceLocator(
        serviceRegistrar = { service -> Purchases.registerService(service) },
    )
}
