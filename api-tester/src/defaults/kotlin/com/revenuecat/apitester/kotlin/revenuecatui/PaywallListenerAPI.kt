package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class PaywallListenerAPI {
    fun check() {
        val listener = object : PaywallListener {
            override fun onPurchaseStarted(rcPackage: Package) {}

            override fun onPurchaseError(error: PurchasesError) {}

            override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {}

            override fun onRestoreStarted() {}

            override fun onRestoreError(error: PurchasesError) {}

            override fun onRestoreCompleted(customerInfo: CustomerInfo) {}
        }
    }
}
