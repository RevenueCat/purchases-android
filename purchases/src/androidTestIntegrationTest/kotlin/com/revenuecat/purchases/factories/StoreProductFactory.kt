package com.revenuecat.purchases.factories

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import io.mockk.mockk

object StoreProductFactory {

    fun createPrice(
        price: String = "€5.49",
        priceAmountMicros: Long = 5490000,
        priceCurrencyCode: String = "EUR",
    ): Price {
        return Price(price, priceAmountMicros, priceCurrencyCode)
    }

    fun createPeriod(
        value: Int = 1,
        unit: Period.Unit = Period.Unit.MONTH,
        iso8601: String = "P1M",
    ): Period {
        return Period(value, unit, iso8601)
    }

    fun createPricingPhase(
        period: Period = createPeriod(),
        recurrenceMode: RecurrenceMode = RecurrenceMode.INFINITE_RECURRING,
        numberOfPeriods: Int = 0,
        price: Price = createPrice(),
    ): PricingPhase {
        return PricingPhase(period, recurrenceMode, numberOfPeriods, price)
    }

    @Suppress("LongParameterList")
    fun createGoogleSubscriptionOption(
        productId: String = Constants.productIdToPurchase,
        basePlanId: String = Constants.basePlanIdToPurchase,
        offerId: String? = null,
        pricingPhases: List<PricingPhase> = listOf(createPricingPhase()),
        tags: List<String> = emptyList(),
        productDetails: ProductDetails = mockk(),
        offerToken: String = "test-offer-token",
    ): GoogleSubscriptionOption {
        return GoogleSubscriptionOption(
            productId,
            basePlanId,
            offerId,
            pricingPhases,
            tags,
            productDetails,
            offerToken,
        )
    }

    @Suppress("LongParameterList")
    fun createGoogleStoreProduct(
        productId: String = Constants.productIdToPurchase,
        basePlanId: String? = Constants.basePlanIdToPurchase,
        type: ProductType = ProductType.SUBS,
        price: Price = createPrice(),
        title: String = "Monthly Product Intro Pricing One Week (RevenueCat SDK Tester)",
        description: String = "Monthly Product Intro Pricing One Week",
        period: Period? = createPeriod(),
        defaultOptionIndex: Int = 0,
        productDetails: ProductDetails = mockk(),
        subscriptionOptionsList: List<SubscriptionOption>? = listOf(
            createGoogleSubscriptionOption(productDetails = productDetails),
        ),
    ): StoreProduct {
        val subscriptionOptions = subscriptionOptionsList?.let { SubscriptionOptions(it) }
        return GoogleStoreProduct(
            productId = productId,
            basePlanId = basePlanId,
            type = type,
            price = price,
            title = title,
            description = description,
            period = period,
            subscriptionOptions = subscriptionOptions,
            defaultOption = subscriptionOptions?.let { it[defaultOptionIndex] },
            productDetails = productDetails,
        )
    }
}
