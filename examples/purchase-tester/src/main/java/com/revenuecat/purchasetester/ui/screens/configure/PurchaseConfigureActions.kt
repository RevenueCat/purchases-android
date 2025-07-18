package com.revenuecat.purchasetester.ui.screens.configure

import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.PurchasesAreCompletedBy

data class SupportedStores(
    val googleSupported: Boolean = true,
    val amazonSupported: Boolean = true
)

sealed class PurchaseConfigureActions {
    data object OnContinue : PurchaseConfigureActions()
    data class OnApiKeyChanged(val apiKey: String) : PurchaseConfigureActions()
    data class OnProxyUrlChanged(val proxyUrl: String) : PurchaseConfigureActions()
    data class OnEntitlementVerificationModeChanged(val mode: EntitlementVerificationMode) : PurchaseConfigureActions()
    data class OnStoreChanged(val useAmazon: Boolean) : PurchaseConfigureActions()
    data class OnPurchaseCompletionChanged(val completedBy: PurchasesAreCompletedBy) : PurchaseConfigureActions()
    data object OnErrorDismissed : PurchaseConfigureActions()
}
