package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData

internal sealed class PaywallViewState {
    object Loading : PaywallViewState()
    data class Error(val errorMessage: String) : PaywallViewState()
    data class Template2(
        val paywallData: PaywallData,
        val packages: List<Package>,
        val selectedPackage: Package,
        val shouldShowRestorePurchasesButton: Boolean,
        val shouldShowTermsButton: Boolean,
        val shouldShowPrivacyPolicyButton: Boolean,
    ) : PaywallViewState()
}
