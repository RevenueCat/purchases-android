package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.material3.ColorScheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewOptions
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext

internal class PaywallViewModelFactory(
    private val applicationContext: ApplicationContext,
    private val mode: PaywallViewMode,
    private val options: PaywallViewOptions,
    private val colorScheme: ColorScheme,
    private val preview: Boolean = false,
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PaywallViewModelImpl(applicationContext, mode, options, colorScheme, preview) as T
    }
}
