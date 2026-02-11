package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import java.util.Locale

/**
 * Interface to be implemented when calling [getStorefrontLocale][com.revenuecat.purchases.Purchases.getStorefrontLocale]
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public interface GetStorefrontLocaleCallback {
    /**
     * Will be called after the call has completed.
     * @param storefrontLocale **Note:** this locale only has a region set.
     */
    public fun onReceived(storefrontLocale: Locale)

    /**
     * Will be called after the call has completed with an error.
     * @param error A [com.revenuecat.purchases.PurchasesError] containing the reason for the failure of the call
     */
    public fun onError(error: PurchasesError)
}
