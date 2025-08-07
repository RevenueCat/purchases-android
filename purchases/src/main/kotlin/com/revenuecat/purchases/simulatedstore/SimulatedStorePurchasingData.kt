package com.revenuecat.purchases.simulatedstore

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct

internal data class SimulatedStorePurchasingData(
    override val productId: String,
    override val productType: ProductType,
    val storeProduct: StoreProduct,
) : PurchasingData
