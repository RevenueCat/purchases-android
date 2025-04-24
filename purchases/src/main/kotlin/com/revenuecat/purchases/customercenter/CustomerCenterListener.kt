package com.revenuecat.purchases.customercenter

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError

/**
 * Listener interface for receiving callbacks for Customer Center events.
 */
interface CustomerCenterListener {

    /**
     * Called when a restore purchases operation is initiated by the user in the Customer Center.
     */
    fun onRestoreStarted() {
        // Default empty implementation
    }

    /**
     * Called when a restore purchases operation fails.
     *
     * @param error The error that occurred during the restore operation.
     */
    fun onRestoreFailed(error: PurchasesError) {
        // Default empty implementation
    }

    /**
     * Called when a restore purchases operation completes successfully.
     *
     * @param customerInfo The updated customer information after the restore.
     */
    fun onRestoreCompleted(customerInfo: CustomerInfo) {
        // Default empty implementation
    }

    /**
     * Called when the user requests to manage their subscription through the Customer Center.
     * This happens when the user taps on the cancel subscription button, which takes them
     * to the Google Play subscription management screen.
     */
    fun onShowingManageSubscriptions() {
        // Default empty implementation
    }

    /**
     * Called when the user completes a feedback survey in the Customer Center.
     *
     * @param feedbackSurveyOptionId The ID of the selected feedback option.
     */
    fun onFeedbackSurveyCompleted(feedbackSurveyOptionId: String) {
        // Default empty implementation
    }

    /**
     * Called when a customer center management option is selected.
     *
     * @param action The selected management action
     */
    fun onManagementOptionSelected(action: CustomerCenterManagementOption) {
        // Default empty implementation
    }
}
