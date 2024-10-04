package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType

internal class CustomerCenterViewModelFactory(
    private val purchases: PurchasesType,
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CustomerCenterViewModelImpl(purchases) as T
    }
}
