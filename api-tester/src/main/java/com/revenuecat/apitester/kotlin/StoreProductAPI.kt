package com.revenuecat.apitester.kotlin

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.amazon.AmazonStoreProduct
import com.revenuecat.purchases.amazon.amazonProduct
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.models.googleProduct
import org.json.JSONObject
import java.util.Locale

@Suppress("unused", "UNUSED_VARIABLE", "LongMethod", "DEPRECATION")
private class StoreProductAPI {
    fun check(product: StoreProduct) {
        val locale = Locale.getDefault()
        with(product) {
            val storeProductId: String = id
            val sku: String = sku
            val type: ProductType = type
            val price: Price = price
            val formattedPricePerMonth: String? = formattedPricePerMonth(locale)
            val formattedPricePerMonthNoLocale: String? = formattedPricePerMonth()
            val pricePerWeek: Price? = pricePerWeek(locale)
            val pricePerMonth: Price? = pricePerMonth(locale)
            val pricePerYear: Price? = pricePerYear(locale)
            val pricePerWeekNoLocale: Price? = pricePerYear()
            val pricePerMonthNoLocale: Price? = pricePerMonth()
            val pricePerYearNoLocale: Price? = pricePerYear()
            val name: String = name
            val title: String = title
            val description: String = description
            val period: Period? = period
            val subscriptionOptions: SubscriptionOptions? = subscriptionOptions
            val defaultOption: SubscriptionOption? = defaultOption
            val presentedOfferingIdentifier: String? = presentedOfferingIdentifier
            val presentedOfferingContext: PresentedOfferingContext = presentedOfferingContext
            val underlyingGoogleProduct: GoogleStoreProduct? = googleProduct
            val underlyingAmazonProduct: AmazonStoreProduct? = amazonProduct
            val copiedProductWithOfferingId = copyWithOfferingId("offeringId")
            val copiedProduct = copyWithPresentedOfferingContext(PresentedOfferingContext())
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
            googleStoreProduct.productDetails,
        )

        val constructedGoogleStoreProductWithPresentedOfferingContext = GoogleStoreProduct(
            googleStoreProduct.id,
            null,
            googleStoreProduct.type,
            googleStoreProduct.price,
            googleStoreProduct.name,
            googleStoreProduct.title,
            googleStoreProduct.description,
            googleStoreProduct.period,
            googleStoreProduct.subscriptionOptions,
            googleStoreProduct.defaultOption,
            googleStoreProduct.productDetails,
            PresentedOfferingContext(),
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
            googleStoreProduct.productDetails,
            googleStoreProduct.presentedOfferingIdentifier,
        )

        val constructedGoogleStoreProductWithName = GoogleStoreProduct(
            googleStoreProduct.id,
            null,
            googleStoreProduct.type,
            googleStoreProduct.price,
            googleStoreProduct.name,
            googleStoreProduct.title,
            googleStoreProduct.description,
            googleStoreProduct.period,
            googleStoreProduct.subscriptionOptions,
            googleStoreProduct.defaultOption,
            googleStoreProduct.productDetails,
            googleStoreProduct.presentedOfferingIdentifier,
        )

        val constructedGoogleStoreProductWithNameButNoOfferingId = GoogleStoreProduct(
            googleStoreProduct.id,
            null,
            googleStoreProduct.type,
            googleStoreProduct.price,
            googleStoreProduct.name,
            googleStoreProduct.title,
            googleStoreProduct.description,
            googleStoreProduct.period,
            googleStoreProduct.subscriptionOptions,
            googleStoreProduct.defaultOption,
            googleStoreProduct.productDetails,
        )

        val productId: String = constructedGoogleStoreProduct.productId
        val basePlanId: String? = constructedGoogleStoreProduct.basePlanId
        val productDetails: ProductDetails = googleStoreProduct.productDetails
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

        val constructedAmazonStoreProductWithContext = AmazonStoreProduct(
            amazonStoreProduct.id,
            amazonStoreProduct.type,
            amazonStoreProduct.name,
            amazonStoreProduct.title,
            amazonStoreProduct.description,
            amazonStoreProduct.period,
            amazonStoreProduct.price,
            amazonStoreProduct.subscriptionOptions,
            amazonStoreProduct.defaultOption,
            amazonStoreProduct.iconUrl,
            amazonStoreProduct.freeTrialPeriod,
            amazonStoreProduct.originalProductJSON,
            PresentedOfferingContext(),
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

        val constructedAmazonStoreProductWithName = AmazonStoreProduct(
            amazonStoreProduct.id,
            amazonStoreProduct.type,
            amazonStoreProduct.name,
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

        val constructedAmazonStoreProductWithNameAndOfferingId = AmazonStoreProduct(
            amazonStoreProduct.id,
            amazonStoreProduct.type,
            amazonStoreProduct.name,
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
