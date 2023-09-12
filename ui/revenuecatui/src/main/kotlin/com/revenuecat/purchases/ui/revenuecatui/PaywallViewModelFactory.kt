package com.revenuecat.purchases.ui.revenuecatui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.Offering

internal class PaywallViewModelFactory(
    private val offering: Offering?,
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PaywallViewModelImpl(offering) as T
    }
}
