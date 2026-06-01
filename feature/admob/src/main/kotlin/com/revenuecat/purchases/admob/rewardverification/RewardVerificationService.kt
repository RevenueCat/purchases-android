package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesService

/**
 * [PurchasesService] entry point for AdMob reward verification.
 *
 * Discovered through [java.util.ServiceLoader] (see
 * `META-INF/services/com.revenuecat.purchases.PurchasesService`), so it must keep a public
 * no-argument constructor. It forwards lifecycle callbacks to [RewardVerificationManager]'s shared
 * runtime, which is the same instance that [RewardVerificationManager.install] and
 * [RewardVerificationManager.handleRewardEarned] operate on.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class RewardVerificationService : PurchasesService {
    override fun initialize(purchases: Purchases) {
        RewardVerificationManager.runtime.initialize()
    }

    override fun close(purchases: Purchases) {
        RewardVerificationManager.runtime.close()
    }
}
