package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction

public interface PaywallListener {
    public fun onPurchaseStarted(rcPackage: Package) {}
    public fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {}
    public fun onPurchaseError(error: PurchasesError) {}
    public fun onPurchaseCancelled() {}
    public fun onRestoreStarted() {}
    public fun onRestoreCompleted(customerInfo: CustomerInfo) {}
    public fun onRestoreError(error: PurchasesError) {}
}
