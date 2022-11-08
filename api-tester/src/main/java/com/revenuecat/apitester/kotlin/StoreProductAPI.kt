package com.revenuecat.apitester.kotlin

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.googleProduct

@Suppress("unused", "UNUSED_VARIABLE")
private class StoreProductAPI {
    fun check(product: StoreProduct) {
        with(product) {
            val storeProductId: String = sku // TODOBC5 - rename to storeProductId
            val type: ProductType = type
            val oneTimeProductPrice: Price? = oneTimeProductPrice
            val title: String = title
            val description: String = description
            val subscriptionPeriod: String? = subscriptionPeriod
            val underlyingProduct: GoogleStoreProduct? = googleProduct
        }
    }

    fun check(type: ProductType) {
        when (type) {
            ProductType.SUBS,
            ProductType.INAPP,
            ProductType.UNKNOWN
            -> {}
        }.exhaustive
    }

    fun checkGoogleStoreProduct(googleStoreProduct: GoogleStoreProduct) {
        check(googleStoreProduct)
        val productDetails: ProductDetails = googleStoreProduct.productDetails
    }
}
