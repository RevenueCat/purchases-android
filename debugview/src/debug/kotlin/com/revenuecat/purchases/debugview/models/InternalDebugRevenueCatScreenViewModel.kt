package com.revenuecat.purchases.debugview.models

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.debugview.DebugRevenueCatViewModel

internal class InternalDebugRevenueCatScreenViewModel : DebugRevenueCatViewModel {
    override val settingGroups: List<SettingGroupState>
        get() = state.toGroupStates()

    private var state: SettingScreenState

    init {
        state = if (Purchases.isConfigured) {
            SettingScreenState.Configured(
                configurationGroup(),
                customerInfoGroup(),
                SettingGroupState.Loading("Offerings"),
            )
        } else {
            SettingScreenState.NotConfigured(
                configurationGroup(),
            )
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

    private fun customerInfoGroup(): SettingGroupState {
        return SettingGroupState.Loading(
            "Customer info",
        )
    }
}
