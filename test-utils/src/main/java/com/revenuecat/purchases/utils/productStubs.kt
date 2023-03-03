package com.revenuecat.purchases.utils

import android.os.Parcel
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.models.toRecurrenceMode

@SuppressWarnings("MatchingDeclarationName")
private data class StubPurchasingData(
    override val productId: String,
) : PurchasingData {
    override val productType: ProductType
        get() = ProductType.SUBS
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubStoreProduct(
    productId: String,
    defaultOption: SubscriptionOption? = stubSubscriptionOption(
        "monthly_base_plan", productId,
        Period(1, Period.Unit.MONTH, "P1M"),
    ),
    subscriptionOptions: List<SubscriptionOption> = defaultOption?.let { listOf(defaultOption) } ?: emptyList(),
    price: Price = subscriptionOptions.first().fullPricePhase!!.price
): StoreProduct = object : StoreProduct {
    override val id: String
        get() = productId
    override val type: ProductType
        get() = ProductType.SUBS
    override val price: Price
        get() = price
    override val title: String
        get() = ""
    override val description: String
        get() = ""
    override val period: Period?
        get() = subscriptionOptions.firstOrNull { it.isBasePlan }?.pricingPhases?.get(0)?.billingPeriod
    override val subscriptionOptions: SubscriptionOptions
        get() = SubscriptionOptions(subscriptionOptions)
    override val defaultOption: SubscriptionOption?
        get() = defaultOption
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId
        )
    override val sku: String
        get() = productId

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {}
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubINAPPStoreProduct(
    productId: String
): StoreProduct = object : StoreProduct {
    override val id: String
        get() = productId
    override val type: ProductType
        get() = ProductType.INAPP
    override val price: Price
        get() = Price("\$1.00", MICROS_MULTIPLIER * 1L, "USD")
    override val title: String
        get() = ""
    override val description: String
        get() = ""
    override val period: Period?
        get() = null
    override val subscriptionOptions: SubscriptionOptions
        get() = SubscriptionOptions(listOf(defaultOption))
    override val defaultOption: SubscriptionOption
        get() = stubSubscriptionOption(productId, productId)
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId
        )
    override val sku: String
        get() = productId

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {}
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubSubscriptionOption(
    id: String,
    productId: String,
    duration: Period = Period(1, Period.Unit.MONTH, "P1M"),
    pricingPhases: List<PricingPhase> = listOf(stubPricingPhase(billingPeriod = duration))
): SubscriptionOption = object : SubscriptionOption {
    override val id: String
        get() = id
    override val pricingPhases: List<PricingPhase>
        get() = pricingPhases
    override val tags: List<String>
        get() = listOf("tag")
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId
        )

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel?, flags: Int) {}
}

fun stubFreeTrialPricingPhase(
    billingPeriod: Period = Period(1, Period.Unit.MONTH, "P1M"),
    priceCurrencyCodeValue: String = "USD",
) = stubPricingPhase(
    billingPeriod = billingPeriod,
    priceCurrencyCodeValue = priceCurrencyCodeValue,
    price = 0.0,
    recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
    billingCycleCount = 1
)

fun stubPricingPhase(
    billingPeriod: Period = Period(1, Period.Unit.MONTH, "P1M"),
    priceCurrencyCodeValue: String = "USD",
    price: Double = 4.99,
    recurrenceMode: Int = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
    billingCycleCount: Int = 0
): PricingPhase = PricingPhase(
    billingPeriod,
    recurrenceMode.toRecurrenceMode(),
    billingCycleCount,
    Price(if (price == 0.0) "Free" else "${'$'}$price", price.times(MICROS_MULTIPLIER).toLong(), priceCurrencyCodeValue)
)
