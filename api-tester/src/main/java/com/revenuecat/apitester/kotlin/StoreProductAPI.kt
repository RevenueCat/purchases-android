package com.revenuecat.apitester.kotlin

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.googleProduct

@Suppress("unused", "UNUSED_VARIABLE")
private class StoreProductAPI {
    fun check(product: StoreProduct) {
        with(product) {
            val storeProductId: String = productId
            val sku: String = sku
            val type: ProductType = type
            val oneTimeProductPrice: Price? = oneTimeProductPrice
            val title: String = title
            val description: String = description
            val subscriptionPeriod: String? = subscriptionPeriod
            val subscriptionOptions: List<SubscriptionOption> = subscriptionOptions
            val defaultOption: SubscriptionOption? = defaultOption
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
        val subscriptionOptions: List<GoogleSubscriptionOption> = googleStoreProduct.subscriptionOptions
        val defaultOption: GoogleSubscriptionOption? = googleStoreProduct.defaultOption
        val constructedGoogleStoreProduct = GoogleStoreProduct(
            googleStoreProduct.productId,
            googleStoreProduct.type,
            googleStoreProduct.oneTimeProductPrice,
            googleStoreProduct.title,
            googleStoreProduct.description,
            googleStoreProduct.subscriptionPeriod,
            googleStoreProduct.subscriptionOptions,
            googleStoreProduct.defaultOption,
            googleStoreProduct.productDetails
        )
    }
}
