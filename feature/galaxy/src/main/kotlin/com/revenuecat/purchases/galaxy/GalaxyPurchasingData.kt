package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchasingData
import dev.drewhamilton.poko.Poko

@ExperimentalPreviewRevenueCatPurchasesAPI
public sealed class GalaxyPurchasingData : PurchasingData {

    @Poko
    public class Product(
        override val productId: String,
        override val productType: ProductType,
    ) : GalaxyPurchasingData()
}
