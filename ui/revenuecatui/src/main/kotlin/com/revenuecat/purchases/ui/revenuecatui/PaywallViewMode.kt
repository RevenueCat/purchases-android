package com.revenuecat.purchases.ui.revenuecatui

internal enum class PaywallViewMode {
    FULL_SCREEN,
    FOOTER,
    FOOTER_CONDENSED,
    ;

    companion object {
        val default = FULL_SCREEN
    }

    fun shouldDisplayIcon(): Boolean {
        return when (this) {
            FULL_SCREEN -> true
            FOOTER, FOOTER_CONDENSED -> false
        }
    }
}
