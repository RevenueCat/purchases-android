package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.revenuecat.purchases.UiConfig

internal data class ScreenConditionSnapshot(
    val condition: ScreenCondition = ScreenCondition.COMPACT,
    val orientation: ScreenOrientation = ScreenOrientation.UNKNOWN,
    val screenSize: UiConfig.AppConfig.ScreenSize? = null,
)

internal enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE,
    UNKNOWN,
}

internal val LocalScreenCondition = staticCompositionLocalOf { ScreenConditionSnapshot() }
