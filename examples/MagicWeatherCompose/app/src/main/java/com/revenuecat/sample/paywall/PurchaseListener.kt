package com.revenuecat.sample.paywall

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError

interface PurchaseListener {
    fun onPurchaseStarted(packageToPurchase: Package)
    fun onPurchaseCompleted(customerInfo: CustomerInfo)
    fun onPurchaseCancelled()
    fun onPurchaseErrored(error: PurchasesError)
}
