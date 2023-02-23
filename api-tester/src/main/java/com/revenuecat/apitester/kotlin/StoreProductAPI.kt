package com.revenuecat.apitester.kotlin

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.models.googleProduct

@Suppress("unused", "UNUSED_VARIABLE")
private class StoreProductAPI {
    fun check(product: StoreProduct) {
        with(product) {
            val storeProductId: String = id
            val sku: String = sku
            val type: ProductType = type
            val price: Price? = price
            val title: String = title
            val description: String = description
            val period: Period? = period
            val subscriptionOptions: SubscriptionOptions? = subscriptionOptions
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
        val subscriptionOptions: SubscriptionOptions? = googleStoreProduct.subscriptionOptions
        val defaultOption: GoogleSubscriptionOption? = googleStoreProduct.defaultOption
        val constructedGoogleStoreProduct = GoogleStoreProduct(
            googleStoreProduct.id,
            null,
            googleStoreProduct.type,
            googleStoreProduct.price,
            googleStoreProduct.title,
            googleStoreProduct.description,
            googleStoreProduct.period,
            googleStoreProduct.subscriptionOptions,
            googleStoreProduct.defaultOption,
            googleStoreProduct.productDetails
        )

        val productId = constructedGoogleStoreProduct.productId
        val basePlanId = constructedGoogleStoreProduct.basePlanId
    }
}
