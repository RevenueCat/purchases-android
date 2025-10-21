package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction

/**
 * Represents the type of purchase flow being initiated.
 */
sealed class PurchaseFlowType {
    /**
     * A standard package purchase flow.
     * @param packageId The identifier of the package being purchased.
     */
    data class Standard(val packageId: String) : PurchaseFlowType()

    /**
     * A restore purchases flow.
     */
    object Restore : PurchaseFlowType()

    /**
     * An offer code redemption flow.
     */
    object OfferCode : PurchaseFlowType()
}

interface PaywallListener {
    /**
     * Called when a purchase flow is about to be initiated, before the payment sheet is displayed.
     * This allows the app to perform any necessary preparation (e.g., authentication) before proceeding.
     *
     * @param flowType The type of purchase flow being initiated.
     * @param resume A callback that must be invoked to continue with the purchase flow.
     *               If not called, the purchase flow will not proceed.
     */
    fun onPurchaseFlowInitiated(flowType: PurchaseFlowType, resume: () -> Unit) {
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
