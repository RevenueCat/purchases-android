package com.revenuecat.purchasetester.ui.model.configure

import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchasetester.ui.screens.configure.SupportedStores

data class PurchaseConfigureState(
    val apiKey: String = "",
    val proxyUrl: String = "",
    val entitlementVerificationMode: EntitlementVerificationMode = EntitlementVerificationMode.INFORMATIONAL,
    val useAmazonStore: Boolean = false,
    val purchasesAreCompletedBy: PurchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val supportedStores: SupportedStores = SupportedStores(),
    val isConfigured: Boolean = false
)