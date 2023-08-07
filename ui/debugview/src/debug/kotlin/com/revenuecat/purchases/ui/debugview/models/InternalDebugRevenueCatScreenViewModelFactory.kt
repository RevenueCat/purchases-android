package com.revenuecat.purchases.ui.debugview.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.models.StoreTransaction

class InternalDebugRevenueCatScreenViewModelFactory(
    private val onPurchaseCompleted: (StoreTransaction) -> Unit,
    private val onPurchaseErrored: (PurchasesTransactionException) -> Unit,
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = InternalDebugRevenueCatScreenViewModel(
        onPurchaseCompleted,
        onPurchaseErrored,
    ) as T
}
