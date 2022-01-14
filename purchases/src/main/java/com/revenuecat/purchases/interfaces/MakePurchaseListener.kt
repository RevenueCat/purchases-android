//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//
package com.revenuecat.purchases.interfaces

import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.PurchaserInfo

/**
 * Interface to be implemented when making purchases.
 */
@Deprecated(
    """
       Replace with PurchaseCallback, which returns a StoreTransaction instead of a Purchase, and a CustomerInfo 
       instead of a PurchaserInfo. You can use `MakePurchaseListener.toPurchaseCallback()` in Kotlin for an 
       easy migration 
    """,
    replaceWith = ReplaceWith("PurchaseCallback")
)
interface MakePurchaseListener : PurchaseErrorListener {
    /**
     * Will be called after the purchase has completed
     * @param purchase Purchase object for the purchased product.
     * @param purchaserInfo Updated [PurchaserInfo].
     */
    fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo)
}
