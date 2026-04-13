@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.runtime.staticCompositionLocalOf
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.events.PaywallComponentInteractionData

/**
 * Tracks paywall component interactions for analytics.
 */
internal fun interface PaywallComponentInteractionTracker {
    fun track(data: PaywallComponentInteractionData)
}

internal val LocalPaywallComponentInteractionTracker =
    staticCompositionLocalOf<PaywallComponentInteractionTracker> {
        PaywallComponentInteractionTracker { _ -> }
    }

/**
 * V1 template footer / tier control `component_name` constants.
 */
internal object PaywallLegacyComponentInteraction {
    const val ALL_PLANS_BUTTON_NAME = "all_plans_button"
    const val RESTORE_BUTTON_NAME = "restore_button"
    const val TERMS_LINK_NAME = "terms_link"
    const val PRIVACY_LINK_NAME = "privacy_link"
    const val TIER_SELECTOR_NAME = "tier_selector"

    object Value {
        const val TOGGLE_ALL_PLANS = "toggle_all_plans"
        const val RESTORE_PURCHASES = "restore_purchases"
        const val NAVIGATE_TO_TERMS = "navigate_to_terms"
        const val NAVIGATE_TO_PRIVACY_POLICY = "navigate_to_privacy_policy"
    }
}
