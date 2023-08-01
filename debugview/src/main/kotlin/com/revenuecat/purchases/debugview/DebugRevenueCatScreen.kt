package com.revenuecat.purchases.debugview

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.models.StoreTransaction

@Composable
fun DebugRevenueCatScreen(
    onPurchaseCompleted: (StoreTransaction) -> Unit,
    onPurchaseErrored: (PurchasesTransactionException) -> Unit,
) {
    InternalDebugRevenueCatScreen(onPurchaseCompleted, onPurchaseErrored)
}
