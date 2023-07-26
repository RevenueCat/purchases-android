package com.revenuecat.purchases.debugview.models

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.debugview.DebugRevenueCatViewModel
import com.revenuecat.purchases.getCustomerInfoWith

internal class InternalDebugRevenueCatScreenViewModel : DebugRevenueCatViewModel {
    override val settingGroups: List<SettingGroupState>
        get() = state.toGroupStates()

    private var state: SettingScreenState = SettingScreenState.NotConfigured(
        configurationGroup(),
    )

    init {
        if (Purchases.isConfigured) {
            refreshCustomerInfo()
        }
    }

    private fun refreshCustomerInfo() {
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = {},
            onSuccess = {
                state = SettingScreenState.Configured(
                    configurationGroup(),
                    customerInfoGroup(it),
                    SettingGroupState.Loading("Offerings"),
                )
            },
        )
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
                SettingState.TextLoaded("Customer ID", customerInfo.originalAppUserId),
                SettingState.TextLoaded(
                    "Active entitlements",
                    customerInfo.entitlements.active
                        .map { "${it.key} until ${it.value.expirationDate}" }
                        .joinToString("\n")
                        .takeIf { it.isNotEmpty() } ?: "None",
                ),
            ),
        )
    }
}
