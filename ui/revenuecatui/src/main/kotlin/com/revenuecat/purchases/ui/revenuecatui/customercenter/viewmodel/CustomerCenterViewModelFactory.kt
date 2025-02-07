package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import androidx.compose.material3.ColorScheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType

internal class CustomerCenterViewModelFactory(
    private val purchases: PurchasesType,
    private val colorScheme: ColorScheme,
    private val isDarkMode: Boolean,
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CustomerCenterViewModelImpl(
            purchases,
            colorScheme = colorScheme,
            isDarkMode = isDarkMode,
        ) as T
    }
}
