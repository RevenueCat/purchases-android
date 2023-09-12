package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.Offering

@Suppress("ReturnCount")
internal fun Offering.toPaywallViewState(): PaywallViewState {
    val paywallData = this.paywall
        ?: return PaywallViewState.Error("No paywall data for offering: $identifier")
    val packageById = this.availablePackages.associateBy { it.identifier }
    val packages = paywallData.config.packages.map { packageId ->
        packageById[packageId] ?: return PaywallViewState.Error("No package $packageId in offering $identifier")
    }
    return PaywallViewState.Template2(
        paywallData = paywallData,
        packages = packages,
        selectedPackage = packages.first(), // TODO-PAYWALLS: Use correct default package
        shouldShowRestorePurchasesButton = paywallData.config.displayRestorePurchases,
        shouldShowTermsButton = paywallData.config.termsOfServiceURL != null,
        shouldShowPrivacyPolicyButton = paywallData.config.privacyURL != null,
    )
}
