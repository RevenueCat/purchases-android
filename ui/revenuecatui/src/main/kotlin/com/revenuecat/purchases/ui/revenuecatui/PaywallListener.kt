package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable

@Suppress("TooManyFunctions")
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

    /**
     * Called when restoring purchases is about to be initiated, before the restore flow starts.
     * This allows the app to perform any necessary preparation (e.g., authentication) before proceeding.
     *
     * @param resume A callback that must be invoked to continue with the restore flow.
     *               If not called, the restore flow will not proceed.
     */
    public fun onRestoreInitiated(resume: Resumable) {
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

    /**
     * Called when the user taps a web checkout CTA and the external payment URL was opened.
     * Distinct from cancellation: the user has not cancelled, they left to pay externally.
     */
    public fun onWebCheckoutOpened() {}

    /**
     * Called after the paywall successfully opened a URL, either from a button with a URL destination or from a
     * link inside a text component. Called for all opening methods: in-app browser, external browser and deep link.
     *
     * Not called for web checkout URLs. Use [onWebCheckoutOpened] for those.
     *
     * @param url The URL that was opened.
     */
    public fun onUrlOpened(url: String) {}

    /**
     * Called when the user interacts with a paywall control.
     *
     * Exceptions thrown here are logged and do not affect the paywall.
     *
     * @param event The [PaywallInteractionEvent] containing the event data.
     */
    public fun onInteraction(event: PaywallInteractionEvent) {}
}
