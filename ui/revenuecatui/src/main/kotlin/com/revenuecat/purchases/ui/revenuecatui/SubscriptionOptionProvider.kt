package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.SubscriptionOption

/**
 * Interface for providing custom subscription options when purchasing a Package within a RevenueCat paywall
 *
 * This is useful when for example using `SubscriptionOptions` tags to provide discounts using promo codes
 */
interface SubscriptionOptionProvider {
    fun subscriptionOption(rcPackage: Package): SubscriptionOption?
}