package com.revenuecat.purchases.ui.revenuecatui.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewListener

internal class PaywallViewModelFactory(
    private val applicationContext: Context,
    private val offering: Offering?,
    private val listener: PaywallViewListener?,
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PaywallViewModelImpl(applicationContext, offering, listener) as T
    }
}
