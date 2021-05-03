package com.revenuecat.purchases.common

import com.android.billingclient.api.BillingClient

/**
 * Enum mapping billing feature types
 * Allows for a common interface when calling feature eligibility methods from hybrid SDKs
 */
enum class BillingFeature(@BillingClient.FeatureType val playBillingClientName: String) {
    SUBSCRIPTIONS(BillingClient.FeatureType.SUBSCRIPTIONS),
    SUBSCRIPTIONS_UPDATE(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE),
    IN_APP_ITEMS_ON_VR(BillingClient.FeatureType.IN_APP_ITEMS_ON_VR),
    SUBSCRIPTIONS_ON_VR(BillingClient.FeatureType.SUBSCRIPTIONS_ON_VR),
    PRICE_CHANGE_CONFIRMATION(BillingClient.FeatureType.PRICE_CHANGE_CONFIRMATION)
}
