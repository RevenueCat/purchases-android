package com.revenuecat.purchases.ui.debugview

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.models.StoreTransaction

/**
 * Composable function that allows to display the debug screen as any part of the screen in your composable tree.
 *
 * @param onPurchaseCompleted Callback that will be called when a purchase is completed within the debug screen.
 * @param onPurchaseErrored Callback that will be called when a purchase fails or is cancelled within the debug screen.
 */
@Composable
fun DebugRevenueCatScreen(
    onPurchaseCompleted: (StoreTransaction) -> Unit,
    onPurchaseErrored: (PurchasesTransactionException) -> Unit,
) {
    InternalDebugRevenueCatScreen(onPurchaseCompleted, onPurchaseErrored)
}
