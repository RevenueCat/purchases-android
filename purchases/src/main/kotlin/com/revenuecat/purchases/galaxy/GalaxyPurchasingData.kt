package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchasingData
import dev.drewhamilton.poko.Poko

sealed class GalaxyPurchasingData : PurchasingData {
    @Poko
    class Product(
        val storeProduct: GalaxyStoreProduct,
    ) : GalaxyPurchasingData() {
        override val productId: String
            get() = storeProduct.id
        override val productType: ProductType
            get() = storeProduct.type
    }
}
