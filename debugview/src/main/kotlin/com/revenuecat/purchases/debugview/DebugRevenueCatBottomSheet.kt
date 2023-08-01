package com.revenuecat.purchases.debugview

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.models.StoreTransaction

@Composable
fun DebugRevenueCatBottomSheet(
    onPurchaseCompleted: (StoreTransaction) -> Unit,
    onPurchaseErrored: (PurchasesTransactionException) -> Unit,
    isVisible: Boolean = false,
    onDismissCallback: (() -> Unit)? = null,
) {
    InternalDebugRevenueCatBottomSheet(
        onPurchaseCompleted,
        onPurchaseErrored,
        isVisible,
        onDismissCallback,
    )
}
