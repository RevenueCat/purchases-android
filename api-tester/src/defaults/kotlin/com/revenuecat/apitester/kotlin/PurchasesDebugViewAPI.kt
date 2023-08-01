package com.revenuecat.apitester.kotlin

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.debugview.DebugRevenueCatBottomSheet
import com.revenuecat.purchases.debugview.DebugRevenueCatScreen
import com.revenuecat.purchases.models.StoreTransaction

@Suppress("unused")
private class PurchasesDebugViewAPI {
    @Composable
    fun CheckDebugView(
        onPurchaseCompleted: (StoreTransaction) -> Unit,
        onPurchaseErrored: (PurchasesTransactionException) -> Unit,
        isVisible: Boolean,
        onDismissCallback: (() -> Unit)? = null,
    ) {
        DebugRevenueCatScreen(onPurchaseCompleted, onPurchaseErrored)
        DebugRevenueCatBottomSheet(
            onPurchaseCompleted = onPurchaseCompleted,
            onPurchaseErrored = onPurchaseErrored,
            isVisible = isVisible,
            onDismissCallback = onDismissCallback,
        )
    }
}
