package com.revenuecat.purchases.models

import com.android.billingclient.api.BillingClient

/**
 * Enum mapping billing feature types
 * Allows for a common interface when calling feature eligibility methods from hybrid SDKs
 */
enum class BillingFeature(@BillingClient.FeatureType val playBillingClientName: String) {
    SUBSCRIPTIONS(BillingClient.FeatureType.SUBSCRIPTIONS),
    SUBSCRIPTIONS_UPDATE(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE),
    PRICE_CHANGE_CONFIRMATION(BillingClient.FeatureType.PRICE_CHANGE_CONFIRMATION),
}
