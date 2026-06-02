package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesService

/**
 * [PurchasesService] that wires AdMob reward verification into the [Purchases] lifecycle. Discovered
 * via [java.util.ServiceLoader], so it must keep a no-argument constructor.
 *
 * Owns the [RewardVerificationRuntime] for one configuration: it registers itself as the active service
 * on [initialize] and, on [close], cancels the runtime and deregisters. All verification state lives on
 * this instance, so it is discarded when the service is closed.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal class RewardVerificationService : PurchasesService {
    var runtime: RewardVerificationRuntime? = null
        private set

    override fun initialize(purchases: Purchases) {
        runtime = RewardVerificationRuntime()
        RewardVerificationManager.activeService = this
    }

    override fun close(purchases: Purchases) {
        runtime?.close()
        runtime = null
        if (RewardVerificationManager.activeService === this) {
            RewardVerificationManager.activeService = null
        }
    }
}
