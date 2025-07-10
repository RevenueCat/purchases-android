package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError

public interface SyncPurchasesCallback {
    public fun onSuccess(customerInfo: CustomerInfo)
    public fun onError(error: PurchasesError)
}
