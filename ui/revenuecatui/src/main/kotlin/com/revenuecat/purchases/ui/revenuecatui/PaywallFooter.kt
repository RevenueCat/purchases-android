package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.Offering

/**
 * Composable offering a footer Paywall UI using a bottom sheet. It can be configured from the RevenueCat dashboard.
 * @param isVisible Whether the bottom sheet should be visible or not.
 * @param shouldAllowDismissing Whether the bottom sheet should be dismissible or not.
 * @param condensed Whether the bottom sheet will be in condensed form or not.
 * @param offering The offering to be displayed. If null, the [com.revenuecat.purchases.Offerings.current] will be used.
 * @param listener Listener for common paywall events.
 */
@Suppress("unused")
@Composable
fun PaywallFooter(
    isVisible: Boolean = true,
    shouldAllowDismissing: Boolean = true,
    condensed: Boolean = false,
    offering: Offering? = null,
    listener: PaywallViewListener = object : PaywallViewListener {},
) {
    // WIP
}
