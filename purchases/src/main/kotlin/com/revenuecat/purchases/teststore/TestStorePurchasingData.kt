package com.revenuecat.purchases.teststore

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct

internal data class TestStorePurchasingData(
    override val productId: String,
    override val productType: ProductType,
    val storeProduct: StoreProduct,
) : PurchasingData
