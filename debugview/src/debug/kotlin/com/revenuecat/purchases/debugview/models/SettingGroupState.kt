package com.revenuecat.purchases.debugview.models

internal sealed class SettingGroupState(open val title: String) {
    data class Loading(override val title: String) : SettingGroupState(title)
    data class Loaded(override val title: String, val settings: List<SettingState>) : SettingGroupState(title)
}
