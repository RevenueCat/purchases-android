//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//
package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.toPurchaserInfo

/**
 * Interface to be implemented when making calls that return a [PurchaserInfo]
 */
@Suppress("unused")
@Deprecated(
    "Renamed to ReceiveCustomerInfoListener",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("ReceiveCustomerInfoListener")
)
interface ReceivePurchaserInfoListener {
    /**
     * Will be called after the call has completed.
     * @param purchaserInfo [PurchaserInfo] class sent back when the call has completed
     */
    fun onReceived(purchaserInfo: PurchaserInfo)

    /**
     * Will be called after the call has completed with an error.
     * @param error A [PurchasesError] containing the reason for the failure of the call
     */
    fun onError(error: PurchasesError)
}
