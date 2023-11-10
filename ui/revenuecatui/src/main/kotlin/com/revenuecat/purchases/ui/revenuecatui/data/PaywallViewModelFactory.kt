package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.material3.ColorScheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
internal class PaywallViewModelFactory(
    private val resourceProvider: ResourceProvider,
    private val options: PaywallOptions,
    private val colorScheme: ColorScheme,
    private val isDarkMode: Boolean,
    private val preview: Boolean = false,
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PaywallViewModelImpl(
            resourceProvider = resourceProvider,
            options = options,
            colorScheme = colorScheme,
            isDarkMode = isDarkMode,
            preview = preview,
        ) as T
    }
}
