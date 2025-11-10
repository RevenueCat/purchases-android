package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable

interface PaywallListener {
    /**
     * Called when a package purchase is about to be initiated, before the payment sheet is displayed.
     * This allows the app to perform any necessary preparation (e.g., authentication) before proceeding.
     *
     * @param rcPackage: The Package being purchased.
     * @param resume A callback that must be invoked to continue with the purchase flow.
     *               If not called, the purchase flow will not proceed.
     */
    fun onPurchasePackageInitiated(rcPackage: Package, resume: Resumable) {
        // Default implementation immediately resumes
        resume()
    }

    fun onPurchaseStarted(rcPackage: Package) {}
    fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {}
    fun onPurchaseError(error: PurchasesError) {}
    fun onPurchaseCancelled() {}
    fun onRestoreStarted() {}
    fun onRestoreCompleted(customerInfo: CustomerInfo) {}
    fun onRestoreError(error: PurchasesError) {}
}
