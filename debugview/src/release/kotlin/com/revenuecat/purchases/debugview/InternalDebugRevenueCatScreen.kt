package com.revenuecat.purchases.debugview

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.models.StoreTransaction

@Suppress("EmptyFunctionBlock")
@Composable
internal fun InternalDebugRevenueCatScreen(
    onPurchaseCompleted: (StoreTransaction) -> Unit,
    onPurchaseErrored: (PurchasesTransactionException) -> Unit,
) {}
