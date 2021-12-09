//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//
package com.revenuecat.purchases.interfaces

import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.PurchaserInfo

/**
 * Interface to be implemented when upgrading or downgrading a purchase.
 */
@Deprecated(
    """
       Replace with ProductChangeCallback, which returns a StoreTransaction instead of a Purchase, and a CustomerInfo 
       instead of a PurchaserInfo. You can use `ProductChangeListener.toProductChangeCallback()` in Kotlin for an 
       easy migration 
    """,
    replaceWith = ReplaceWith("ProductChangeCallback")
)
interface ProductChangeListener : PurchaseErrorListener {
    /**
     * Will be called after the product change has been completed
     * @param purchase Purchase object for the purchased product. Will be null if the change is deferred.
     * @param purchaserInfo Updated [PurchaserInfo].
     */
    fun onCompleted(purchase: Purchase?, purchaserInfo: PurchaserInfo)
}
