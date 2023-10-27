package com.revenuecat.purchases.ui.revenuecatui

internal enum class PaywallMode {
    FULL_SCREEN,
    FOOTER,
    FOOTER_CONDENSED,
    ;

    companion object {
        val default = FULL_SCREEN

        fun footerMode(condensed: Boolean): PaywallMode {
            return if (condensed) FOOTER_CONDENSED else FOOTER
        }
    }
}

internal val PaywallMode.isFullScreen: Boolean
    get() = this == PaywallMode.FULL_SCREEN
