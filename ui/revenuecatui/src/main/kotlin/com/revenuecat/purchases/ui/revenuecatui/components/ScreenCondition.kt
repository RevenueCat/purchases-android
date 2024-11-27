package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.window.core.layout.WindowWidthSizeClass

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
                else -> COMPACT
            }
    }
}
