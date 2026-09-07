package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toSize
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * The current window size in dp, for window size condition evaluation. Reads
 * [currentWindowSize]'s window metrics, so it updates automatically on rotation,
 * resize, and multi-window changes — the same source the window size classes
 * derive from.
 */
@JvmSynthetic
@Composable
internal fun currentWindowDpSize(): DpSize = with(LocalDensity.current) {
    currentWindowSize().toSize().toDpSize()
}

internal enum class ScreenCondition {
    COMPACT,
    MEDIUM,
    EXPANDED,
    ;

    companion object {
        @JvmSynthetic
        fun from(sizeClass: WindowWidthSizeClass) =
            when (sizeClass) {
                WindowWidthSizeClass.COMPACT -> COMPACT
                WindowWidthSizeClass.MEDIUM -> MEDIUM
                WindowWidthSizeClass.EXPANDED -> EXPANDED
                else -> {
                    Logger.d("Unexpected WindowWidthSizeClass: '$sizeClass'. Falling back to COMPACT.")
                    COMPACT
                }
            }
    }
}
