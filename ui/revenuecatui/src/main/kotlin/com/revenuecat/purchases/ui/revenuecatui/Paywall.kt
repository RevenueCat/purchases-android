package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.Composable

/**
 * Composable offering a full screen Paywall UI configured from the RevenueCat dashboard.
 * @param options The options to configure the [Paywall] if needed.
 */
@Suppress("unused")
@Composable
fun Paywall(
    options: PaywallOptions = PaywallOptions.Builder().build(),
) {
    InternalPaywall(options)
}
