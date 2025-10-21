package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction

interface PaywallListener {
    /**
     * Called when a package purchase is about to be initiated, before the payment sheet is displayed.
     * This allows the app to perform any necessary preparation (e.g., authentication) before proceeding.
     *
     * @param packageId The identifier of the package being purchased.
     * @param resume A callback that must be invoked to continue with the purchase flow.
     *               If not called, the purchase flow will not proceed.
     */
    fun onPurchasePackageInitiated(packageId: String, resume: () -> Unit) {
        // Default implementation immediately resumes
        resume()
    }

    /**
     * Called when a restore purchases flow is about to be initiated.
     * This allows the app to perform any necessary preparation (e.g., authentication) before proceeding.
     *
     * @param resume A callback that must be invoked to continue with the restore flow.
     *               If not called, the restore flow will not proceed.
     */
    fun onRestoreInitiated(resume: () -> Unit) {
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
