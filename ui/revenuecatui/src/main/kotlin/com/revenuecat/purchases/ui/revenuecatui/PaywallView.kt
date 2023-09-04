package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.Offering

/**
 * Composable offering a full screen Paywall UI configured from the RevenueCat dashboard.
 * @param offering The offering to be displayed. If null, the [com.revenuecat.purchases.Offerings.current] will be used.
 * @param shouldDisplayDismissButton Whether a dismiss button should be displayed as part of the paywall or not.
 * @param listener Listener for common paywall events.
 */
@Suppress("unused")
@Composable
fun PaywallView(
    offering: Offering? = null,
    shouldDisplayDismissButton: Boolean = false,
    listener: PaywallViewListener = object : PaywallViewListener {},
) {
    // WIP
}
