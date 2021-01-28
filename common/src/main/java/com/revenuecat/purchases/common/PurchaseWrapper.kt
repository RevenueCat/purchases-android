package com.revenuecat.purchases.common

import com.revenuecat.purchases.ProductType

interface PurchaseWrapper {
    val type: ProductType
    val purchaseToken: String
    val purchaseTime: Long
    val sku: String
    val presentedOfferingIdentifier: String?
    val purchaseState: RevenueCatPurchaseState
    val storeUserID: String?
}

enum class RevenueCatPurchaseState {
    UNSPECIFIED_STATE, PURCHASED, PENDING
}
