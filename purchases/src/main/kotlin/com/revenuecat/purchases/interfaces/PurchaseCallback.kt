package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction

public interface PurchaseCallback : PurchaseErrorCallback {
    /**
     * Will be called after the purchase has completed
     * @param storeTransaction StoreTransaction object for the purchased product.
     * @param customerInfo Updated [CustomerInfo].
     */
    fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo)
}
