package com.revenuecat.purchases.models

import com.revenuecat.purchases.ProductType

public interface PurchasingData {
    public val productId: String
    public val productType: ProductType
}
