package com.revenuecat.purchases.debugview

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.models.StoreTransaction

/**
 * Composable function that allows to display the debug screen as a bottom sheet.
 *
 * @param onPurchaseCompleted Callback that will be called when a purchase is completed within the debug screen.
 * @param onPurchaseErrored Callback that will be called when a purchase fails or is cancelled within the debug screen.
 * @param isVisible Whether the bottom sheet should be visible or not.
 * @param onDismissCallback Callback that will be called when the bottom sheet is dismissed.
 */
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
