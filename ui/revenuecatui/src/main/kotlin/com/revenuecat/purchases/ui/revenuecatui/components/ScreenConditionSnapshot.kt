package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.UiConfig

internal data class ScreenConditionSnapshot(
    val orientation: ScreenOrientation = ScreenOrientation.UNKNOWN,
    val screenSize: UiConfig.AppConfig.ScreenSize? = null,
)

internal enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE,
    UNKNOWN,
}
