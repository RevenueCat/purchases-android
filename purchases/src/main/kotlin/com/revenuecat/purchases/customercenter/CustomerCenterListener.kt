package com.revenuecat.purchases.customercenter

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError

/**
 * Listener interface for receiving callbacks for Customer Center events.
 */
public interface CustomerCenterListener {

    /**
     * Called when a restore purchases operation is initiated by the user in the Customer Center.
     */
    public fun onRestoreStarted() {
        // Default empty implementation
    }

    /**
     * Called when a restore purchases operation fails.
     *
     * @param error The error that occurred during the restore operation.
     */
    public fun onRestoreFailed(error: PurchasesError) {
        // Default empty implementation
    }

    /**
     * Called when a restore purchases operation completes successfully.
     *
     * @param customerInfo The updated customer information after the restore.
     */
    public fun onRestoreCompleted(customerInfo: CustomerInfo) {
        // Default empty implementation
    }

    /**
     * Called when the user requests to manage their subscription through the Customer Center.
     * This happens when the user taps on the cancel subscription button, which takes them
     * to the Google Play subscription management screen.
     */
    public fun onShowingManageSubscriptions() {
        // Default empty implementation
    }

    /**
     * Called when the user completes a feedback survey in the Customer Center.
     *
     * @param feedbackSurveyOptionId The ID of the selected feedback option.
     */
    public fun onFeedbackSurveyCompleted(feedbackSurveyOptionId: String) {
        // Default empty implementation
    }

    /**
     * Called when a customer center management option is selected.
     *
     * @param action The selected management action
     */
    public fun onManagementOptionSelected(action: CustomerCenterManagementOption) {
        // Default empty implementation
    }

    /**
     * Called when a custom action is selected in the Customer Center.
     *
     * @param actionIdentifier The unique identifier for the custom action
     * @param purchaseIdentifier The product identifier of the purchase being viewed, if any
     */
    public fun onCustomActionSelected(actionIdentifier: String, purchaseIdentifier: String?) {
        // Default empty implementation
    }
}
