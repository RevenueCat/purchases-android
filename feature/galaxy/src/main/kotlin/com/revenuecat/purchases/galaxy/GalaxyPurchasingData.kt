package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchasingData
import dev.drewhamilton.poko.Poko
public abstract class GalaxyPurchasingData internal constructor() : PurchasingData {

    @Poko
    public class Product(
        override val productId: String,
        override val productType: ProductType,
    ) : GalaxyPurchasingData()
}
