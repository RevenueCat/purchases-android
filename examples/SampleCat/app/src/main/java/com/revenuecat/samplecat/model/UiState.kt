package com.revenuecat.samplecat.model

/**
 * Represents the state of a purchasable item.
 */
enum class PurchasableState {
    ReadyToPurchase,
    Purchasing,
    Purchased,
    PurchasingOtherProduct,
}
