package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.Offering

sealed class PaywallViewState {
    object Loading : PaywallViewState()
    data class Error(val errorMessage: String) : PaywallViewState()
    data class Loaded(val offering: Offering) : PaywallViewState()
}
