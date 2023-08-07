package com.revenuecat.apitester.kotlin

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.debugview.DebugRevenueCatBottomSheet
import com.revenuecat.purchases.ui.debugview.DebugRevenueCatScreen

@Suppress("unused")
private class PurchasesDebugViewAPI {
    @Composable
    fun CheckDebugView(
        onPurchaseCompleted: (StoreTransaction) -> Unit,
        onPurchaseErrored: (PurchasesTransactionException) -> Unit,
        isVisible: Boolean,
        onDismissCallback: (() -> Unit)? = null,
    ) {
        DebugRevenueCatScreen(
            onPurchaseCompleted = onPurchaseCompleted,
            onPurchaseErrored = onPurchaseErrored,
        )
        DebugRevenueCatBottomSheet(
            onPurchaseCompleted = onPurchaseCompleted,
            onPurchaseErrored = onPurchaseErrored,
            isVisible = isVisible,
            onDismissCallback = onDismissCallback,
        )
    }
}
