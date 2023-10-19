package com.revenuecat.paywallstester.ui.screens.paywall

import com.revenuecat.purchases.Offering

sealed class PaywallScreenState {
    object Loading : PaywallScreenState()
    data class Error(val errorMessage: String) : PaywallScreenState()
    data class Loaded(
        val offering: Offering,
        val dialogText: String? = null,
        val footerCondensed: Boolean = false,
    ) : PaywallScreenState()
}
