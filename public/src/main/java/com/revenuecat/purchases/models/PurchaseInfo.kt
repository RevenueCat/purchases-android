package com.revenuecat.purchases.models

import com.revenuecat.purchases.ProductType

interface PurchaseInfo {
    val productId: String
    val productType: ProductType
}