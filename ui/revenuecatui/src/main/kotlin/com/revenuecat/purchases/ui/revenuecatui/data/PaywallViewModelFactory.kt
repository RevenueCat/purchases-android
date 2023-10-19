package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.material3.ColorScheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext

internal class PaywallViewModelFactory(
    private val applicationContext: ApplicationContext,
    private val options: PaywallOptions,
    private val colorScheme: ColorScheme,
    private val preview: Boolean = false,
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PaywallViewModelImpl(
            applicationContext = applicationContext,
            options = options,
            colorScheme = colorScheme,
            preview = preview,
        ) as T
    }
}
