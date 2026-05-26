package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases

@OptIn(InternalRevenueCatAPI::class)
internal object RewardVerificationServices {
    val serviceRegistrar: RewardVerificationServiceRegistrar =
        RewardVerificationServiceRegistrar { service -> Purchases.registerService(service) }

    val locator: RewardVerificationServiceLocator = RewardVerificationServiceLocator(serviceRegistrar)
}
