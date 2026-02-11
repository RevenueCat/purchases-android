package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError

public interface SyncPurchasesCallback {
    fun onSuccess(customerInfo: CustomerInfo)
    fun onError(error: PurchasesError)
}
