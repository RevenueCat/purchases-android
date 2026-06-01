package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesService

/**
 * [PurchasesService] that wires AdMob reward verification into the [Purchases] lifecycle. Discovered
 * via [java.util.ServiceLoader], so it must keep a no-argument constructor. Forwards to
 * [RewardVerificationManager]'s shared runtime.
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
