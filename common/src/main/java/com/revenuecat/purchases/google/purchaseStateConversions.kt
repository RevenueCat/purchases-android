package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase.PurchaseState as GooglePurchaseState
import com.revenuecat.purchases.models.PurchaseState as RevenueCatPurchaseState

fun Int.toRevenueCatPurchaseState(): RevenueCatPurchaseState {
    return when (this) {
        GooglePurchaseState.UNSPECIFIED_STATE -> RevenueCatPurchaseState.UNSPECIFIED_STATE
        GooglePurchaseState.PURCHASED -> RevenueCatPurchaseState.PURCHASED
        GooglePurchaseState.PENDING -> RevenueCatPurchaseState.PENDING
        else -> RevenueCatPurchaseState.UNSPECIFIED_STATE
    }
}

fun RevenueCatPurchaseState.toGooglePurchaseState(): Int {
    return when (this) {
        RevenueCatPurchaseState.UNSPECIFIED_STATE -> GooglePurchaseState.UNSPECIFIED_STATE
        RevenueCatPurchaseState.PURCHASED -> GooglePurchaseState.PURCHASED
        RevenueCatPurchaseState.PENDING -> GooglePurchaseState.PENDING
    }
}
