package com.revenuecat.purchases.models

import com.android.billingclient.api.BillingFlowParams

internal fun StoreReplacementMode.toGoogleBillingClientMode(): Int {
    return when (this) {
        StoreReplacementMode.WITHOUT_PRORATION ->
            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION
        StoreReplacementMode.WITH_TIME_PRORATION ->
            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
        StoreReplacementMode.CHARGE_FULL_PRICE ->
            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
        StoreReplacementMode.CHARGE_PRORATED_PRICE ->
            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE
        StoreReplacementMode.DEFERRED ->
            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED
    }
}

internal fun StoreReplacementMode.toGoogleReplacementMode(): GoogleReplacementMode {
    return when (this) {
        StoreReplacementMode.WITHOUT_PRORATION -> GoogleReplacementMode.WITHOUT_PRORATION
        StoreReplacementMode.WITH_TIME_PRORATION -> GoogleReplacementMode.WITH_TIME_PRORATION
        StoreReplacementMode.CHARGE_FULL_PRICE -> GoogleReplacementMode.CHARGE_FULL_PRICE
        StoreReplacementMode.CHARGE_PRORATED_PRICE -> GoogleReplacementMode.CHARGE_PRORATED_PRICE
        StoreReplacementMode.DEFERRED -> GoogleReplacementMode.DEFERRED
    }
}
