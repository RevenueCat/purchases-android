@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.apitester.kotlin.exhaustive
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPaywallOutcome

@Suppress("unused", "UNUSED_VARIABLE")
private class CheckpointPaywallOutcomeAPI {

    fun check(
        outcome: CheckpointPaywallOutcome,
        customerInfo: CustomerInfo,
        storeTransaction: StoreTransaction,
        error: PurchasesError,
    ) {
        when (outcome) {
            is CheckpointPaywallOutcome.Dismissed -> {}
            is CheckpointPaywallOutcome.Purchased -> {
                val purchasedCustomerInfo: CustomerInfo = outcome.customerInfo
                val purchasedStoreTransaction: StoreTransaction = outcome.storeTransaction
            }
            is CheckpointPaywallOutcome.Restored -> {
                val restoredCustomerInfo: CustomerInfo = outcome.customerInfo
            }
            is CheckpointPaywallOutcome.Finished -> {
                val finishedCustomerInfo: CustomerInfo = outcome.customerInfo
            }
            is CheckpointPaywallOutcome.Error -> {
                val outcomeError: PurchasesError = outcome.error
            }
            is CheckpointPaywallOutcome.WebCheckoutOpened -> {}
            // The hierarchy is closed but not sealed, so consumers must handle cases added later.
            else -> {}
        }.exhaustive

        val dismissed: CheckpointPaywallOutcome = CheckpointPaywallOutcome.Dismissed
        val purchased: CheckpointPaywallOutcome = CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction)
        val restored: CheckpointPaywallOutcome = CheckpointPaywallOutcome.Restored(customerInfo)
        val finished: CheckpointPaywallOutcome = CheckpointPaywallOutcome.Finished(customerInfo)
        val errored: CheckpointPaywallOutcome = CheckpointPaywallOutcome.Error(error)
        val webCheckoutOpened: CheckpointPaywallOutcome = CheckpointPaywallOutcome.WebCheckoutOpened
    }
}
