package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.Logger

/**
 * Invalidates the virtual currencies cache if the SDK is configured.
 *
 * Called after reward verification grants a virtual-currency reward so the next
 * [Purchases.getVirtualCurrencies] fetch returns the updated balance instead of a stale cached value.
 * If [Purchases] has not been configured yet, logs a warning and skips invalidation.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun invalidateVirtualCurrenciesCacheIfConfigured() {
    if (!Purchases.isConfigured) {
        Logger.w(
            "Purchases is not configured. " +
                "Skipping virtual currencies cache invalidation after reward verification.",
        )
        return
    }
    Purchases.sharedInstance.invalidateVirtualCurrenciesCache()
}
