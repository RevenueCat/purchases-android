package com.revenuecat.apitester.kotlin

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.google.GooglePurchaseOption
import com.revenuecat.purchases.google.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.google.googleProduct

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
            val purchaseOptions: List<PurchaseOption> = purchaseOptions
            val defaultOption: PurchaseOption? = defaultOption
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
        val purchaseOptions: List<GooglePurchaseOption> = googleStoreProduct.purchaseOptions
        val constructedGoogleStoreProduct = GoogleStoreProduct(
            googleStoreProduct.productId,
            googleStoreProduct.type,
            googleStoreProduct.oneTimeProductPrice,
            googleStoreProduct.title,
            googleStoreProduct.description,
            googleStoreProduct.subscriptionPeriod,
            googleStoreProduct.purchaseOptions,
            googleStoreProduct.defaultOption,
            googleStoreProduct.productDetails
        )
    }
}
