package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesService

/**
 * [PurchasesService] that wires AdMob reward verification into the [Purchases] lifecycle. Discovered
 * via [java.util.ServiceLoader], so it must keep a no-argument constructor. Owns the
 * [RewardVerificationManager.runtime] lifecycle: a fresh runtime per configuration, dropped on close.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal class RewardVerificationService : PurchasesService {
    override fun initialize(purchases: Purchases) {
        RewardVerificationManager.runtime = RewardVerificationRuntime()
    }

    override fun close(purchases: Purchases) {
        RewardVerificationManager.runtime?.close()
        RewardVerificationManager.runtime = null
    }
}
