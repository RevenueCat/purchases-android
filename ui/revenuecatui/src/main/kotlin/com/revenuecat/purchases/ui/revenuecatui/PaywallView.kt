package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.Offering

@Suppress("unused")
@Composable
fun PaywallView(
    offering: Offering? = null,
    shouldDisplayDismissButton: Boolean = false,
    listener: PaywallViewListener = object : PaywallViewListener {},
) {
    // WIP
}
