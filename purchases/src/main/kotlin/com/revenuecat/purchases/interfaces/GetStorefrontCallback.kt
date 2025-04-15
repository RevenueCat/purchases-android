//  Purchases
//
//  Copyright © 2025 RevenueCat, Inc. All rights reserved.
//
package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError

/**
 * Interface to be implemented when calling [Purchases.getStorefrontCountryCode]
 */
interface GetStorefrontCallback {
    /**
     * Will be called after the call has completed.
     * @param customerInfo [CustomerInfo] class sent back when the call has completed
     */
    fun onReceived(storefrontCountryCode: String)

    /**
     * Will be called after the call has completed with an error.
     * @param error A [PurchasesError] containing the reason for the failure of the call
     */
    fun onError(error: PurchasesError)
}
