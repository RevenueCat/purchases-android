//  Purchases
//
//  Copyright © 2025 RevenueCat, Inc. All rights reserved.
//
package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.PurchasesError

/**
 * Interface to be implemented when calling [Purchases.getStorefrontCountryCode]
 */
public interface GetStorefrontCallback {
    /**
     * Will be called after the call has completed.
     * @param storefrontCountryCode [String] containing the Store country code in ISO-3166-1 alpha2
     */
    public fun onReceived(storefrontCountryCode: String)

    /**
     * Will be called after the call has completed with an error.
     * @param error A [PurchasesError] containing the reason for the failure of the call
     */
    public fun onError(error: PurchasesError)
}
