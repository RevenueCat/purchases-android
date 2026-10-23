package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import androidx.compose.material3.ColorScheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterPreviewProvider
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType

@OptIn(InternalRevenueCatAPI::class)
internal class CustomerCenterViewModelFactory(
    private val purchases: PurchasesType,
    private val colorScheme: ColorScheme,
    private val isDarkMode: Boolean,
    private val listener: CustomerCenterListener? = null,
    private val previewProvider: CustomerCenterPreviewProvider? = null,
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CustomerCenterViewModelImpl(
            purchases,
            colorScheme = colorScheme,
            isDarkMode = isDarkMode,
            listener = listener,
            previewProvider = previewProvider,
        ) as T
    }
}
