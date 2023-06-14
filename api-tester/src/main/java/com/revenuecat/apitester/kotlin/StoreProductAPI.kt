package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.amazon.AmazonStoreProduct
import com.revenuecat.purchases.amazon.amazonProduct
import com.revenuecat.purchases.models.GoogleProductData
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.models.googleProduct
import org.json.JSONObject

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
            val presentedOfferingIdentifier: String? = presentedOfferingIdentifier
            val underlyingGoogleProduct: GoogleStoreProduct? = googleProduct
            val underlyingAmazonProduct: AmazonStoreProduct? = amazonProduct
        }
    }

    fun check(type: ProductType) {
        when (type) {
            ProductType.SUBS,
            ProductType.INAPP,
            ProductType.UNKNOWN,
            -> {}
        }.exhaustive
    }

    fun checkGoogleStoreProduct(googleStoreProduct: GoogleStoreProduct) {
        check(googleStoreProduct)
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
            googleStoreProduct.googleProductData,
        )

        val constructedGoogleStoreProductWithOfferingId = GoogleStoreProduct(
            googleStoreProduct.id,
            null,
            googleStoreProduct.type,
            googleStoreProduct.price,
            googleStoreProduct.title,
            googleStoreProduct.description,
            googleStoreProduct.period,
            googleStoreProduct.subscriptionOptions,
            googleStoreProduct.defaultOption,
            googleStoreProduct.googleProductData,
            googleStoreProduct.presentedOfferingIdentifier,
        )

        val productId: String = constructedGoogleStoreProduct.productId
        val basePlanId: String? = constructedGoogleStoreProduct.basePlanId
        val googleProductData: GoogleProductData = googleStoreProduct.googleProductData
    }

    fun checkAmazonStoreProduct(amazonStoreProduct: AmazonStoreProduct) {
        check(amazonStoreProduct)
        val constructedAmazonStoreProduct = AmazonStoreProduct(
            amazonStoreProduct.id,
            amazonStoreProduct.type,
            amazonStoreProduct.title,
            amazonStoreProduct.description,
            amazonStoreProduct.period,
            amazonStoreProduct.price,
            amazonStoreProduct.subscriptionOptions,
            amazonStoreProduct.defaultOption,
            amazonStoreProduct.iconUrl,
            amazonStoreProduct.freeTrialPeriod,
            amazonStoreProduct.originalProductJSON,
        )

        val constructedAmazonStoreProductWithOfferingId = AmazonStoreProduct(
            amazonStoreProduct.id,
            amazonStoreProduct.type,
            amazonStoreProduct.title,
            amazonStoreProduct.description,
            amazonStoreProduct.period,
            amazonStoreProduct.price,
            amazonStoreProduct.subscriptionOptions,
            amazonStoreProduct.defaultOption,
            amazonStoreProduct.iconUrl,
            amazonStoreProduct.freeTrialPeriod,
            amazonStoreProduct.originalProductJSON,
            amazonStoreProduct.presentedOfferingIdentifier,
        )
        val iconUrl: String = amazonStoreProduct.iconUrl
        val freeTrialPeriod: Period? = amazonStoreProduct.freeTrialPeriod
        val originalProductJson: JSONObject = amazonStoreProduct.originalProductJSON
    }
}
