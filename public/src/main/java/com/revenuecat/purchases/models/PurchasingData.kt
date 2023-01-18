package com.revenuecat.purchases.models

import com.revenuecat.purchases.ProductType

interface PurchasingData {
    val productId: String
    val productType: ProductType
}
