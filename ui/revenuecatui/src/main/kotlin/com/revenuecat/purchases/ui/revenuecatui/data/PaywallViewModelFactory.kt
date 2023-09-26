package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.material3.ColorScheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext

internal class PaywallViewModelFactory(
    private val applicationContext: ApplicationContext,
    private val mode: PaywallViewMode,
    private val offering: Offering?,
    private val listener: PaywallViewListener?,
    private val colorScheme: ColorScheme,
    private val preview: Boolean = false,
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PaywallViewModelImpl(applicationContext, mode, offering, listener, colorScheme, preview) as T
    }
}
