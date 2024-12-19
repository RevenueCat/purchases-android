package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class ManageSubscriptionsViewModelFactory(
    private val purchases: PurchasesType,
    private val screen: CustomerCenterConfigData.Screen,
    private val purchaseInformation: PurchaseInformation? = null,
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManageSubscriptionsViewModel(purchases, screen, purchaseInformation) as T
    }
}
