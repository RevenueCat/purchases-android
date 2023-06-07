package com.revenuecat.sample.paywall

import com.revenuecat.purchases.Offering

sealed class PaywallState {
    object Loading : PaywallState()
    data class Error(val message: String) : PaywallState()
    data class Success(val offering: Offering) : PaywallState()
}
