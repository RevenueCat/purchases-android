package com.revenuecat.purchasetester.ui.model.login

data class PurchaseLoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToOverview: Boolean = false,
    val navigateToConfigure: Boolean = false,
    val navigateToLogs: Boolean = false,
    val navigateToProxy: Boolean = false
)