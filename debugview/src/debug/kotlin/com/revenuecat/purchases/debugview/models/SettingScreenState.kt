package com.revenuecat.purchases.debugview.models

internal sealed class SettingScreenState(open val configuration: SettingGroupState) {
    data class NotConfigured(override val configuration: SettingGroupState) : SettingScreenState(configuration)
    data class Configured(
        override val configuration: SettingGroupState,
        val customerInfo: SettingGroupState,
        val offerings: SettingGroupState,
    ) : SettingScreenState(configuration)

    fun toGroupStates(): List<SettingGroupState> {
        return when (this) {
            is NotConfigured -> listOf(configuration)
            is Configured -> listOf(configuration, customerInfo, offerings)
        }
    }
}
