package com.revenuecat.purchases.debugview

import com.revenuecat.purchases.debugview.models.SettingGroupState

internal interface DebugRevenueCatViewModel {
    val settingGroups: List<SettingGroupState>
}
