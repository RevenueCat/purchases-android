package com.revenuecat.purchases.debugview.models

internal sealed class SettingScreenState(
    open val configuration: SettingGroupState,
    open val toastMessage: String? = null,
) {
    data class NotConfigured(
        override val configuration: SettingGroupState,
        override val toastMessage: String? = null,
    ) : SettingScreenState(configuration, toastMessage)
    data class Configured(
        override val configuration: SettingGroupState,
        val customerInfo: SettingGroupState,
        val offerings: SettingGroupState,
        override val toastMessage: String? = null,
    ) : SettingScreenState(configuration, toastMessage)

    fun toSettingGroupStates(): List<SettingGroupState> {
        return when (this) {
            is NotConfigured -> listOf(configuration)
            is Configured -> listOf(configuration, customerInfo, offerings)
        }
    }
}
