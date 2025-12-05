package com.revenuecat.purchases.samsung

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchasingData
import dev.drewhamilton.poko.Poko

sealed class SamsungPurchasingData : PurchasingData {
    @Poko
    class Product(
        val storeProduct: SamsungStoreProduct,
    ) : SamsungPurchasingData() {
        override val productId: String
            get() = storeProduct.id
        override val productType: ProductType
            get() = storeProduct.type
    }
}
