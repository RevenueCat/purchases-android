//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction

/**
 * Interface to be implemented when upgrading or downgrading a purchase.
 */
interface ProductChangeCallback : PurchaseErrorCallback {
    /**
     * Will be called after the product change has been completed
     * @param storeTransaction StoreTransaction object for the purchased product.
     * Will be null if the change is deferred.
     * @param customerInfo Updated [CustomerInfo].
     */
    fun onCompleted(storeTransaction: StoreTransaction?, customerInfo: CustomerInfo)
}
