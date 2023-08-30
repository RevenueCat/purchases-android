package com.revenuecat.purchases.ui.revenuecatui

enum class PaywallViewMode {
    FULL_SCREEN,
    FOOTER,
    FOOTER_CONDENSED,
    ;

    companion object {
        val default = FULL_SCREEN
    }
}
