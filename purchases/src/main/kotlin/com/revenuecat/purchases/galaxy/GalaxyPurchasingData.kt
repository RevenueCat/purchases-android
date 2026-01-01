package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchasingData
import dev.drewhamilton.poko.Poko

sealed class GalaxyPurchasingData : PurchasingData {
    @Poko
    class Product(
        override val productId: String,
        override val productType: ProductType,
    ) : GalaxyPurchasingData()
}
