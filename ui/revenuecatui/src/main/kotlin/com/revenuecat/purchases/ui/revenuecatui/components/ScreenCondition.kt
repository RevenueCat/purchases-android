package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

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
