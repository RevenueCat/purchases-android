package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchasingData

sealed class AmazonPurchasingData : PurchasingData {
    data class Product(
        val storeProduct: AmazonStoreProduct,
    ) : AmazonPurchasingData() {
        override val productId: String
            get() = storeProduct.id
        override val productType: ProductType
            get() = storeProduct.type
    }
}
