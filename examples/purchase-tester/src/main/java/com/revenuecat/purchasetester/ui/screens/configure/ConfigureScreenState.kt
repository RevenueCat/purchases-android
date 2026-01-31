package com.revenuecat.purchasetester.ui.screens.configure

import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.PurchasesAreCompletedBy

enum class StoreType {
    GOOGLE,
    AMAZON
}

sealed class ConfigureScreenState {

    object Loading : ConfigureScreenState()
    data class ConfigureScreenData(
        val apiKey: String = "",
        val proxyUrl: String = "",
        val entitlementVerificationMode: EntitlementVerificationMode = EntitlementVerificationMode.INFORMATIONAL,
        val selectedStoreType: StoreType = StoreType.GOOGLE,
        val purchasesAreCompletedBy: PurchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
    ) : ConfigureScreenState()

}

sealed interface ConfigureUiEvent {
    data class Error(val message: String) : ConfigureUiEvent
    object Success: ConfigureUiEvent

}

sealed interface ValidationResult {
    object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}
