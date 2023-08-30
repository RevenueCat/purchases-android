package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError

interface PaywallViewListener {
    fun onPurchaseStarted(rcPackage: Package) {}
    fun onPurchaseCompleted(customerInfo: CustomerInfo) {}
    fun onPurchaseError(error: PurchasesError) {}
    fun onRestoreStarted() {}
    fun onRestoreCompleted(customerInfo: CustomerInfo) {}
    fun onRestoreError(error: PurchasesError) {}
    fun onDismissed() {}
}
