package com.revenuecat.purchases.debugview

import com.revenuecat.purchases.debugview.models.SettingScreenState
import kotlinx.coroutines.flow.StateFlow

internal interface DebugRevenueCatViewModel {
    val state: StateFlow<SettingScreenState>
}
