package com.revenuecat.purchases.debugview.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.debugview.DebugRevenueCatViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class InternalDebugRevenueCatScreenViewModel : ViewModel(), DebugRevenueCatViewModel {
    override val state: StateFlow<SettingScreenState>
        get() = _state

    private var _state: MutableStateFlow<SettingScreenState> = MutableStateFlow(
        SettingScreenState.NotConfigured(configurationGroup()),
    )

    init {
        if (Purchases.isConfigured) {
            refreshInfo()
        }
    }

    private fun refreshInfo() {
        viewModelScope.launch {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            _state.update {
                SettingScreenState.Configured(
                    configurationGroup(),
                    customerInfoGroup(customerInfo),
                    offeringsGroup(offerings),
                )
            }
        }
    }

    private fun configurationGroup(): SettingGroupState {
        val storeName = if (Purchases.isConfigured) {
            Purchases.sharedInstance.store.name
        } else {
            "Not configured"
        }
        val observerMode = if (Purchases.isConfigured) {
            "${!Purchases.sharedInstance.finishTransactions}"
        } else {
            "Not configured"
        }
        return SettingGroupState.Loaded(
            "Configuration",
            listOf(
                SettingState.TextLoaded("SDK version", Purchases.frameworkVersion),
                SettingState.TextLoaded("Is configured", "${Purchases.isConfigured}"),
                SettingState.TextLoaded("Store", storeName),
                SettingState.TextLoaded("Observer mode", observerMode),
            ),
        )
    }

    private fun customerInfoGroup(customerInfo: CustomerInfo): SettingGroupState {
        return SettingGroupState.Loaded(
            title = "Customer info",
            settings = listOf(
                SettingState.TextLoaded("Current User ID", Purchases.sharedInstance.appUserID),
                SettingState.TextLoaded("Original User ID", customerInfo.originalAppUserId),
                SettingState.TextLoaded(
                    "Active entitlements",
                    customerInfo.entitlements.active
                        .map { "${it.key} until ${it.value.expirationDate}" }
                        .joinToString("\n")
                        .takeIf { it.isNotEmpty() } ?: "None",
                ),
                SettingState.TextLoaded("Verification result", customerInfo.entitlements.verification.name),
                SettingState.TextLoaded("Request date", customerInfo.requestDate.toString()),
            ),
        )
    }

    private fun offeringsGroup(offerings: Offerings): SettingGroupState {
        return SettingGroupState.Loaded(
            title = "Offerings",
            settings = offerings.all.values.map { offering ->
                SettingState.TextLoaded(
                    title = offering.identifier,
                    content = "TODO",
                )
            },
        )
    }
}
