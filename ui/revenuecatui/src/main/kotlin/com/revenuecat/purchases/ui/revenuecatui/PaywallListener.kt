package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable

public interface PaywallListener {
    /**
     * Called when a package purchase is about to be initiated, before the payment sheet is displayed.
     * This allows the app to perform any necessary preparation (e.g., authentication) before proceeding.
     *
     * @param rcPackage: The Package being purchased.
     * @param resume A callback that must be invoked to continue with the purchase flow.
     *               If not called, the purchase flow will not proceed.
     */
    public fun onPurchasePackageInitiated(rcPackage: Package, resume: Resumable) {
        // Default implementation immediately resumes
        resume()
    }

    public fun onPurchaseStarted(rcPackage: Package) {}
    public fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {}
    public fun onPurchaseError(error: PurchasesError) {}
    public fun onPurchaseCancelled() {}
    public fun onRestoreStarted() {}
    public fun onRestoreCompleted(customerInfo: CustomerInfo) {}
    public fun onRestoreError(error: PurchasesError) {}
}
