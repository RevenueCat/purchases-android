package com.revenuecat.purchases.ui.revenuecatui.data.navigation

import com.revenuecat.purchases.paywalls.components.common.ExitPaywallPresentation

internal data class ExitPaywallSettings(
    val bounce: ExitPaywall?,
    val abandonment: ExitPaywall?,
) {
    val hasAny: Boolean
        get() = bounce != null || abandonment != null

    data class ExitPaywall(
        val offeringId: String,
        val presentation: ExitPaywallPresentation,
        val dismissCurrent: Boolean,
    )
}
