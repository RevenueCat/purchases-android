package com.revenuecat.purchases.debugview

import android.app.Activity
import androidx.compose.runtime.Composable
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.models.StoreTransaction

@Suppress("EmptyFunctionBlock", "UnusedParameter")
@Composable
internal fun InternalDebugRevenueCatScreen(
    onPurchaseCompleted: (StoreTransaction) -> Unit,
    onPurchaseErrored: (PurchasesTransactionException) -> Unit,
    screenViewModel: DebugRevenueCatViewModel? = null,
    activity: Activity? = null,
) {}
