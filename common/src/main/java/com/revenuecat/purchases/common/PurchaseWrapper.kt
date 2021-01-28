package com.revenuecat.purchases.common

import com.revenuecat.purchases.ProductType

interface PurchaseWrapper {
    val type: ProductType
    val purchaseToken: String
    val purchaseTime: Long
    val sku: String
    val presentedOfferingIdentifier: String?
    val purchaseState: RevenueCatPurchaseState
}

enum class RevenueCatPurchaseState {
    UNSPECIFIED_STATE, PURCHASED, PENDING
}
