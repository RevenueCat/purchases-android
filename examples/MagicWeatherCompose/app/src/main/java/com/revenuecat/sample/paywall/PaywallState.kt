package com.revenuecat.sample.paywall

import androidx.annotation.DrawableRes
import com.revenuecat.purchases.Offering

sealed class PaywallState {
    object Loading : PaywallState()
    data class Error(val message: String) : PaywallState()
    data class Success(
        val offering: Offering,
        val title: String,
        val subtitle: String,
        @DrawableRes val imageResource: Int,
    ) : PaywallState()
}
