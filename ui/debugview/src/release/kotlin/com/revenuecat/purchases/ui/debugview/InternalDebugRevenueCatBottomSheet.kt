package com.revenuecat.purchases.ui.debugview

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.models.StoreTransaction

@Suppress("EmptyFunctionBlock", "UnusedParameter")
@Composable
internal fun InternalDebugRevenueCatBottomSheet(
    onPurchaseCompleted: (StoreTransaction) -> Unit,
    onPurchaseErrored: (PurchasesTransactionException) -> Unit,
    isVisible: Boolean = false,
    onDismissCallback: (() -> Unit)? = null,
) {}
