package com.revenuecat.purchases.ui.revenuecatui

internal enum class PaywallViewMode {
    FULL_SCREEN,
    FOOTER,
    FOOTER_CONDENSED,
    ;

    companion object {
        val default = FULL_SCREEN
    }

    val isFullScreen: Boolean
        get() = when (this) {
            FULL_SCREEN -> true
            FOOTER, FOOTER_CONDENSED -> false
        }
}
