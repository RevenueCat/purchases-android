package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchasingData
import dev.drewhamilton.poko.Poko

sealed class AmazonPurchasingData : PurchasingData {
    @Poko
    class Product(
        public val storeProduct: AmazonStoreProduct,
    ) : AmazonPurchasingData() {
        override val productId: String
            get() = storeProduct.id
        override val productType: ProductType
            get() = storeProduct.type
    }
}
