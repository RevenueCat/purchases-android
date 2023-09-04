package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.Offering

@Suppress("unused")
@Composable
fun PaywallBottomSheet(
    isVisible: Boolean = true,
    shouldAllowDismissing: Boolean = true,
    condensed: Boolean = false,
    offering: Offering? = null,
    listener: PaywallViewListener = object : PaywallViewListener {},
) {
    // WIP
}
