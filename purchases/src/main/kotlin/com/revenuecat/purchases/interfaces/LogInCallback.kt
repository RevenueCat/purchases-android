//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//
package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError

/**
 * Interface to be implemented when calling logIn
 */
public interface LogInCallback {
    /**
     * Will be called after the call has completed.
     * @param customerInfo [CustomerInfo] class sent back when the call has completed
     * @param created [Boolean] true if a new user has been registered in the backend,
     * false if the user had already been registered.
     */
    public fun onReceived(customerInfo: CustomerInfo, created: Boolean)

    /**
     * Will be called after the call has completed with an error.
     * @param error A [PurchasesError] containing the reason for the failure of the call
     */
    public fun onError(error: PurchasesError)
}
